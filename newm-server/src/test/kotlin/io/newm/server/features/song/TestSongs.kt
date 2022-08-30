package io.newm.server.features.song

import io.newm.server.features.song.model.Song

val testSong1 = Song(
    title = "Test Song 1",
    genre = "Genre 1",
    coverArtUrl = "https://projectnewm.io/song1.png",
    description = "Song 1 description",
    credits = "Song 1 credits",
    streamUrl = "https://projectnewm.io/song1.mp3",
    nftPolicyId = "NFT Policy ID 1",
    nftName = "NFT Name 1"
)

val testSong2 = Song(
    title = "Test Song 2",
    genre = "Genre 2",
    coverArtUrl = "https://projectnewm.io/song2.png",
    description = "Song 2 description",
    credits = "Song 2 credits",
    streamUrl = "https://projectnewm.io/song2.mp3",
    nftPolicyId = "NFT Policy ID 2",
    nftName = "NFT Name 2"
)

val testSong3 = Song(
    title = "Test Song 3",
    genre = "Genre 3",
    coverArtUrl = "https://projectnewm.io/song3.png",
    description = "Song 3 description",
    credits = "Song 3 credits",
    streamUrl = "https://projectnewm.io/song3.mp3",
    nftPolicyId = "NFT Policy ID 3",
    nftName = "NFT Name 3"
)

val testSongs = listOf(testSong1, testSong2, testSong3)
