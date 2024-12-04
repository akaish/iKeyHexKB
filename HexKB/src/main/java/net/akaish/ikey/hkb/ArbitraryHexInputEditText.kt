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

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.util.Log
import android.view.KeyEvent
import net.akaish.ikey.hkb.IKeyHexKeyboard.Companion.CODE_00
import net.akaish.ikey.hkb.IKeyHexKeyboard.Companion.CODE_CLEAR
import net.akaish.ikey.hkb.IKeyHexKeyboard.Companion.CODE_DELETE
import net.akaish.ikey.hkb.IKeyHexKeyboard.Companion.CODE_END
import net.akaish.ikey.hkb.IKeyHexKeyboard.Companion.CODE_FF
import net.akaish.ikey.hkb.IKeyHexKeyboard.Companion.CODE_HOME
import net.akaish.ikey.hkb.IKeyHexKeyboard.Companion.CODE_LEFT
import net.akaish.ikey.hkb.IKeyHexKeyboard.Companion.CODE_RIGHT
import net.akaish.ikey.hkb.Util.Companion.backwardIterator
import net.akaish.ikey.hkb.Util.Companion.forwardIterator
import net.akaish.ikey.hkb.Util.Companion.hexOnly
import net.akaish.ikey.hkb.Util.Companion.isHexDigit
import net.akaish.ikey.hkb.Util.Companion.toPlaceholderCase

@Suppress("Unused")
class ArbitraryHexInputEditText : AbstractHexInputField {

    constructor(context: Context) : super(context) {
        init(HexTextWatcher())
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(HexTextWatcher())
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(HexTextWatcher())
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        event?.let {
            return when (keyCode) {
                KeyEvent.KEYCODE_DEL -> del()
                KeyEvent.KEYCODE_FORWARD_DEL -> true
                KeyEvent.KEYCODE_TAB -> true
                KeyEvent.KEYCODE_MOVE_HOME -> {
                    setSelection(0)
                    true
                }
                KeyEvent.KEYCODE_MOVE_END -> {
                    setSelection(editableText.length)
                    true
                }
                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    if(selectionStart < editableText.length) {
                        setSelection(1 + selectionStart)
                    }
                    true
                }
                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    if(selectionStart > 0)
                        setSelection(selectionStart - 1)
                    true
                }
                else -> {
                    if (it.isPrintingKey) {
                        type(it.keyCode)
                        true
                    } else false
                }
            }
        } ?: run {
            return when (keyCode) {
                CODE_DELETE -> del()
                CODE_LEFT -> {
                    if(selectionStart > 0)
                        setSelection(selectionStart - 1)
                    true
                }
                CODE_RIGHT -> {
                    if(selectionStart < editableText.length) {
                        setSelection(1 + selectionStart)
                    }
                    true
                }
                CODE_HOME -> {
                    setSelection(0)
                    true
                }
                CODE_END -> {
                    setSelection(editableText.length)
                    true
                }
                CODE_CLEAR -> {
                    text = null
                    setSelection(0)
                    true
                }
                CODE_00 -> {
                    type(48)
                    type(48)
                    true
                }
                CODE_FF -> {
                    type(70)
                    type(70)
                    true
                }
                else -> type(keyCode)
            }
        }
    }

    //----------------------------------------------------------------------------------------------
    // Printing
    //----------------------------------------------------------------------------------------------
    private fun type(keyCode: Int): Boolean {
        val toPrintHexOnly = String(Character.toChars(keyCode)).hexOnly()
        if(toPrintHexOnly.toCharArray().isEmpty()) return true
        val startSelection = selectionStart
        val endSelection = selectionEnd
        if(startSelection == endSelection) {
            editableText.insert(endSelection, (toPrintHexOnly[0].toString()))
        } else {
            val iterator = editableText.forwardIterator(startSelection)
            while (iterator.hasNext()) {
                if (iterator.next().isHexDigit()) typeChar(toPrintHexOnly[0].toString(), iterator.position + 1)
                continue
            }
        }
        return true
    }

    private fun typeChar(char: String, position: Int) {
        editableText.insert(position - 1, char)
    }

    private fun del(): Boolean {
        val startSelection = selectionStart
        val endSelection = selectionEnd
        if(startSelection == endSelection) {
            if (startSelection > 0) {
                val backwardIterator = editableText.backwardIterator(startSelection)
                while (backwardIterator.hasNext()) {
                    if(backwardIterator.next().isHexDigit()) {
                        delChar(backwardIterator.position)
                        setSelection(backwardIterator.position)
                        return true
                    }
                    continue
                }
            }
        } else {
            val backwardIterator = editableText.backwardIterator(endSelection)
            while (backwardIterator.hasNext()) {
                val isValuable = backwardIterator.next().isHexDigit()
                if(isValuable) delChar(backwardIterator.position)
                if(backwardIterator.position == startSelection) {
                    setSelection(startSelection)
                    return true
                }
                continue
            }
        }
        return true
    }

    private fun delChar(position: Int) = editableText.delete(position, position + 1)

    //----------------------------------------------------------------------------------------------
    // Watching text
    //----------------------------------------------------------------------------------------------
    inner class HexTextWatcher : TextWatcher {

        private var lock = false
        private var moveToPosition: Int? = null

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) { }

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) = Unit

        override fun afterTextChanged(s: Editable) {
            if (lock) return
            lock = true

            val iterator = s.forwardIterator()
            val sb = StringBuilder()

            if (iterator.hasNext())
                while (iterator.hasNext()) {
                    val userInput = iterator.next()
                    if (userInput.isHexDigit()) {
                        sb.append(userInput.toUpperCase())
                    } else {
                        continue
                    }
                }

            s.replace(0, s.length, sb.toString())
            val endSelection: Int = selectionEnd

            postProcessorInstance?.process(s)
            decoratorInstance?.let {
                val toRemove = s.getSpans(0, s.lastIndex, KeyboardSpan::class.java)
                for(i in toRemove.indices) s.removeSpan(toRemove[i])
                it.decorate(s)
            }

            moveToPosition?.let {
                try {
                    if (it > 0) setSelection(it, it).also { moveToPosition = null }
                    else setSelection(s.lastIndex + 1, s.lastIndex + 1).also { moveToPosition = null }
                } catch (tr: Throwable) {
                    tr.printStackTrace()
                    setSelection(0)
                }
            } ?: run {
                setSelection(endSelection)
            }
            lock = false
        }

    }

    //----------------------------------------------------------------------------------------------
    // HexInputField impl
    //----------------------------------------------------------------------------------------------
    override fun resetMask(mask: String) = Unit
}