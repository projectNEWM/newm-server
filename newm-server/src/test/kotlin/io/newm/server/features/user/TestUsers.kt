package io.newm.server.features.user

import io.newm.server.auth.password.Password
import io.newm.server.features.user.model.User

val testUser1 = User(
    firstName = "FirstName1",
    lastName = "LastName1",
    nickname = "Nickname1",
    pictureUrl = "https://projectnewm.io/pic1.jpg",
    role = "Role1",
    genre = "Genre1",
    email = "testuser1@projectnewm.io",
    newPassword = Password("password1"),
    confirmPassword = Password("password1"),
    currentPassword = Password("password2"),
    authCode = "123456"
)

val testUser2 = User(
    firstName = "FirstName2",
    lastName = "LastName2",
    nickname = "Nickname2",
    pictureUrl = "https://projectnewm.io/pic2.jpg",
    role = "Role2",
    genre = "Genre2",
    email = "testuser2@projectnewm.io",
    newPassword = Password("password2"),
    confirmPassword = Password("password2"),
    currentPassword = Password("password1"),
    authCode = "123456"
)

val testUser3 = User(
    firstName = "FirstName3",
    lastName = "LastName3",
    nickname = "Nickname3",
    pictureUrl = "https://projectnewm.io/pic2.jpg",
    role = "Role3",
    genre = "Genre3",
    email = "testuser3@projectnewm.io",
    newPassword = Password("password3"),
    confirmPassword = Password("password3"),
    currentPassword = Password("password2"),
    authCode = "123456"
)

val testUsers = listOf(testUser1, testUser2, testUser3)
