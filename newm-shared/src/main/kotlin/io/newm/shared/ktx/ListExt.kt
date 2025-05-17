package io.newm.shared.ktx

fun List<String?>.containsIgnoreCase(element: String?): Boolean = this.any { it.equals(element, ignoreCase = true) }
