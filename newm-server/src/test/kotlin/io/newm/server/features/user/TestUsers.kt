package io.newm.server.features.user

import io.newm.shared.auth.Password
import io.newm.server.features.user.model.User

val testUser1 = User(
    firstName = "FirstName1",
    lastName = "LastName1",
    nickname = "Nickname1",
    pictureUrl = "https://projectnewm.io/pic1.jpg",
    bannerUrl = "https://projectnewm.io/banner1.jpg",
    websiteUrl = "https://user1.com",
    twitterUrl = "https://twitter.com/user1",
    location = "Location1",
    role = "Role1",
    genre = "Genre1",
    biography = "Biography1",
    walletAddress = "addr1111111111111111111111111111",
    email = "testuser1@projectnewm.io",
    newPassword = Password("Password1"),
    confirmPassword = Password("Password1"),
    currentPassword = Password("Password2"),
    authCode = "123456"
)

val testUser2 = User(
    firstName = "FirstName2",
    lastName = "LastName2",
    nickname = "Nickname2",
    pictureUrl = "https://projectnewm.io/pic2.jpg",
    bannerUrl = "https://projectnewm.io/banner2.jpg",
    websiteUrl = "https://user2.com",
    twitterUrl = "https://twitter.com/user2",
    location = "Location2",
    role = "Role2",
    genre = "Genre2",
    biography = "Biography2",
    walletAddress = "addr2222222222222222222222222222",
    email = "testuser2@projectnewm.io",
    newPassword = Password("Password2"),
    confirmPassword = Password("Password2"),
    currentPassword = Password("Password1"),
    authCode = "123456"
)
