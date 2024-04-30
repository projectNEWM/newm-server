package io.newm.server.database.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.jetbrains.exposed.sql.transactions.transaction

class V53__CreateReleases : BaseJavaMigration() {
    override fun migrate(context: Context?) {
        transaction {
            execInBatch(
                listOf(
                    """
                    CREATE TABLE IF NOT EXISTS "releases"
                    (
                        "id" uuid PRIMARY KEY,
                        "owner_id" uuid NOT NULL,
                        "release_type" varchar(20) NOT NULL,
                        "title" text NOT NULL,
                        "distribution_release_id" bigint,
                        "barcode_type" integer,
                        "barcode_number" text,
                        "release_date" DATE,
                        "publication_date" DATE,
                        "cover_art_url" text,
                        "arweave_cover_art_url" text,
                        "has_submitted_for_distribution" boolean NOT NULL DEFAULT FALSE,
                        "error_message" text,
                        "force_distributed" boolean,
                        "archived" boolean NOT NULL DEFAULT FALSE,
                        "created_at" timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        CONSTRAINT fk_releases_owner_id__id FOREIGN KEY (owner_id) REFERENCES users (id) ON UPDATE NO ACTION ON DELETE NO ACTION
                    );
                    """.trimIndent(),
                    """
                    ALTER TABLE "songs"
                    RENAME COLUMN "album" TO "release_id";
                    """.trimIndent(),
                    """
                    ALTER TABLE "songs"
                    ALTER COLUMN "release_id" SET DATA TYPE uuid USING release_id::uuid;
                    """.trimIndent(),
                    """
                    ALTER TABLE "songs"
                    ADD CONSTRAINT fk_songs_release_id__id FOREIGN KEY (release_id) REFERENCES releases (id) ON UPDATE NO ACTION ON DELETE NO ACTION;
                    """.trimIndent(),
                    // Note: initially, a release_id will match a song_id since everything is a single and it makes this
                    // migration simpler. In the future, that will not be the case.
                    """
                    INSERT INTO releases (id, owner_id, release_type, title, distribution_release_id, barcode_type, barcode_number, release_date, publication_date, cover_art_url, arweave_cover_art_url, has_submitted_for_distribution, error_message, force_distributed, archived, created_at)
                    (SELECT
                        id,
                        owner_id,
                        'single',
                        title,
                        distribution_release_id,
                        barcode_type,
                        barcode_number,
                        release_date,
                        publication_date,
                        cover_art_url,
                        arweave_cover_art_url,
                        has_submitted_for_distribution,
                        error_message,
                        force_distributed,
                        archived,
                        created_at
                    FROM songs);
                    """.trimIndent(),
                    """
                    UPDATE songs
                    SET release_id = id;
                    """.trimIndent(),
                    """
                    ALTER TABLE songs
                    DROP COLUMN distribution_release_id,
                    DROP COLUMN barcode_type,
                    DROP COLUMN barcode_number,
                    DROP COLUMN release_date,
                    DROP COLUMN publication_date,
                    DROP COLUMN cover_art_url,
                    DROP COLUMN arweave_cover_art_url,
                    DROP COLUMN has_submitted_for_distribution,
                    DROP COLUMN force_distributed,
                    DROP COLUMN error_message;
                    """.trimIndent()
                )
            )
        }
    }
}
