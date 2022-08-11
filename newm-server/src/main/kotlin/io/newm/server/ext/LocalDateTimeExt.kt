package io.projectnewm.server.ext

import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date

fun LocalDateTime.toDate(): Date = atZone(ZoneId.systemDefault()).toInstant().toDate()
