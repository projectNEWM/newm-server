package io.projectnewm.server.features.song

import io.projectnewm.server.features.song.model.Song

val testSong1 = Song(
    title = "Test Song 1",
    genre = "Genre 1",
    coverArtUrl = "https://projectnewm.io/song1.png",
    description = "Song 1 description",
    credits = "Song 1 credits "
)

val testSong2 = Song(
    title = "Test Song 2",
    genre = "Genre 2",
    coverArtUrl = "https://projectnewm.io/song2.png",
    description = "Song 2 description",
    credits = "Song 2 credits "
)

val testSong3 = Song(
    title = "Test Song 3",
    genre = "Genre 3",
    coverArtUrl = "https://projectnewm.io/song3.png",
    description = "Song 3 description",
    credits = "Song 3 credits "
)

val testSongs = listOf(testSong1, testSong2, testSong3)