package net.akaish.ikey.hkb.theme

import android.content.Context
import android.graphics.Color
import com.android.inputmethodservice.KeyboardViewAttributes
import net.akaish.ikey.hkb.R

@Suppress("Unused")
class ModernTheme(context: Context) : ITheme {
    override fun xmlPadResource(): Int = R.xml.hex_keyboard_modern_pad

    override fun xmlSendEnabledPadResource(): Int = R.xml.hex_keyboard_modern_send_pad

    override val attributes: KeyboardViewAttributes = KeyboardViewAttributes(
        keyBackground = context.resources.getDrawable(R.drawable.modern_button),
        keyTextColor = Color.parseColor("#FFFFFF")
    )

    override fun backgroundColor(): Int = Color.parseColor("#f0f0f0")

    override val containerSizeDp: Int = 226
}