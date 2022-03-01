package io.projectnewm.server.portal.song.impl

import io.projectnewm.server.portal.model.Contributor
import io.projectnewm.server.portal.model.Role
import io.projectnewm.server.portal.model.Song
import kotlinx.datetime.LocalDate
import java.time.Month

val mockSongs = listOf(
    Song(
        id = "39F63442-DC66-4AED-8280-6FB618082DFE",
        name = "Mr Rager",
        description = "Kid Cudi's best",
        genres = listOf("Hip Hop"),
        releaseDate = LocalDate(2016, Month.MAY, 1),
        albumImageUrl = "https://upload.wikimedia.org/wikipedia/en/0/0a/Kidcudimanonthemoonthelegendof.jpg",
        extraInfo = "extra info",
        contributors = listOf(
            Contributor(
                name = "John",
                role = Role.Producer,
                stake = 0.25
            ),
            Contributor(
                name = "Dan",
                role = Role.SoundEngineer,
                stake = 0.25
            ),
            Contributor(
                name = "Cudi",
                role = Role.Singer,
                stake = 0.5
            )
        )
    ),
    Song(
        id = "AF453F0F-93B2-4230-89E2-5FABFC4BE73B",
        name = "Neighbors",
        description = "J.Cole's best",
        genres = listOf("Hip Hop"),
        releaseDate = LocalDate(2017, Month.MAY, 1),
        albumImageUrl = "https://images.genius.com/6d0fbbc7ce189a8c81671ef92546446e.1000x1000x1.png",
        extraInfo = "extra info",
        contributors = listOf(
            Contributor(
                name = "John",
                role = Role.Producer,
                stake = 0.25
            ),
            Contributor(
                name = "Dan",
                role = Role.SoundEngineer,
                stake = 0.25
            ),
            Contributor(
                name = "J Cole",
                role = Role.Singer,
                stake = 0.5
            )
        )
    ),
    Song(
        id = "D0222091-7707-4C6E-BB62-AA73684EB80F",
        name = "Alright",
        description = "Kendrick's best",
        releaseDate = LocalDate(2018, Month.MAY, 1),
        albumImageUrl = "https://afterlivesofslavery.files.wordpress.com/2018/04/on-top-of-the-world.jpg",
        extraInfo = "extra info",
        genres = listOf("Hip Hop"),
        contributors = listOf(
            Contributor(
                name = "John",
                role = Role.Producer,
                stake = 0.25
            ),
            Contributor(
                name = "Dan",
                role = Role.SoundEngineer,
                stake = 0.25
            ),
            Contributor(
                name = "Kendrick Lamar",
                role = Role.Singer,
                stake = 0.5
            )
        )
    ),
    Song(
        id = "AC0F8210-30B2-4C75-89FC-83B9830EB2F6",
        name = "Flower Boy",
        description = "Kendrick's best",
        genres = listOf("Hip Hop"),
        releaseDate = LocalDate(2018, Month.MAY, 1),
        albumImageUrl = "https://m.media-amazon.com/images/I/91OeYnLoCJL._SL1500_.jpg",
        extraInfo = "extra info",
        contributors = listOf(
            Contributor(
                name = "John",
                role = Role.Producer,
                stake = 0.25
            ),
            Contributor(
                name = "Dan",
                role = Role.SoundEngineer,
                stake = 0.25
            ),
            Contributor(
                name = "Tyler the Creator",
                role = Role.Singer,
                stake = 0.5
            )
        )
    ),
    Song(
        id = "574C2F44-1216-45E6-BE67-3418A0B1B21E",
        name = "Sad people",
        description = "Sad people",
        genres = listOf("Hip Hop"),
        releaseDate = LocalDate(2018, Month.MAY, 1),
        albumImageUrl = "https://images.complex.com/complex/images/c_fill,dpr_auto,f_auto,q_90,w_1400/fl_lossy,pg_1/hcjrqlvc6dfhpjxob9nt/cudi",
        extraInfo = "extra info",
        contributors = listOf(
            Contributor(
                name = "John",
                role = Role.Producer,
                stake = 0.25
            ),
            Contributor(
                name = "Dan",
                role = Role.SoundEngineer,
                stake = 0.25
            ),
            Contributor(
                name = "Kid Cudi",
                role = Role.Singer,
                stake = 0.5
            )
        )
    )
)
