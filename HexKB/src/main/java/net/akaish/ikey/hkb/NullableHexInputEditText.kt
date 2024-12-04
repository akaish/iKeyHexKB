package net.akaish.ikey.hkb

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
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
import net.akaish.ikey.hkb.Util.Companion.isNullableValuable
import net.akaish.ikey.hkb.Util.Companion.toPlaceholderCase
import kotlin.math.min

class NullableHexInputEditText : AbstractHexInputField {

    private var dataNotifier: Util.DataNotifier? = null
    private val maskValuableSize: Int

    fun setDataNotifier(dataNotifier: Util.DataNotifier) {
        this.dataNotifier = dataNotifier
    }

    constructor(context: Context) : super(context) {
        init(HexTextWatcher(attributes.mask))
        maskValuableSize = attributes.mask.filter { it.isNullableValuable() }.length
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(HexTextWatcher(attributes.mask))
        maskValuableSize = attributes.mask.filter { it.isNullableValuable() }.length
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(HexTextWatcher(attributes.mask))
        maskValuableSize = attributes.mask.filter { it.isNullableValuable() }.length
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        event?.let {
            return when (keyCode) {
                KeyEvent.KEYCODE_DEL -> {
                    del()
                    true
                }
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
                    } else {
                        dataNotifier?.right()
                    }
                    true
                }
                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    if(selectionStart > 0) {
                        setSelection(selectionStart - 1)
                    } else {
                        dataNotifier?.left()
                    }
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
                CODE_DELETE -> {
                    del()
                    true
                }
                CODE_LEFT -> {
                    if(selectionStart > 0) {
                        setSelection(selectionStart - 1)
                    } else {
                        dataNotifier?.left()
                    }
                    true
                }
                CODE_RIGHT -> {
                    if(selectionStart < editableText.length) {
                        setSelection(1 + selectionStart)
                    } else {
                        dataNotifier?.right()
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
                else -> {
                    type(keyCode)
                    true
                }
            }
        }
    }

    //----------------------------------------------------------------------------------------------
    // Overriding set selection
    //----------------------------------------------------------------------------------------------
    override fun setSelection(index: Int) {
        super.setSelection(index)
    }

    override fun setSelection(start: Int, stop: Int) {
        super.setSelection(start, stop)
    }

    //----------------------------------------------------------------------------------------------
    // Printing
    //----------------------------------------------------------------------------------------------
    private fun type(keyCode: Int) {
        val toPrintHexOnly = String(Character.toChars(keyCode)).hexOnly()
        if(toPrintHexOnly.toCharArray().isEmpty()) return
        val startSelection = selectionStart
        val endSelection = selectionEnd
        if(startSelection == endSelection) {
            if(startSelection < editableText.length) {
                val iterator = editableText.forwardIterator(startSelection)
                while (iterator.hasNext()) {
                    val next = iterator.next()
                    if(next.isHexDigit() || next == Util.NULL) {
                        typeChar(toPrintHexOnly[0].toString(), iterator.position)
                        return
                    }
                    continue
                }
            } else {
                dataNotifier?.right()
                dataNotifier?.onDataSizeChanged(1, toPrintHexOnly[0].toString())
            }
        } else {
            if(endSelection <= editableText.length) {
                del()
                val newSelection = selectionEnd
                if(startSelection <= editableText.length) {
                    val iterator = editableText.forwardIterator(startSelection)
                    while (iterator.hasNext()) {
                        val next = iterator.next()
                        if(iterator.next().isHexDigit() || next == Util.NULL) {
                            typeChar(toPrintHexOnly[0].toString(), iterator.position, newSelection+1)
                            return
                        }
                        continue
                    }
                }
            }
        }
        return
    }

    private fun typeChar(char: String, position: Int, newSelection: Int? = null) {
        editableText.insert(min(position - 1, minimalValidSelection()), char)
        if(newSelection != null) {
            setSelection(min(newSelection, editableText.length))
        } else {
            setSelection( min(min(position - 1, minimalValidSelection()) + 1, editableText.length) )
        }
    }

    private fun del() {
        val startSelection = selectionStart
        val endSelection = selectionEnd
        if(startSelection == endSelection) {
            if (startSelection > 0) {
                val backwardIterator = editableText.backwardIterator(startSelection)
                while (backwardIterator.hasNext()) {
                    val next = backwardIterator.next()
                    if(next.isHexDigit() || next == Util.NULL) {
                        delChar(backwardIterator.position)
                        setSelection(backwardIterator.position)
                        return
                    }
                    continue
                }
            } else {
                dataNotifier?.left()
            }
        } else {
            val backwardIterator = editableText.backwardIterator(endSelection)
            while (backwardIterator.hasNext()) {
                val next = backwardIterator.next()
                val isValuable = next.isHexDigit() || next == Util.NULL
                if(isValuable) delChar(backwardIterator.position)
                if(backwardIterator.position == startSelection) {
                    setSelection(startSelection)
                    return
                }
                continue
            }
        }
        return
    }

    private fun delChar(position: Int) {
        editableText.delete(position, position + 1)
    }

    private fun minimalValidSelection() : Int {
        val textStr = text?.toString()
        textStr?.forEachIndexed { index, c ->
            if(c == Util.NULL) {
                return if(index > 0)
                    index - 1
                else
                    0
            }
        }
        return textStr?.length ?: 0
    }

    //----------------------------------------------------------------------------------------------
    // Watching text
    //----------------------------------------------------------------------------------------------
    inner class HexTextWatcher(private val mask: String) : TextWatcher {

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
            val data = s.filter {
                it.isHexDigit()
            }
            val diffSize = data.length - maskValuableSize
            val diff = if(diffSize > 0) {
                data.substring(data.length - diffSize)
            } else ""

            val iterator = data.forwardIterator()
            val sb = StringBuilder()
            for (maskChar in mask) {
                if (maskChar.isNullableValuable()) {
                    if (iterator.hasNext()) {
                        while (iterator.hasNext()) {
                            val userInput = iterator.next()
                            if (userInput.isHexDigit() || userInput == Util.NULL) {
                                sb.append(userInput.toPlaceholderCase(maskChar))
                                break
                            } else continue
                        }
                    } else {
                        sb.append(Util.NULL)
                    }
                } else sb.append(maskChar)
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
            dataNotifier?.onDataSizeChanged(diffSize, diff)
        }

    }

    //----------------------------------------------------------------------------------------------
    // HexInputField impl
    //----------------------------------------------------------------------------------------------
    override fun resetMask(mask: String) {
        super.removeTextChangedListener(hexTextWatcher)
        attributes.mask = mask
        hexTextWatcher = HexTextWatcher(attributes.mask)
        super.addTextChangedListener(hexTextWatcher)
        setSelection(0)
    }
}