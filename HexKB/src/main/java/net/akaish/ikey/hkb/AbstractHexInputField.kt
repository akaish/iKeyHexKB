/*
 * ---
 *
 *  Copyright (c) 2019-2022 iKey (ikey.ru)
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
import android.content.Context
import android.graphics.Typeface
import android.text.TextWatcher
import android.util.AttributeSet
import com.google.android.material.textfield.TextInputEditText
import java.util.*

abstract class AbstractHexInputField : TextInputEditText, HexInputField {

    override val fieldId = UUID.randomUUID().mostSignificantBits
    protected val attributes: Util.Attributes
    protected var hexTextWatcher: TextWatcher? = null
    private var customTypeFace: Typeface? = null

    protected var postProcessorInstance: Util.FixedHexPostProcessor? = null
    protected var decoratorInstance: Util.FixedHexDecorator? = null

    constructor(context: Context) : super(context) {
        attributes = Util.Attributes(null, Util.defaultMask, Util.defaultKeyBehavior, false)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        attributes = Util.parseAttributes(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        attributes = Util.parseAttributes(context, attrs)
    }

    protected fun init(textWatcher: TextWatcher) {
        hexTextWatcher = textWatcher
        super.removeTextChangedListener(hexTextWatcher)
        super.addTextChangedListener(hexTextWatcher)
        attributes.assetsFontPath?.let { customTypeFace = Typeface.createFromAsset(context.assets, it)}
        customTypeFace?.let { typeface = it }
        val last = editableText.toString()
        setText(last)
        setSelection(0)
    }


    //----------------------------------------------------------------------------------------------
    // HexInputField impl
    //----------------------------------------------------------------------------------------------
    override fun setAssetsFont(fontPath: String?) {
        attributes.assetsFontPath = fontPath
        attributes.assetsFontPath?.let { customTypeFace = Typeface.createFromAsset(context.assets, it)}
        customTypeFace?.let { typeface = it }
    }

    override fun setDecorator(decorator: Util.FixedHexDecorator) {
        this.decoratorInstance = decorator
        text = text
    }

    override fun setPostProcessor(postProcessor: Util.FixedHexPostProcessor) {
        this.postProcessorInstance = postProcessor
        text = text
    }

    //----------------------------------------------------------------------------------------------
    // Wrapping OnTouch, OnClick and OnFocus
    //----------------------------------------------------------------------------------------------
    // OnClick
    private var onClickListener: OnClickListener? = null
    private var onClickListenerWrapper: OnClickListener? = null

    private fun createWrappedOnClickListener() {
        onClickListener.let { onClc ->
            onClickListenerWrapper.let { onClcWrapper ->
                when {
                    onClc == null && onClcWrapper == null -> super.setOnClickListener(null)
                    onClc != null && onClcWrapper == null -> super.setOnClickListener(onClc)
                    onClc == null && onClcWrapper != null -> super.setOnClickListener(onClcWrapper)
                    onClc != null && onClcWrapper != null -> super.setOnClickListener {
                        onClcWrapper.onClick(it)
                        onClc.onClick(it)
                    }
                }
            }
        }
    }

    override fun setOnClickListener(l: OnClickListener?) {
        onClickListener = l
        createWrappedOnClickListener()
    }

    fun setOnClickListenerWrapper(l: OnClickListener?) {
        onClickListenerWrapper = l
        createWrappedOnClickListener()
    }

    // OnTouch
    private var onTouchListener: OnTouchListener? = null
    private var onTouchListenerWrapper: OnTouchListener? = null

    @SuppressLint("ClickableViewAccessibility")
    private fun createWrappedOnTouchListener() {
        onTouchListener.let { onTouch ->
            onTouchListenerWrapper.let { onTouchWrapper ->
                when {
                    onTouch == null && onTouchWrapper == null -> super.setOnTouchListener(null)
                    onTouch != null && onTouchWrapper == null -> super.setOnTouchListener(onTouch)
                    onTouch == null && onTouchWrapper != null -> super.setOnTouchListener(onTouchWrapper)
                    onTouch != null && onTouchWrapper != null -> super.setOnTouchListener { v, mE ->
                        onTouchWrapper.onTouch(v, mE)
                        onTouch.onTouch(v, mE)
                    }
                }
            }
        }
    }

    override fun setOnTouchListener(l: OnTouchListener?) {
        onTouchListener = l
        createWrappedOnTouchListener()
    }

    fun setOnTouchListenerWrapper(l: OnTouchListener?) {
        onTouchListenerWrapper = l
        createWrappedOnTouchListener()
    }

    // OnFocus
    private var onFocusChangeListenerGeneric: OnFocusChangeListener? = null
    private var onFocusChangeListenerWrapper: OnFocusChangeListener? = null

    private fun createWrappedOnFocusChangedListener() {
        onFocusChangeListenerGeneric.let { onFocus ->
            onFocusChangeListenerWrapper.let { onFocusWrapper ->
                when {
                    onFocus == null && onFocusWrapper == null -> super.setOnFocusChangeListener(null)
                    onFocus != null && onFocusWrapper == null -> super.setOnFocusChangeListener(onFocus)
                    onFocus == null && onFocusWrapper != null -> super.setOnFocusChangeListener(onFocusWrapper)
                    onFocus != null && onFocusWrapper != null -> super.setOnFocusChangeListener { v, hasFocus ->
                        onFocusWrapper.onFocusChange(v, hasFocus)
                        onFocus.onFocusChange(v, hasFocus)
                    }
                }
            }
        }
    }

    override fun setOnFocusChangeListener(l: OnFocusChangeListener?) {
        onFocusChangeListenerGeneric = l
        createWrappedOnFocusChangedListener()
    }

    fun setOnFocusChangeListenerWrapper(l: OnFocusChangeListener?) {
        onFocusChangeListenerWrapper = l
        createWrappedOnFocusChangedListener()
    }

}