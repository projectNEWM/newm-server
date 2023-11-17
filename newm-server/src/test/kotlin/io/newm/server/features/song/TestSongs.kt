package io.newm.server.features.song

import io.newm.server.features.song.model.Song
import io.newm.server.features.song.model.SongBarcodeType
import java.time.LocalDate

val testSong1 = Song(
    archived = false,
    title = "Test Song 1",
    genres = listOf("Genre 1.1", "Genre 1.2"),
    moods = listOf("Mood 1.1", "Mood 1.2"),
    coverArtUrl = "https://projectnewm.io/song1.png",
    description = "Song 1 description",
    album = "Song 1 album",
    track = 1,
    language = "Song 1 language",
    coverRemixSample = false,
    compositionCopyrightOwner = "Song 1 compositionCopyrightOwner",
    compositionCopyrightYear = 1111,
    phonographicCopyrightOwner = "Song 1 phonographicCopyrightOwner",
    phonographicCopyrightYear = 2222,
    parentalAdvisory = "Song 1 parentalAdvisory",
    barcodeType = SongBarcodeType.Upc,
    barcodeNumber = "Barcode 1",
    isrc = "Song 1 isrc",
    iswc = "Song 1 iswc",
    ipis = listOf("Song 1 ipi 0", "Song 1 ipi 1"),
    releaseDate = LocalDate.of(2023, 1, 1),
    publicationDate = LocalDate.of(2023, 1, 2),
    lyricsUrl = "https://projectnewm.io/lirycs1.txt",
)

val testSong2 = Song(
    archived = true,
    title = "Test Song 2",
    genres = listOf("Genre 2.1", "Genre 2.2"),
    moods = listOf("Mood 2.1", "Mood 2.2"),
    coverArtUrl = "https://projectnewm.io/song2.png",
    description = "Song 2 description",
    album = "Song 2 album",
    track = 2,
    language = "Song 2 language",
    coverRemixSample = true,
    compositionCopyrightOwner = "Song 2 compositionCopyrightOwner",
    compositionCopyrightYear = 2222,
    phonographicCopyrightOwner = "Song 2 phonographicCopyrightOwner",
    phonographicCopyrightYear = 1111,
    parentalAdvisory = "Song 2 parentalAdvisory",
    barcodeType = SongBarcodeType.Ean,
    barcodeNumber = "Barcode 2",
    isrc = "Song 2 isrc",
    iswc = "Song 2 iswc",
    ipis = listOf("Song 2 ipi 0", "Song 2 ipi 1"),
    releaseDate = LocalDate.of(2023, 2, 2),
    publicationDate = LocalDate.of(2023, 2, 3),
    lyricsUrl = "https://projectnewm.io/lirycs2.txt",
)
