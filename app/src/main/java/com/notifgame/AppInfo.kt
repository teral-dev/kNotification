package com.notifgame

data class AppInfo(
    val label: String,
    val packageName: String
) {
    override fun toString(): String = label
}
