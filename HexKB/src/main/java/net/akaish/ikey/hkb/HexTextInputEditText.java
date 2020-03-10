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
package net.akaish.ikey.hkb;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.text.TextWatcher;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;


public class HexTextInputEditText extends TextInputEditText {

    private final Typeface clm;
    private ArrayList<TextWatcher> textWatchers = null;

    private final String path;

    public HexTextInputEditText(Context context) {
        super(context);
        path = null;
        clm = null;
    }

    public HexTextInputEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        path = getPathFromAttributes(context, attrs);
        if(path != null) {
            clm = Typeface.createFromAsset(context.getAssets(), path);
            setTypeface(clm);
        } else {
            clm = null;
        }
    }

    public HexTextInputEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        path = getPathFromAttributes(context, attrs);
        if(path != null) {
            clm = Typeface.createFromAsset(context.getAssets(), path);
            setTypeface(clm);
        } else {
            clm = null;
        }
    }

    private String getPathFromAttributes(@NonNull Context context, @Nullable AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.HexTextInputEditText);
        final int n = a.getIndexCount();
        String path = null;
        for (int i = 0; i < n; ++i) {
            int attrId = a.getIndex(i);
            if(attrId == R.styleable.HexTextInputEditText_assetsFontOld) {
                path = a.getString(attrId);
            }
        }
        a.recycle();
        return path;
    }

    @Override
    public void addTextChangedListener(TextWatcher watcher) {
        if (textWatchers == null) {
            textWatchers = new ArrayList<>();
        }
        textWatchers.add(watcher);

        super.addTextChangedListener(watcher);
    }

    @Override
    public void removeTextChangedListener(TextWatcher watcher) {
        if (textWatchers != null) {
            int i = textWatchers.indexOf(watcher);
            if (i >= 0) {
                textWatchers.remove(i);
            }
        }

        super.removeTextChangedListener(watcher);
    }

    public void clearTextChangedListeners() {
        if (textWatchers != null) {
            for (TextWatcher watcher : textWatchers) {
                super.removeTextChangedListener(watcher);
            }

            textWatchers.clear();
            textWatchers = null;
        }
    }

}
