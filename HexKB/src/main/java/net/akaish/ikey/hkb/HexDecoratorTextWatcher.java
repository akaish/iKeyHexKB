/*
 * ---
 *
 *  Copyright (c) 2019 iKey (ikey.ru)
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
package net.akaish.ikey.hkb;

import android.os.Build;
import android.text.Editable;
import android.text.Html;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextWatcher;
import android.widget.EditText;

import androidx.annotation.NonNull;

public abstract class HexDecoratorTextWatcher implements TextWatcher {

    private EditText decoratedEditText;
    private InputFilter[] filters;

    void setEditText(@NonNull EditText editText) {
        decoratedEditText = editText;
    }

    void setFilters(@NonNull InputFilter[] filters) {
        this.filters = filters;
    }

    public abstract Spanned decorateText(CharSequence source, int start, int beforeS, int count);

    /**
     * This method is called to notify you that, within <code>s</code>,
     * the <code>count</code> characters beginning at <code>start</code>
     * are about to be replaced by new text with length <code>after</code>.
     * It is an error to attempt to make changes to <code>s</code> from
     * this callback.
     *
     * @param s param
     * @param start param
     * @param count param
     * @param after param
     */
    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

    /**
     * This method is called to notify you that, within <code>s</code>,
     * the <code>count</code> characters beginning at <code>start</code>
     * have just replaced old text that had length <code>before</code>.
     * It is an error to attempt to make changes to <code>s</code> from
     * this callback.
     *
     * @param s param
     * @param start param
     * @param before param
     * @param count param
     */
    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        decoratedEditText.removeTextChangedListener(this);
        decoratedEditText.setFilters(new InputFilter[]{});
        int startSelection = decoratedEditText.getSelectionStart();
        Spanned newText = decorateText(s, start, before, count);
        if(newText != null)
            decoratedEditText.setText(newText);
        if(startSelection > decoratedEditText.length()) startSelection = decoratedEditText.length();
        decoratedEditText.setSelection(startSelection);
        decoratedEditText.setFilters(filters);
        decoratedEditText.addTextChangedListener(this);
    }

    /**
     * This method is called to notify you that, somewhere within
     * <code>s</code>, the text has been changed.
     * It is legitimate to make further changes to <code>s</code> from
     * this callback, but be careful not to get yourself into an infinite
     * loop, because any changes you make will cause this method to be
     * called again recursively.
     * (You are not told where the change took place because other
     * afterTextChanged() methods may already have made other changes
     * and invalidated the offsets.  But if you need to know here,
     * you can use {@link android.text.Spannable#setSpan} in {@link #onTextChanged}
     * to mark your place and then look up from here where the span
     * ended up.
     *
     * @param s param
     */
    @Override
    public void afterTextChanged(Editable s) {}

    protected final Spanned fromHtmlString(String source) {
        return HexDecoratorTextWatcher.fromHtml(source);
    }

    @SuppressWarnings("deprecation")
    protected static Spanned fromHtml(String source) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Html.fromHtml(source, Html.FROM_HTML_MODE_LEGACY);
        } else {
            return Html.fromHtml(source);
        }
    }
}
