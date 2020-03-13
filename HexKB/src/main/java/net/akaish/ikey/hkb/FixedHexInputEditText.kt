/*
 * ---
 *
 *  Copyright (c) 2019-2020 iKey (ikey.ru)
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
import android.graphics.Typeface
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.KeyEvent

import com.google.android.material.textfield.TextInputEditText
import net.akaish.ikey.hkb.IKeyHexKeyboard.Companion.CODE_DELETE
import net.akaish.ikey.hkb.IKeyHexKeyboard.Companion.CODE_END
import net.akaish.ikey.hkb.IKeyHexKeyboard.Companion.CODE_HOME
import net.akaish.ikey.hkb.IKeyHexKeyboard.Companion.CODE_LEFT
import net.akaish.ikey.hkb.IKeyHexKeyboard.Companion.CODE_RIGHT
import java.lang.IllegalArgumentException
import java.lang.NullPointerException

class FixedHexInputEditText : TextInputEditText, HexInputField {

    companion object {
        const val placeholderCapital: Char = 'X'
        const val placeholder: Char = 'x'
        val digits = "0123456789aAbBcCdDeEfF".toCharArray()

        const val REPLACE_KEY_BEHAVIOR = 1
        const val INSERT_KEY_BEHAVIOUR = 2
        const val defaultKeyBehavior = REPLACE_KEY_BEHAVIOR

        const val defaultMask = "XX XX XX XX XX XX XX XX"
    }

    private val parameters: Parameters
    private val hexTextWatcher: HexTextWatcher
    private var customTypeFace: Typeface? = null

    private var postProcessor: FixedHexPostProcessor? = null
    private var decorator: FixedHexDecorator? = null

    constructor(context: Context) : super(context) {
        parameters = Parameters(null, defaultMask, defaultKeyBehavior, false)
        hexTextWatcher = HexTextWatcher(parameters.mask)
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        parameters = parseAttributes(context, attrs)
        hexTextWatcher = HexTextWatcher(parameters.mask)
        init()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        parameters = parseAttributes(context, attrs)
        hexTextWatcher = HexTextWatcher(parameters.mask)
        init()
    }

    private fun init() {
        super.removeTextChangedListener(hexTextWatcher)
        super.addTextChangedListener(hexTextWatcher)
        parameters.assetsFontPath?.let { customTypeFace = Typeface.createFromAsset(context.assets, it)}
        customTypeFace?.let { typeface = it }
        val last = editableText.toString()
        setText(last)
        setSelection(0)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        event?.let {
            return when (keyCode) {
                KeyEvent.KEYCODE_DEL -> del(parameters.isReplaceMode)
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
                        type(parameters.isReplaceMode, it.keyCode)
                        true
                    } else false
                }
            }
        } ?: run {
            return when (keyCode) {
                CODE_DELETE -> del(parameters.isReplaceMode)
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
                else -> type(parameters.isReplaceMode, keyCode)
            }
        }
    }

    //----------------------------------------------------------------------------------------------
    // Printing
    //----------------------------------------------------------------------------------------------
    private fun type(replaceMode: Boolean, keyCode: Int): Boolean {
        val toPrintHexOnly = String(Character.toChars(keyCode)).hexOnly()
        if(toPrintHexOnly.toCharArray().isEmpty()) return true
        val startSelection = selectionStart
        val endSelection = selectionEnd
        if(startSelection == endSelection) {
            if(startSelection < editableText.length) {
                val iterator = editableText.forwardIterator(startSelection)
                while (iterator.hasNext()) {
                    if(iterator.next().isHexDigit()) {
                        typeChar(replaceMode, toPrintHexOnly[0].toString(), iterator.position)
                        return true
                    }
                    continue
                }
            }
        } else {
            if(endSelection <= editableText.length) {
                if (parameters.fillMode) {
                    val iterator = editableText.forwardIterator(startSelection)
                    while (iterator.hasNext() && iterator.position < endSelection) {
                        if (iterator.next().isHexDigit()) typeChar(replaceMode, toPrintHexOnly[0].toString(), iterator.position)
                        continue
                    }
                } else {
                    del(replaceMode)
                    val newSelection = selectionEnd
                    if(startSelection <= editableText.length) {
                        val iterator = editableText.forwardIterator(startSelection)
                        while (iterator.hasNext()) {
                            if(iterator.next().isHexDigit()) {
                                typeChar(replaceMode, toPrintHexOnly[0].toString(), iterator.position, newSelection+1)
                                return true
                            }
                            continue
                        }
                    }
                }
            }
        }
        return true
    }

    private fun typeChar(replaceMode: Boolean, char: String, position: Int, newSelection: Int = position) {
        if (replaceMode) editableText.replace(position - 1, position, char)
        else editableText.insert(position - 1, char)
        setSelection(newSelection)
    }

    private fun del(replaceMode: Boolean): Boolean {
        val startSelection = selectionStart
        val endSelection = selectionEnd
        if(startSelection == endSelection) {
            if (startSelection > 0) {
                val backwardIterator = editableText.backwardIterator(startSelection)
                while (backwardIterator.hasNext()) {
                    if(backwardIterator.next().isHexDigit()) {
                        delChar(replaceMode, backwardIterator.position)
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
                if(isValuable) delChar(replaceMode, backwardIterator.position)
                if(backwardIterator.position == startSelection) {
                    setSelection(startSelection)
                    return true
                }
                continue
            }
        }
        return true
    }

    private fun delChar(replaceMode: Boolean, position: Int) {
        if(replaceMode) editableText.replace(position, position + 1, "0")
        else editableText.delete(position, position + 1)
    }

    //----------------------------------------------------------------------------------------------
    // Watching text
    //----------------------------------------------------------------------------------------------
    inner class HexTextWatcher constructor(private val mask: String) : TextWatcher {

        private var lock = false
        private var moveToPosition: Int? = null

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            if(!lock) {
                if (count > after) moveToPosition = start
                if (count + 1 < after) moveToPosition = -1
            }
        }

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) = Unit

        override fun afterTextChanged(s: Editable) {
            if (lock) return
            lock = true
            val iterator = s.forwardIterator()
            val sb = StringBuilder()
            for(maskChar in mask) {
                if(maskChar.isValuable()) {
                    if(iterator.hasNext())
                        while (iterator.hasNext()) {
                            val userInput = iterator.next()
                            if(userInput.isHexDigit()) {
                                sb.append(userInput.toPlaceholderCase(maskChar))
                                break
                            } else continue
                        }
                    else sb.append('0')
                } else sb.append(maskChar)
            }
            s.replace(0, s.length, sb.toString())
            val endSelection = selectionEnd

            postProcessor?.let { it.process(s) }
            decorator?.let {
                val toRemove = s.getSpans(0, s.lastIndex, KeyboardSpan::class.java)
                for(i in toRemove.indices) s.removeSpan(toRemove[i])
                it.decorate(s)
            }

            moveToPosition?.let {
                if(it > 0) setSelection(it, it).also { moveToPosition = null }
                else setSelection(s.lastIndex+1, s.lastIndex+1).also { moveToPosition = null }
            } ?: run {
                setSelection(endSelection)
            }
            lock = false
        }

    }

    //----------------------------------------------------------------------------------------------
    // Attributes parsing
    //----------------------------------------------------------------------------------------------
    data class Parameters(val assetsFontPath: String?, val mask: String, val behavior: Int, val fillMode: Boolean) {
        val isReplaceMode = behavior == REPLACE_KEY_BEHAVIOR
    }

    private fun parseAttributes(context: Context, attrs: AttributeSet): Parameters {
        var assetsFontPath: String? = null
        var mask: String? = null
        var behaviorString: String ? = null
        var fillMode = false
        var behavior: Int
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.FixedHexInputEditText)
        val count = typedArray.indexCount
        for(i in 0..count) {
            when(val attributeId = typedArray.getIndex(i)) {
                R.styleable.FixedHexInputEditText_assetsFont -> assetsFontPath = typedArray.getString(attributeId)
                R.styleable.FixedHexInputEditText_keyBehaviour -> behaviorString = typedArray.getString(attributeId)
                R.styleable.FixedHexInputEditText_mask -> mask = typedArray.getString(attributeId)
                R.styleable.FixedHexInputEditText_fillMode -> fillMode = typedArray.getBoolean(attributeId, false)
            }
        }
        typedArray.recycle()
        if(mask == null) throw NullPointerException("FixedHexInputEditText requires mask attr parameter to be set!")
        behavior = when(behaviorString) {
            "insert" -> INSERT_KEY_BEHAVIOUR
            "replace" -> REPLACE_KEY_BEHAVIOR
            "null" -> defaultKeyBehavior
            null -> defaultKeyBehavior
            else -> throw IllegalArgumentException("FixedHexInputEditText unknown value ($behaviorString) for attr parameter keyBehaviour (only null, insert and replace allowed)!")
        }
        return Parameters(assetsFontPath, mask, behavior, fillMode)
    }

    //----------------------------------------------------------------------------------------------
    // Postprocessor and decorators
    //----------------------------------------------------------------------------------------------
    interface FixedHexPostProcessor {
        fun process(source: Editable)
    }

    interface FixedHexDecorator {
        fun decorate(source: Editable)
    }

    fun setDecorator(decorator: FixedHexDecorator) {
        this.decorator = decorator
        text = text
    }

    fun setPostProcessor(postProcessor: FixedHexPostProcessor) {
        this.postProcessor = postProcessor
        text = text
    }

    //----------------------------------------------------------------------------------------------
    // Editable extensions
    //----------------------------------------------------------------------------------------------
    fun Editable.forwardIterator(startFrom: Int = 0) = ForwardEditableIterator(this, startFrom)

    fun Editable.backwardIterator(startFrom: Int = this.length) = BackwardEditableIterator(this, startFrom)

    class BackwardEditableIterator(private val editable: Editable, var position: Int = editable.length): Iterator<Char> {

        override fun hasNext() = position > 0

        override fun next(): Char  {
            position--
            return editable[position]
        }
    }

    class ForwardEditableIterator(private val editable: Editable, var position: Int = 0): Iterator<Char> {

        override fun hasNext() = editable.length > position

        override fun next(): Char  {
            val currentPosition = position
            position++
            return editable[currentPosition]
        }
    }

    //----------------------------------------------------------------------------------------------
    // String and char extensions
    //----------------------------------------------------------------------------------------------
    fun Char.isValuable() = this == placeholderCapital || this == placeholder

    fun Char.isHexDigit() = this in digits

    fun Char.toPlaceholderCase(placeholder: Char) = if(placeholder.isUpperCase()) this.toUpperCase() else this.toLowerCase()

    fun String.hexOnly(): String {
        val sb = StringBuilder()
        for(char in this.toCharArray())
            if(char.isHexDigit()) sb.append(char)
        return sb.toString()
    }
}
