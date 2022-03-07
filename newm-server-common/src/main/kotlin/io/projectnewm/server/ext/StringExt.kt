package io.projectnewm.server.ext

import java.util.UUID

fun String.toUUID(): UUID = UUID.fromString(this)
