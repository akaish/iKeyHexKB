package net.akaish.ikey.hkb.theme

import android.content.Context
import android.graphics.Color
import com.android.inputmethodservice.KeyboardViewAttributes
import net.akaish.ikey.hkb.R

@Suppress("Unused")
class ClassicTheme(context: Context) : ITheme {
    override fun xmlPadResource(): Int = R.xml.hex_keyboard_pad

    override fun xmlSendEnabledPadResource(): Int = R.xml.hex_keyboard_send_pad

    override val attributes: KeyboardViewAttributes = KeyboardViewAttributes(
        keyBackground = context.resources.getDrawable(R.drawable.btn_keyboard_key),
        keyTextColor = Color.parseColor("#FFFFFF")
    )

    override fun backgroundColor(): Int = Color.parseColor("#272727")

    override val containerSizeDp: Int = 216
}