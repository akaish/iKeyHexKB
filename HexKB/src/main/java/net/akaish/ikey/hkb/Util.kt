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

import android.content.Context
import android.text.Editable
import android.util.AttributeSet
import android.util.DisplayMetrics
import java.lang.IllegalArgumentException
import kotlin.math.roundToInt

class Util {

    companion object {
        private const val placeholderCapital: Char = 'X'
        private const val placeholder: Char = 'x'
        private val digits = "0123456789aAbBcCdDeEfF".toCharArray()

        const val REPLACE_KEY_BEHAVIOR = 1
        @Suppress("Unused")
        const val INSERT_KEY_BEHAVIOUR = 2
        const val defaultKeyBehavior = REPLACE_KEY_BEHAVIOR

        const val defaultMask = "XX XX XX XX XX XX XX XX"


        fun Char.isValuable() = this == placeholderCapital || this == placeholder

        fun Char.isHexDigit() = this in digits

        fun Char.toPlaceholderCase(placeholder: Char) = if(placeholder.isUpperCase()) this.toUpperCase() else this.toLowerCase()

        fun String.hexOnly(): String {
            val sb = StringBuilder()
            for(char in this.toCharArray())
                if(char.isHexDigit()) sb.append(char)
            return sb.toString()
        }

        //------------------------------------------------------------------------------------------
        // Editable extensions
        //------------------------------------------------------------------------------------------
        fun Editable.forwardIterator(startFrom: Int = 0) = ForwardEditableIterator(this, startFrom)

        fun Editable.backwardIterator(startFrom: Int = this.length) = BackwardEditableIterator(this, startFrom)

        //------------------------------------------------------------------------------------------
        // Attributes parsing
        //------------------------------------------------------------------------------------------
        fun parseAttributes(context: Context, attrs: AttributeSet): Attributes {
            var assetsFontPath: String? = null
            var mask: String? = null
            var behaviorString: String ? = null
            var fillMode = false
            val behavior: Int
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
            if(mask == null) mask = defaultMask
            behavior = when(behaviorString) {
                "insert" -> INSERT_KEY_BEHAVIOUR
                "replace" -> REPLACE_KEY_BEHAVIOR
                "null" -> defaultKeyBehavior
                null -> defaultKeyBehavior
                else -> throw IllegalArgumentException("FixedHexInputEditText unknown value ($behaviorString) for attr parameter keyBehaviour (only null, insert and replace allowed)!")
            }
            return Attributes(assetsFontPath, mask, behavior, fillMode)
        }

        fun dpToPx(ctx: Context, dp: Int): Int {
            val displayMetrics = ctx.resources.displayMetrics
            return (dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT)).roundToInt()
        }

        @Suppress("Unused")
        fun pxToDp(ctx: Context, px: Int): Int {
            val displayMetrics = ctx.resources.displayMetrics
            return (px / (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT)).roundToInt()
        }
    }

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
    // Attributes parsing
    //----------------------------------------------------------------------------------------------
    data class Attributes(var assetsFontPath: String?, var mask: String, val behavior: Int, val fillMode: Boolean) {
        val isReplaceMode = behavior == REPLACE_KEY_BEHAVIOR
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
}