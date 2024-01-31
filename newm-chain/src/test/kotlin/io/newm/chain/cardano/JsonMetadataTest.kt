package io.newm.chain.cardano

import com.google.common.truth.Truth.assertThat
import io.newm.chain.database.entity.LedgerAssetMetadata
import org.junit.jupiter.api.Test

class JsonMetadataTest {
    @Test
    fun `test to721Json`() {
        val ledgerAssetMetadata =
            listOf(
                LedgerAssetMetadata(
                    id = 84175031L,
                    assetId = 2519741L,
                    keyType = "string",
                    key = "Hat",
                    valueType = "string",
                    value = "None",
                    nestLevel = 0
                ),
                LedgerAssetMetadata(
                    id = 84175032L,
                    assetId = 2519741L,
                    keyType = "string",
                    key = "Eyes",
                    valueType = "string",
                    value = "Fire Eyes",
                    nestLevel = 0
                ),
                LedgerAssetMetadata(
                    id = 84175033L,
                    assetId = 2519741L,
                    keyType = "string",
                    key = "Mask",
                    valueType = "string",
                    value = "None",
                    nestLevel = 0
                ),
                LedgerAssetMetadata(
                    id = 84175034L,
                    assetId = 2519741L,
                    keyType = "string",
                    key = "Skin",
                    valueType = "string",
                    value = "Dark Brown",
                    nestLevel = 0
                ),
                LedgerAssetMetadata(
                    id = 84175035L,
                    assetId = 2519741L,
                    keyType = "string",
                    key = "name",
                    valueType = "string",
                    value = "Meerkat #10",
                    nestLevel = 0
                ),
                LedgerAssetMetadata(
                    id = 84175036L,
                    assetId = 2519741L,
                    keyType = "string",
                    key = "Mouth",
                    valueType = "string",
                    value = "Smile",
                    nestLevel = 0
                ),
                LedgerAssetMetadata(
                    id = 84175037L,
                    assetId = 2519741L,
                    keyType = "string",
                    key = "files",
                    valueType = "array",
                    value = "",
                    nestLevel = 0,
                    children =
                        listOf(
                            LedgerAssetMetadata(
                                id = 84175038L,
                                assetId = 2519741L,
                                keyType = "string",
                                key = "files",
                                valueType = "map",
                                value = "",
                                nestLevel = 1,
                                children =
                                    listOf(
                                        LedgerAssetMetadata(
                                            id = 84175039L,
                                            assetId = 2519741L,
                                            keyType = "string",
                                            key = "src",
                                            valueType = "string",
                                            value = "ipfs://QmfQMvMwSCsJ6WSPx7tDAH4zJLQqWmx6NThDrvxU3iQ4Wq",
                                            nestLevel = 2
                                        ),
                                        LedgerAssetMetadata(
                                            id = 84175040L,
                                            assetId = 2519741L,
                                            keyType = "string",
                                            key = "name",
                                            valueType = "string",
                                            value = "Meerkat #10",
                                            nestLevel = 2
                                        ),
                                        LedgerAssetMetadata(
                                            id = 84175041L,
                                            assetId = 2519741L,
                                            keyType = "string",
                                            key = "mediaType",
                                            valueType = "string",
                                            value = "image/png",
                                            nestLevel = 2
                                        ),
                                    )
                            ),
                        )
                ),
                LedgerAssetMetadata(
                    id = 84175042L,
                    assetId = 2519741L,
                    keyType = "string",
                    key = "image",
                    valueType = "string",
                    value = "ipfs://QmfQMvMwSCsJ6WSPx7tDAH4zJLQqWmx6NThDrvxU3iQ4Wq",
                    nestLevel = 0
                ),
                LedgerAssetMetadata(
                    id = 84175043L,
                    assetId = 2519741L,
                    keyType = "string",
                    key = "Tattoo",
                    valueType = "string",
                    value = "I Love You",
                    nestLevel = 0
                ),
                LedgerAssetMetadata(
                    id = 84175044L,
                    assetId = 2519741L,
                    keyType = "string",
                    key = "Clothing",
                    valueType = "string",
                    value = "Rainbow Hoodie",
                    nestLevel = 0
                ),
                LedgerAssetMetadata(
                    id = 84175045L,
                    assetId = 2519741L,
                    keyType = "string",
                    key = "mediaType",
                    valueType = "string",
                    value = "image/png",
                    nestLevel = 0
                ),
                LedgerAssetMetadata(
                    id = 84175046L,
                    assetId = 2519741L,
                    keyType = "string",
                    key = "Background",
                    valueType = "string",
                    value = "Orange",
                    nestLevel = 0
                ),
                LedgerAssetMetadata(
                    id = 84175047L,
                    assetId = 2519741L,
                    keyType = "string",
                    key = "Ear Accessory",
                    valueType = "string",
                    value = "Ruby Earrings",
                    nestLevel = 0
                ),
            )

        val json =
            ledgerAssetMetadata.to721Json(
                policy = "99027c7af372116f0716ee02008739ec51224bcfdd54398ec16217ef",
                name = "4d6565726b61743130"
            )
        assertThat(
            json
        ).isEqualTo(
            """{"721":{"99027c7af372116f0716ee02008739ec51224bcfdd54398ec16217ef":{"Meerkat10":{"Hat":"None","Eyes":"Fire Eyes","Mask":"None","Skin":"Dark Brown","name":"Meerkat #10","Mouth":"Smile","files":[{"src":"ipfs://QmfQMvMwSCsJ6WSPx7tDAH4zJLQqWmx6NThDrvxU3iQ4Wq","name":"Meerkat #10","mediaType":"image/png"}],"image":"ipfs://QmfQMvMwSCsJ6WSPx7tDAH4zJLQqWmx6NThDrvxU3iQ4Wq","Tattoo":"I Love You","Clothing":"Rainbow Hoodie","mediaType":"image/png","Background":"Orange","Ear Accessory":"Ruby Earrings"}}}}"""
        )
    }
}
