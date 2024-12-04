package ru.ikey.hexkb.rc

import net.akaish.ikey.hkb.Util

data class LineItem(
    val id: Int,
    val data: String,
    val dataListener: Util.DataNotifier
)