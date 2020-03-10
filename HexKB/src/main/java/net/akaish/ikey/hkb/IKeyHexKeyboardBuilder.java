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

import android.app.Activity;

public class IKeyHexKeyboardBuilder {
    private Activity hostActivity;
    private int keyboardViewId;
    private int containerViewId = -1;
    private IKeyHexKeyboard.HideKeyboard hideKeyboardParam = null;
    private IKeyHexKeyboard.ShowKeyboard showKeyboardParam = null;
    private IKeyHexKeyboard.OnSendButton onSendButton = null;

    public IKeyHexKeyboardBuilder setHostActivity(Activity hostActivity) {
        this.hostActivity = hostActivity;
        return this;
    }

    public IKeyHexKeyboardBuilder setKeyboardViewId(int keyboardViewId) {
        this.keyboardViewId = keyboardViewId;
        return this;
    }

    public IKeyHexKeyboardBuilder setContainerViewId(int containerViewId) {
        this.containerViewId = containerViewId;
        return this;
    }

    public IKeyHexKeyboardBuilder setHideKeyboardParam(IKeyHexKeyboard.HideKeyboard hideKeyboardParam) {
        this.hideKeyboardParam = hideKeyboardParam;
        return this;
    }

    public IKeyHexKeyboardBuilder setShowKeyboardParam(IKeyHexKeyboard.ShowKeyboard showKeyboardParam) {
        this.showKeyboardParam = showKeyboardParam;
        return this;
    }

    public IKeyHexKeyboardBuilder setOnSendButton(IKeyHexKeyboard.OnSendButton onSendButton) {
        this.onSendButton = onSendButton;
        return this;
    }

    public IKeyHexKeyboard build() {
        return new IKeyHexKeyboard(hostActivity, keyboardViewId, containerViewId, hideKeyboardParam, showKeyboardParam, onSendButton);
    }
}