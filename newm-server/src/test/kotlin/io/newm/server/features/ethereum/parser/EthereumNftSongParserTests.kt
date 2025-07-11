package io.newm.server.features.ethereum.parser

import com.google.common.truth.Truth.assertThat
import io.ktor.utils.io.core.toByteArray
import io.newm.server.features.ethereum.model.EthereumNft
import io.newm.server.features.ethereum.model.EthereumNftSong
import io.newm.shared.serialization.UUIDSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import org.junit.jupiter.api.Test
import java.util.UUID

class EthereumNftSongParserTests {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        isLenient = true
        serializersModule = SerializersModule {
            contextual(UUID::class, UUIDSerializer)
        }
    }

    @Test
    fun `A Little Rain Must Fall - Violetta Zironi`() {
        val nftJson =
            """
            {
                "contract": {
                    "address": "0x328B49C56a8A15fb34aB3eCD8883Fac5F9512453",
                    "name": "Another Life by Violetta Zironi",
                    "symbol": "LFE",
                    "totalSupply": "5200",
                    "tokenType": "ERC721",
                    "contractDeployer": "0x0c4956cAA320ced206049ff8dB698644a2d22769",
                    "deployedBlockNumber": 16493813,
                    "openSeaMetadata": {
                        "floorPrice": 0.005990000000000001,
                        "collectionName": "Another Life by Violetta Zironi",
                        "collectionSlug": "another-life-by-violetta-zironi",
                        "safelistRequestStatus": "verified",
                        "imageUrl": "https://i.seadn.io/gcs/files/6dd30e904fecc1d02e978b7dbef2a387.png?w=500&auto=format",
                        "description": "Another Life is a music NFT/PFP collection by Violetta Zironi:\n5 original songs recorded on tape paired with 5200 unique hand drawn PFPs inspired by the music.  \n  \nThrough the unique gamification of frame collection, holders qualify for free airdrops, weekly virtual shows, discounted or free vinyl records and prints of the artworks, free access to live concerts and even the opportunity to book Violetta for an in-person live performance.  \nAs a holder, upon discussion with Violetta and her team, you have ability to utilize the song attached for any project you are working on.  \n  \nFor more information on the utility and the roadmap visit https://violettazironi.com/anotherlifenft or join the discord.  \n  \nLink to the genesis collection Moonshot:\nhttps://market.violettazironi.com/1/collections/0x70be4e3761188d0a8c525e54bb81c4ea97712de4  \n  \nThe Another Life collection and art was funded with the help of Initiative Musik Berlin.",
                        "externalUrl": null,
                        "twitterUsername": "ZironiVioletta",
                        "discordUrl": "https://discord.gg/violetta",
                        "bannerImageUrl": "https://i.seadn.io/gcs/files/ab0fbded66ffe1724cdf6ad54c63ee88.png?w=500&auto=format",
                        "lastIngestedAt": "2025-07-01T21:48:48.000Z"
                    },
                    "isSpam": false,
                    "spamClassifications": []
                },
                "tokenId": "107",
                "tokenType": "ERC721",
                "name": "Another Life by Violetta Zironi #107",
                "description": "Another Life is a music NFT/PFP collection by Violetta Zironi: 5 original songs recorded on tape paired with 5200 unique hand drawn PFPs inspired by the music.  The songs are written and produced by Violetta Zironi and Michael Hunter Ochs.  The artworks are hand drawn by Arianna Rea.",
                "tokenUri": "https://alchemy.mypinata.cloud/ipfs/QmXFMz76HoYcoUXr2mP8aqVX9eHUTTz8omYmHuhHxuz3Gp/107",
                "image": {
                    "cachedUrl": "https://nft-cdn.alchemy.com/eth-mainnet/f495cd89e38ddfdab7aad081711bea2e",
                    "thumbnailUrl": "https://res.cloudinary.com/alchemyapi/image/upload/thumbnailv2/eth-mainnet/f495cd89e38ddfdab7aad081711bea2e",
                    "pngUrl": "https://res.cloudinary.com/alchemyapi/image/upload/convert-png/eth-mainnet/f495cd89e38ddfdab7aad081711bea2e",
                    "contentType": "image/png",
                    "size": 654563,
                    "originalUrl": "https://ipfs.io/ipfs/QmSMVf3Atwjwqkf5KooKvd2kxuvwg3XZXVzb8bwVuRRCxb/107.png"
                },
                "animation": {
                    "cachedUrl": "https://nft-cdn.alchemy.com/eth-mainnet/f495cd89e38ddfdab7aad081711bea2e_animation",
                    "contentType": "audio/mpeg",
                    "size": 3086445,
                    "originalUrl": "https://ipfs.io/ipfs/QmWAKnDzr9Yxn7wFQDkEdxq6GFLwQp2nHowVNEKJVKdKAv/A%20Little%20Rain%20Must%20Fall.mp3"
                },
                "raw": {
                    "tokenUri": "ipfs://QmXFMz76HoYcoUXr2mP8aqVX9eHUTTz8omYmHuhHxuz3Gp/107",
                    "metadata": {
                        "name": "Another Life by Violetta Zironi #107",
                        "description": "Another Life is a music NFT/PFP collection by Violetta Zironi: 5 original songs recorded on tape paired with 5200 unique hand drawn PFPs inspired by the music.  The songs are written and produced by Violetta Zironi and Michael Hunter Ochs.  The artworks are hand drawn by Arianna Rea.",
                        "image": "https://cyan-adjacent-gazelle-655.mypinata.cloud/ipfs/QmSMVf3Atwjwqkf5KooKvd2kxuvwg3XZXVzb8bwVuRRCxb/107.png",
                        "attributes": [
                            {
                                "value": "Shimmer Blue",
                                "trait_type": "Background"
                            },
                            {
                                "value": "Bronze Violet",
                                "trait_type": "Skin"
                            },
                            {
                                "value": "Flower T Shirt",
                                "trait_type": "Outfit"
                            },
                            {
                                "value": "Black Lips",
                                "trait_type": "Face Back"
                            },
                            {
                                "value": "Dublin",
                                "trait_type": "Head"
                            },
                            {
                                "value": "Chilli Pepper",
                                "trait_type": "Neck"
                            },
                            {
                                "value": "Amber",
                                "trait_type": "Frame"
                            },
                            {
                                "value": "Violetta Signature",
                                "trait_type": "Signature"
                            },
                            {
                                "value": "A Little Rain Must Fall",
                                "trait_type": "Song"
                            },
                            {
                                "value": "Violetta Zironi",
                                "trait_type": "Artist"
                            }
                        ],
                        "animation_url": "https://cyan-adjacent-gazelle-655.mypinata.cloud/ipfs/QmWAKnDzr9Yxn7wFQDkEdxq6GFLwQp2nHowVNEKJVKdKAv/A%20Little%20Rain%20Must%20Fall.mp3",
                        "dna": "276023fd4631f21a16dee3a9a8bfc47f711430d3"
                    },
                    "error": null
                },
                "collection": {
                    "name": "Another Life by Violetta Zironi",
                    "slug": "another-life-by-violetta-zironi",
                    "externalUrl": null,
                    "bannerImageUrl": "https://i.seadn.io/gcs/files/ab0fbded66ffe1724cdf6ad54c63ee88.png?w=500&auto=format"
                },
                "mint": {
                    "mintAddress": null,
                    "blockNumber": null,
                    "timestamp": null,
                    "transactionHash": null
                },
                "owners": null,
                "timeLastUpdated": "2025-05-11T02:48:08.698Z",
                "balance": "1",
              "acquiredAt": {
                    "blockTimestamp": null,
                    "blockNumber": null
                }
            }
            """.trimIndent()
        val nft = json.decodeFromString<EthereumNft>(nftJson)
        val result = nft.parseSong()
        assertThat(result).isEqualTo(
            nftSongOf(
                contractAddress = "0x328B49C56a8A15fb34aB3eCD8883Fac5F9512453",
                tokenType = "ERC721",
                tokenId = "107",
                amount = 1L,
                title = "A Little Rain Must Fall",
                imageUrl = "https://cyan-adjacent-gazelle-655.mypinata.cloud/ipfs/QmSMVf3Atwjwqkf5KooKvd2kxuvwg3XZXVzb8bwVuRRCxb/107.png",
                audioUrl = "https://cyan-adjacent-gazelle-655.mypinata.cloud/ipfs/QmWAKnDzr9Yxn7wFQDkEdxq6GFLwQp2nHowVNEKJVKdKAv/A%20Little%20Rain%20Must%20Fall.mp3",
                duration = -1L,
                artists = listOf("Violetta Zironi"),
                genres = emptyList(),
                moods = emptyList()
            )
        )
    }

    @Test
    fun `Landed in Your Heart - Violetta Zironi`() {
        val nftJson =
            """
            {
                "contract": {
                    "address": "0x328B49C56a8A15fb34aB3eCD8883Fac5F9512453",
                    "name": "Another Life by Violetta Zironi",
                    "symbol": "LFE",
                    "totalSupply": "5200",
                    "tokenType": "ERC721",
                    "contractDeployer": "0x0c4956cAA320ced206049ff8dB698644a2d22769",
                    "deployedBlockNumber": 16493813,
                    "openSeaMetadata": {
                        "floorPrice": 0.005990000000000001,
                        "collectionName": "Another Life by Violetta Zironi",
                        "collectionSlug": "another-life-by-violetta-zironi",
                        "safelistRequestStatus": "verified",
                        "imageUrl": "https://i.seadn.io/gcs/files/6dd30e904fecc1d02e978b7dbef2a387.png?w=500&auto=format",
                        "description": "Another Life is a music NFT/PFP collection by Violetta Zironi:\n5 original songs recorded on tape paired with 5200 unique hand drawn PFPs inspired by the music.  \n  \nThrough the unique gamification of frame collection, holders qualify for free airdrops, weekly virtual shows, discounted or free vinyl records and prints of the artworks, free access to live concerts and even the opportunity to book Violetta for an in-person live performance.  \nAs a holder, upon discussion with Violetta and her team, you have ability to utilize the song attached for any project you are working on.  \n  \nFor more information on the utility and the roadmap visit https://violettazironi.com/anotherlifenft or join the discord.  \n  \nLink to the genesis collection Moonshot:\nhttps://market.violettazironi.com/1/collections/0x70be4e3761188d0a8c525e54bb81c4ea97712de4  \n  \nThe Another Life collection and art was funded with the help of Initiative Musik Berlin.",
                        "externalUrl": null,
                        "twitterUsername": "ZironiVioletta",
                        "discordUrl": "https://discord.gg/violetta",
                        "bannerImageUrl": "https://i.seadn.io/gcs/files/ab0fbded66ffe1724cdf6ad54c63ee88.png?w=500&auto=format",
                        "lastIngestedAt": "2025-07-01T21:48:48.000Z"
                    },
                    "isSpam": false,
                    "spamClassifications": []
                },
                "tokenId": "124",
                "tokenType": "ERC721",
                "name": "Another Life by Violetta Zironi #124",
                "description": "Another Life is a music NFT/PFP collection by Violetta Zironi: 5 original songs recorded on tape paired with 5200 unique hand drawn PFPs inspired by the music.  The songs are written and produced by Violetta Zironi and Michael Hunter Ochs.  The artworks are hand drawn by Arianna Rea.",
                "tokenUri": "https://alchemy.mypinata.cloud/ipfs/QmXFMz76HoYcoUXr2mP8aqVX9eHUTTz8omYmHuhHxuz3Gp/124",
                "image": {
                    "cachedUrl": "https://nft-cdn.alchemy.com/eth-mainnet/eb38d25ffb8d7f3b8f6c719ded89dde1",
                    "thumbnailUrl": "https://res.cloudinary.com/alchemyapi/image/upload/thumbnailv2/eth-mainnet/eb38d25ffb8d7f3b8f6c719ded89dde1",
                    "pngUrl": "https://res.cloudinary.com/alchemyapi/image/upload/convert-png/eth-mainnet/eb38d25ffb8d7f3b8f6c719ded89dde1",
                    "contentType": "image/png",
                    "size": 804873,
                    "originalUrl": "https://ipfs.io/ipfs/QmSMVf3Atwjwqkf5KooKvd2kxuvwg3XZXVzb8bwVuRRCxb/124.png"
                },
                "animation": {
                    "cachedUrl": "https://nft-cdn.alchemy.com/eth-mainnet/eb38d25ffb8d7f3b8f6c719ded89dde1_animation",
                    "contentType": "audio/mpeg",
                    "size": 3238125,
                    "originalUrl": "https://ipfs.io/ipfs/QmWAKnDzr9Yxn7wFQDkEdxq6GFLwQp2nHowVNEKJVKdKAv/Landed%20in%20Your%20Heart.mp3"
                },
                "raw": {
                    "tokenUri": "ipfs://QmXFMz76HoYcoUXr2mP8aqVX9eHUTTz8omYmHuhHxuz3Gp/124",
                    "metadata": {
                        "name": "Another Life by Violetta Zironi #124",
                        "description": "Another Life is a music NFT/PFP collection by Violetta Zironi: 5 original songs recorded on tape paired with 5200 unique hand drawn PFPs inspired by the music.  The songs are written and produced by Violetta Zironi and Michael Hunter Ochs.  The artworks are hand drawn by Arianna Rea.",
                        "image": "https://cyan-adjacent-gazelle-655.mypinata.cloud/ipfs/QmSMVf3Atwjwqkf5KooKvd2kxuvwg3XZXVzb8bwVuRRCxb/124.png",
                        "attributes": [
                            {
                                "value": "Violet",
                                "trait_type": "Background"
                            },
                            {
                                "value": "Dandelion",
                                "trait_type": "Accessory"
                            },
                            {
                                "value": "Bronze",
                                "trait_type": "Skin"
                            },
                            {
                                "value": "Yellow Suit",
                                "trait_type": "Outfit"
                            },
                            {
                                "value": "Cosmic",
                                "trait_type": "Head"
                            },
                            {
                                "value": "Amber",
                                "trait_type": "Frame"
                            },
                            {
                                "value": "Arianna Signature",
                                "trait_type": "Signature"
                            },
                            {
                                "value": "Landed in Your Heart",
                                "trait_type": "Song"
                            },
                            {
                                "value": "Violetta Zironi",
                                "trait_type": "Artist"
                            }
                        ],
                        "animation_url": "https://cyan-adjacent-gazelle-655.mypinata.cloud/ipfs/QmWAKnDzr9Yxn7wFQDkEdxq6GFLwQp2nHowVNEKJVKdKAv/Landed%20in%20Your%20Heart.mp3",
                        "dna": "a3a78dbb69495634627510e9345b1b94e1e0dacd"
                    },
                    "error": null
                },
                "collection": {
                    "name": "Another Life by Violetta Zironi",
                    "slug": "another-life-by-violetta-zironi",
                    "externalUrl": null,
                    "bannerImageUrl": "https://i.seadn.io/gcs/files/ab0fbded66ffe1724cdf6ad54c63ee88.png?w=500&auto=format"
                },
                "mint": {
                    "mintAddress": null,
                    "blockNumber": null,
                    "timestamp": null,
                    "transactionHash": null
                },
                "owners": null,
                "timeLastUpdated": "2025-05-11T02:48:08.890Z",
                "balance": "1",
                "acquiredAt": {
                    "blockTimestamp": null,
                    "blockNumber": null
                }
            }
            """.trimIndent()
        val nft = json.decodeFromString<EthereumNft>(nftJson)
        val result = nft.parseSong()
        assertThat(result).isEqualTo(
            nftSongOf(
                contractAddress = "0x328B49C56a8A15fb34aB3eCD8883Fac5F9512453",
                tokenType = "ERC721",
                tokenId = "124",
                amount = 1L,
                title = "Landed in Your Heart",
                imageUrl = "https://cyan-adjacent-gazelle-655.mypinata.cloud/ipfs/QmSMVf3Atwjwqkf5KooKvd2kxuvwg3XZXVzb8bwVuRRCxb/124.png",
                audioUrl = "https://cyan-adjacent-gazelle-655.mypinata.cloud/ipfs/QmWAKnDzr9Yxn7wFQDkEdxq6GFLwQp2nHowVNEKJVKdKAv/Landed%20in%20Your%20Heart.mp3",
                duration = -1L,
                artists = listOf("Violetta Zironi"),
                genres = emptyList(),
                moods = emptyList()
            )
        )
    }

    private fun nftSongOf(
        contractAddress: String,
        tokenType: String,
        tokenId: String,
        amount: Long,
        title: String,
        imageUrl: String,
        audioUrl: String,
        duration: Long,
        artists: List<String>,
        genres: List<String>,
        moods: List<String>
    ): EthereumNftSong =
        EthereumNftSong(
            id = UUID.nameUUIDFromBytes(contractAddress.toByteArray() + tokenId.toByteArray()),
            contractAddress = contractAddress,
            tokenType = tokenType,
            tokenId = tokenId,
            amount = amount,
            title = title,
            imageUrl = imageUrl,
            audioUrl = audioUrl,
            duration = duration,
            artists = artists,
            genres = genres,
            moods = moods
        )
}
