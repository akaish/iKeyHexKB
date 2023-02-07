/*
 * ---
 *
 *  Copyright (c) 2019-2023 iKey (ikey.ru)
 *  Author: Denis Bogomolov (akaish)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This file is a part of Android Hex Keyboard, more info at
 * https://ikey.ru
 *
 * ---
 */
package net.akaish.ikey.hkb

import android.annotation.SuppressLint
import android.app.Activity
import com.android.inputmethodservice.Keyboard
import com.android.inputmethodservice.KeyboardView
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.util.Log
import android.util.LongSparseArray
import android.view.MotionEvent
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.core.util.set
import androidx.core.util.size
import net.akaish.ikey.hkb.Util.Companion.dpToPx
import net.akaish.ikey.hkb.theme.ITheme

class IKeyHexKeyboard(val host: Activity,
                      private val keyboardView: KeyboardView,
                      private val containerView: View?,
                      hideKeyboardParam: HideKeyboard?,
                      showKeyboardParam: ShowKeyboard?,
                      private val onSendButton: OnSendButton?,
                      theme: ITheme? = null) {

    companion object {
        const val CODE_DELETE = -5
        private const val CODE_HIDE_KEYBOARD = -3
        const val CODE_HOME = 55001
        const val CODE_LEFT = 55002
        const val CODE_RIGHT = 55003
        const val CODE_END = 55004
        private const val CODE_DONE = 55005 // Don't remember code for done, using code done instead
        const val CODE_00 = -101
        const val CODE_FF = -102
        const val CODE_CLEAR = -103

    }

    private lateinit var hideKeyboard: HideKeyboard
    private val showKeyboard: ShowKeyboard
    private val state: KeyboardState

    interface HideKeyboard { fun hideKeyboard(): Boolean }
    interface ShowKeyboard { fun showKeyboard(v: View?): Boolean }
    interface KeyboardState { fun isVisible(): Boolean }
    interface OnSendButton {
        /**
         * If true, keyboard would be hidden
         * @return true if keyboard should be hidden
         */
        fun send(value: EditText?): Boolean
    }

    private var isVisible = false
    private var isEnabled = true
    private val registeredInputs = LongSparseArray<AbstractHexInputField>()
    private var currentInputField: Long? = null

    @Suppress("Unused")
    fun enable() { isEnabled = true }

    @Suppress("Unused")
    fun disable() { isEnabled = false }

    @Suppress("Unused")
    fun isEnabled() = isEnabled

    private val keyboardActionListener = object : KeyboardView.OnKeyboardActionListener {

        override fun onKey(primaryCode: Int, keyCodes: IntArray?) {
            currentInputField?.let { fieldId ->
                registeredInputs[fieldId]?.let { inputField ->
                    when(primaryCode) {
                        CODE_HIDE_KEYBOARD -> isVisible = hideKeyboard.hideKeyboard()
                        CODE_DONE -> {
                            onSendButton?.let {
                                if(it.send(inputField)) {
                                    isVisible = hideKeyboard.hideKeyboard()
                                }
                            }
                        }
                        else -> inputField.onKeyDown(primaryCode, null)
                    }
                }
            }

        }

        //override fun onText(text: CharSequence?) = Unit
        override fun swipeRight() = Unit
        //override fun onPress(primaryCode: Int) = Unit
        //override fun onRelease(primaryCode: Int) = Unit
        override fun swipeLeft() = Unit
        override fun swipeUp() = Unit
        override fun swipeDown() = Unit
    }

    init {
        require(!((hideKeyboardParam == null) xor (showKeyboardParam == null))) { " HideKeyboard and ShowKeyboard should be both null or not null! " }
        hideKeyboard = hideKeyboardParam ?: object : HideKeyboard {
            override fun hideKeyboard(): Boolean {
                containerView?.let { it.visibility = GONE }
                keyboardView.visibility = GONE
                keyboardView.isEnabled = false
                return false
            }
        }
        showKeyboard = showKeyboardParam ?: object : ShowKeyboard {
            override fun showKeyboard(v: View?): Boolean {
                containerView?.let { it.visibility = VISIBLE }
                keyboardView.visibility = VISIBLE
                keyboardView.isEnabled = true
                v?.let { (host.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(v.windowToken, 0) }
                return true
            }
        }
        state = object : KeyboardState {
            override fun isVisible() = if(hideKeyboardParam != null) isVisible else keyboardView.visibility == VISIBLE
        }
        host.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)

        if(theme == null) {
            if (onSendButton == null) {
                keyboardView.keyboard = Keyboard(host, R.xml.hex_keyboard_pad)
            } else {
                keyboardView.keyboard = Keyboard(host, R.xml.hex_keyboard_send_pad)
            }
        } else {
            if (onSendButton == null) {
                keyboardView.keyboard = Keyboard(host, theme.xmlPadResource())
            } else {
                val sendEnabledPad = theme.xmlSendEnabledPadResource()
                require(sendEnabledPad != null)
                keyboardView.keyboard = Keyboard(host, sendEnabledPad)
            }
            containerView?.setBackgroundColor(theme.backgroundColor())
            keyboardView.setBackgroundColor(theme.backgroundColor())

            val clp = containerView?.layoutParams
            clp?.height = dpToPx(host, theme.containerSizeDp)
            containerView?.layoutParams = clp
        }

        keyboardView.isPreviewEnabled = false

        keyboardView.setOnKeyboardActionListener(keyboardActionListener)
    }

    @Suppress("Unused")
    fun isStateVisible() = state.isVisible()

    private fun showKeyboard(v: View) {
        if(!isEnabled) return
        isVisible = showKeyboard.showKeyboard(v)
    }

    /**
     * Returns true if keyboard opened and closes it, use this to
     * implement onBackPressed() keyboard closing
     * @return true if keyboard was opened and successfully was closed
     */
    fun onBackPressed(): Boolean {
        return if(state.isVisible()) {
            isVisible = hideKeyboard.hideKeyboard()
            true
        } else false
    }

    @Suppress("Unused")
    fun registerInputs(vararg editTexts: EditText) {
        for(input in editTexts) registerInput(input)
    }

    private val closeKeyboardTask = Runnable {
        hideKeyboard.hideKeyboard()
    }
    private val closeKeyboardTaskHandler = Handler(Looper.getMainLooper())
    private val closeKeyboardDelay = 20L

    @SuppressLint("ClickableViewAccessibility", "ObsoleteSdkInt")
    fun registerInput(editText: EditText) {
        check(editText is AbstractHexInputField) { "Provided text input is not instance of AbstractHexInputField!" }

        editText.setOnFocusChangeListenerWrapper(View.OnFocusChangeListener { v, hasFocus ->
            if (!isEnabled) return@OnFocusChangeListener
            if (hasFocus) {
                closeKeyboardTaskHandler.removeCallbacks(closeKeyboardTask)
                showKeyboard(v).also { currentInputField = (v as AbstractHexInputField).fieldId }
            } else {
                closeKeyboardTaskHandler.postDelayed(closeKeyboardTask, closeKeyboardDelay)
            }
        })

        editText.setOnClickListenerWrapper( View.OnClickListener { v -> this.showKeyboard(v)})

        editText.setOnTouchListenerWrapper(View.OnTouchListener { _: View, event: MotionEvent ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                editText.setShowSoftInputOnFocus(false)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                try {
                    val method = EditText::class.java.getMethod(
                            "setShowSoftInputOnFocus", Boolean::class.javaPrimitiveType)
                    method.isAccessible = true
                    method.invoke(editText, false)
                } catch (e: Exception) {
                    Log.w("IKEY:HEXKB", "Error on hiding system keyboard!", e)
                }
            }
            editText.onTouchEvent(event)
            true
        })

        registeredInputs[editText.fieldId] = editText
        editText.setInputType(editText.getInputType() or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS)
    }

    @Suppress("Unused")
    fun unregisterInput(editText: EditText) {
        if(editText is AbstractHexInputField) {
            registeredInputs[editText.fieldId]?.let {
                it.setOnFocusChangeListenerWrapper(null)
                it.setOnClickListenerWrapper(null)
                it.setOnTouchListenerWrapper(null)
            }
        }
    }

    @Suppress("Unused")
    fun unregisterAllInputs() {
        for(i in 0 until registeredInputs.size) unregisterInput(registeredInputs.valueAt(i))
    }

    @Suppress("Unused")
    class Builder {
        private lateinit var host: Activity
        private var keyboardViewId: Int = -1
        private var containerViewId: Int = -1
        private var hideKeyboard: HideKeyboard? = null
        private var showKeyboard: ShowKeyboard? = null
        private var onSendButton: OnSendButton? = null
        private var containerView: View? = null
        private var keyboardView: KeyboardView? = null
        private var theme: ITheme? = null

        fun withHost(activity: Activity) = apply { host = activity }
        fun withKeyboardViewId(id: Int) = apply { keyboardViewId = id }
        fun withContainerViewId(id: Int) = apply { containerViewId = id }
        fun withKeyboardView(keyboardView: KeyboardView) = apply { this.keyboardView = keyboardView }
        fun withContainerView(containerView: View?) = apply { this.containerView = containerView }
        fun withHideKeyboard(hideKeyboard: HideKeyboard?) = apply { this.hideKeyboard = hideKeyboard }
        fun withShowKeyboard(showKeyboard: ShowKeyboard?) = apply { this.showKeyboard = showKeyboard }
        fun withOnSendButton(onSendButton: OnSendButton?) = apply { this.onSendButton = onSendButton }
        fun withTheme(theme: ITheme?) = apply { this.theme = theme }

        fun build(): IKeyHexKeyboard {
            if(keyboardView == null) keyboardView = host.findViewById(keyboardViewId)
            if(containerView == null)
                containerView = if(containerViewId > 0) host.findViewById(containerViewId) else null
            return IKeyHexKeyboard(host, keyboardView!!, containerView, hideKeyboard, showKeyboard, onSendButton, theme)
        }
    }
}