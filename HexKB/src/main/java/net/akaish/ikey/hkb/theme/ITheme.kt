package net.akaish.ikey.hkb.theme

import androidx.annotation.ColorInt
import androidx.annotation.XmlRes
import com.android.inputmethodservice.KeyboardViewAttributes

interface ITheme {
    @XmlRes fun xmlPadResource(): Int
    @XmlRes fun xmlSendEnabledPadResource(): Int?
    val attributes: KeyboardViewAttributes?
    @ColorInt fun backgroundColor(): Int
    val containerSizeDp: Int
}