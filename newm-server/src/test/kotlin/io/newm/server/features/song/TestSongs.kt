package io.newm.server.features.song

import io.newm.server.features.cardano.model.Key
import io.newm.server.features.song.model.MarketplaceStatus
import io.newm.server.features.song.model.MintingStatus
import io.newm.server.features.song.model.Song
import java.time.LocalDateTime
import java.util.*

val testSong1 = Song(
    title = "Test Song 1",
    genres = listOf("Genre 1.1", "Genre 1.2"),
    moods = listOf("Mood 1.1", "Mood 1.2"),
    coverArtUrl = "https://projectnewm.io/song1.png",
    description = "Song 1 description",
    credits = "Song 1 credits",
    duration = 11111,
    streamUrl = "https://projectnewm.io/song1.mp3",
    nftPolicyId = "NFT Policy ID 1",
    nftName = "NFT Name 1",
    mintingStatus = MintingStatus.Undistributed,
    marketplaceStatus = MarketplaceStatus.NotSelling,
    paymentKeyId = UUID.fromString("00000000-0000-0000-0000-000000000000")
)

val testSong2 = Song(
    title = "Test Song 2",
    genres = listOf("Genre 2.1", "Genre 2.2"),
    moods = listOf("Mood 2.1", "Mood 2.2"),
    coverArtUrl = "https://projectnewm.io/song2.png",
    description = "Song 2 description",
    credits = "Song 2 credits",
    duration = 22222,
    streamUrl = "https://projectnewm.io/song2.mp3",
    nftPolicyId = "NFT Policy ID 2",
    nftName = "NFT Name 2",
    mintingStatus = MintingStatus.Distributed,
    marketplaceStatus = MarketplaceStatus.Selling
)

val testKey = Key(
    id = UUID.fromString("00000000-0000-0000-0000-000000000000"),
    createdAt = LocalDateTime.now(),
    skey = ByteArray(0),
    vkey = ByteArray(0),
    address = "addr_test1vqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqd9tg5t",
    script = null,
    scriptAddress = null,
)
