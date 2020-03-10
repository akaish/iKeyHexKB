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
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Hex keyboard
 * @author akaish
 */
public class IKeyHexKeyboard {

    public interface HideKeyboard {boolean hideKeyboard();}
    public interface ShowKeyboard {boolean showKeyboard(View v);}
    public interface KeyboardState {boolean isVisible();}
    public interface OnSendButton {
        /**
         * If true, keyboard would be hidden
         * @return true if keyboard should be hidden
         */
        boolean send(@Nullable EditText et);
    }

    private final KeyboardView keyboardView;
    private final View containerView;
    private final Activity host;

    private final HideKeyboard hideKeyboard;
    private final ShowKeyboard showKeyboard;
    private final KeyboardState state;
    private final OnSendButton onSendButton;

    private boolean isVisible = false;

    final static public int CODE_DELETE = -5;
    final static private int CODE_HIDE_KEYBOARD = -3;
    final static public int CODE_HOME = 55001;
    final static public int CODE_LEFT = 55002;
    final static public int CODE_RIGHT = 55003;
    final static public int CODE_END = 55004;
    final static private int CODE_DONE = 55005; // Don't remember code for done, using code done instead

    private final SparseArray<InputFieldPair> inputFields = new SparseArray<>();
    private int currentInputField = -1;

    class InputFieldPair {
        EditText editText;
        RMRSupportConfiguration rmrSupportConfiguration;
        IReplaceBehaviour replaceBehaviour;

        InputFieldPair(EditText editText, RMRSupportConfiguration rmrSupportConfiguration, IReplaceBehaviour replaceBehaviour) {
            this.editText = editText;
            this.rmrSupportConfiguration = rmrSupportConfiguration;
            this.replaceBehaviour = replaceBehaviour;
        }
    }

    /**
     * Need to be refactored
     */
    private KeyboardView.OnKeyboardActionListener keyboardActionListener = new KeyboardView.OnKeyboardActionListener() {

        @Override
        public void onKey(int primaryCode, int[] keyCodes) {
            if(currentInputField < 0) return;
            InputFieldPair inputFieldPair = inputFields.get(currentInputField);
            if(inputFieldPair == null) return;

            if(inputFieldPair.editText instanceof FixedHexInputEditText) {
                switch (primaryCode) {
                    case CODE_HIDE_KEYBOARD:
                        isVisible = hideKeyboard.hideKeyboard();
                        break;
                    case CODE_DONE:
                        if(onSendButton!=null) {
                            if(onSendButton.send(inputFieldPair.editText)) {
                                isVisible = hideKeyboard.hideKeyboard();
                            }
                        }
                        break;
                    default:
                        inputFieldPair.editText.onKeyDown(primaryCode, null);
                        break;
                }
                return;
            }

            Editable editable = inputFieldPair.editText.getText();
            if(editable == null) return;
            IReplaceBehaviour replaceBehaviour = inputFieldPair.replaceBehaviour;
            int start = inputFieldPair.editText.getSelectionStart();
            switch (primaryCode) {
                case CODE_DELETE:
                    if (start > 0) {
                        if(inputFieldPair.rmrSupportConfiguration != null) {
                            int skipDecoration = start - 1;
                            while (true) {
                                if(skipDecoration == 0)
                                    break;
                                if(inputFieldPair.rmrSupportConfiguration.isDecoration(editable.charAt(skipDecoration - 1)))
                                    skipDecoration--;
                                else
                                    break;
                            }
                            if(replaceBehaviour == null)
                                editable.delete(start - 1, start);
                            else {
                                if(start >= 1 && start < 3) {
                                    editable.replace(start - 1, start, Character.toString(replaceBehaviour.replaceDefault(start - 1)));
                                } else if(start > 2) {
                                    if(!inputFieldPair.rmrSupportConfiguration.isDecoration(editable.charAt(start-1))) {
                                        editable.replace(start - 1, start, Character.toString(replaceBehaviour.replaceDefault(start - 1)));
                                    }
                                }
                            }
                            inputFieldPair.editText.setSelection(skipDecoration);
                        } else {
                            if(replaceBehaviour == null)
                                editable.delete(start - 1, start);
                            else
                                editable.replace(start - 1, start, Character.toString(replaceBehaviour.replaceDefault(start - 1)));
                        }
                    }
                    break;
                case CODE_LEFT:
                    if (start > 0) inputFieldPair.editText.setSelection(start - 1);
                    break;
                case CODE_RIGHT:
                    if (start < inputFieldPair.editText.length()) inputFieldPair.editText.setSelection(start + 1);
                    break;
                case CODE_HOME:
                    inputFieldPair.editText.setSelection(0);
                    break;
                case CODE_END:
                    inputFieldPair.editText.setSelection(inputFieldPair.editText.length());
                    break;
                case CODE_HIDE_KEYBOARD:
                    isVisible = hideKeyboard.hideKeyboard();
                    break;
                case CODE_DONE:
                    if(onSendButton!=null) {
                        if(onSendButton.send(inputFieldPair.editText)) {
                            isVisible = hideKeyboard.hideKeyboard();
                        }
                    }
                    break;
                    default:
                        int skipDecoration = start + 1;
                        boolean decorationSkipAfterEdit = false;
                        if(inputFieldPair.rmrSupportConfiguration != null) {
                            if (skipDecoration < editable.length()) {
                                if (inputFieldPair.rmrSupportConfiguration.isDecoration(editable.charAt(skipDecoration))
                                        || inputFieldPair.rmrSupportConfiguration.isDecoration(editable.charAt(start))) {
                                    while (true) {
                                        if (skipDecoration == editable.length())
                                            break;
                                        if (inputFieldPair.rmrSupportConfiguration.isDecoration(editable.charAt(skipDecoration))
                                                || skipDecoration == start + 1) {
                                            skipDecoration++;
                                            decorationSkipAfterEdit = true;
                                        } else
                                            break;
                                    }
                                }
                            }
                        }
                        if(replaceBehaviour == null)
                            editable.insert(start, Character.toString((char) primaryCode));
                        else {
                            if(start < editable.length()) {
                                if(inputFieldPair.rmrSupportConfiguration != null) {
                                    if (decorationSkipAfterEdit && (skipDecoration + 1) < editable.length()) {
                                        if(inputFieldPair.rmrSupportConfiguration.isDecoration(editable.charAt(skipDecoration+1))) {
                                            editable.replace(skipDecoration - 1, skipDecoration, Character.toString((char) primaryCode));
                                        } else {
                                            editable.replace(start, start + 1, Character.toString((char) primaryCode));
                                        }
                                    } else {
                                            if (inputFieldPair.rmrSupportConfiguration.isDecoration(editable.charAt(start)))
                                                editable.replace(skipDecoration - 1, skipDecoration, Character.toString((char) primaryCode));
                                            else
                                                editable.replace(start, start + 1, Character.toString((char) primaryCode));

                                    }
                                } else {
                                    editable.replace(start, start + 1, Character.toString((char) primaryCode));
                                }
                            }
                        }
                        if(start == 0 && (inputFieldPair.editText.getSelectionStart() == editable.length()))
                            inputFieldPair.editText.setSelection(start+1);

                        inputFieldPair.editText.setSelection((editable.length() <= skipDecoration) ? editable.length() : skipDecoration);
                        break;
            }
        }

        @Override
        public void onPress(int arg0) {}

        @Override
        public void onRelease(int primaryCode) {}

        @Override
        public void onText(CharSequence text) {}

        @Override
        public void swipeDown() {}

        @Override
        public void swipeLeft() { }

        @Override
        public void swipeRight() { }

        @Override
        public void swipeUp() {}
    };

    IKeyHexKeyboard(Activity hostActivity, int keyboardViewId, int containerViewId,
                    HideKeyboard hideKeyboardParam, ShowKeyboard showKeyboardParam,
                    OnSendButton onSendButton) {
        this.host = hostActivity;
        keyboardView = hostActivity.findViewById(keyboardViewId);
        if(containerViewId > 0)
            containerView = hostActivity.findViewById(containerViewId);
        else
            containerView = null;
        this.onSendButton = onSendButton;

        if(hideKeyboardParam == null ^ showKeyboardParam == null) {
            throw new IllegalStateException("HideKeyboard and ShowKeyboard should be both null or not null!");
        }

        if(hideKeyboardParam == null) {
            this.hideKeyboard = () -> {
                if(containerView != null) containerView.setVisibility(View.GONE);
                keyboardView.setVisibility(View.GONE);
                keyboardView.setEnabled(false);
                return false;
            };
        } else {
            this.hideKeyboard = hideKeyboardParam;
        }

        if(showKeyboardParam == null) {
            this.showKeyboard = (View v) -> {
                if(containerView != null) containerView.setVisibility(View.VISIBLE);
                keyboardView.setVisibility(View.VISIBLE);
                keyboardView.setEnabled(true);
                if (v != null)
                    ((InputMethodManager) host.getSystemService(Activity.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(v.getWindowToken(), 0);
                return true;
            };
        } else {
            this.showKeyboard = showKeyboardParam;
        }

        state = () -> {
            if(hideKeyboardParam != null)
                return isVisible;
            else {
                return keyboardView.getVisibility() == View.VISIBLE;
            }
        };

        host.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        if(onSendButton == null)
            keyboardView.setKeyboard(new Keyboard(host, R.xml.hex_keyboard_pad));
        else
            keyboardView.setKeyboard(new Keyboard(host, R.xml.hex_keyboard_send_pad));
        keyboardView.setPreviewEnabled(false);

        keyboardView.setOnKeyboardActionListener(keyboardActionListener);
    }

    public boolean isStateVisible() {
        return state.isVisible();
    }

    private void hideKeyboard() {
        isVisible = hideKeyboard.hideKeyboard();
        currentInputField = -1;
    }

    /**
     * Shows hex keyboard on provided view
     * @param v
     */
    private void showKeyboard(View v) {
        isVisible = showKeyboard.showKeyboard(v);
    }

    private static final Pattern HEX_PATTERN = Pattern.compile("^\\p{XDigit}+$");

    private static final InputFilter HEX_FILTER = (CharSequence source, int start, int end, Spanned dest, int dstart, int dend) -> {
        StringBuilder sb = new StringBuilder();

        for (int i = start; i < end; i++) {
            if (!Character.isLetterOrDigit(source.charAt(i)) && !Character.isSpaceChar(source.charAt(i))) {
                continue;
            }
            //"0123456789ABCDEF";
            Matcher matcher = HEX_PATTERN.matcher(String.valueOf(source.charAt(i)));
            if (matcher.matches()) {
                sb.append(source.charAt(i));
            }

        }
        return  sb.toString().toUpperCase();
    };

    private static final InputFilter[] FILTERS = new InputFilter[]{HEX_FILTER};

    public void registerEditText(EditText editText) {
        registerEditText(editText, null, null, null);
    }

    public void registerEditText(EditText editText, @Nullable HexDecoratorTextWatcher decoratorTextWatcher) {
        registerEditText(editText, decoratorTextWatcher, null, null);
    }

    public void unregisterEditText(EditText editText) {
        if(inputFields.get(editText.getId()) != null) {
            EditText et = inputFields.get(editText.getId()).editText;
            et.setFilters(new InputFilter[0]);
            et.setOnFocusChangeListener(null);
            et.setOnClickListener(null);
            et.setOnTouchListener(null);
        }
    }

    public void registerEditText(@NonNull EditText editText, @Nullable HexDecoratorTextWatcher decoratorTextWatcher,
                                 RMRSupportConfiguration rmrSupportConfiguration, IReplaceBehaviour replaceBehaviour) {
        if(!(decoratorTextWatcher == null && rmrSupportConfiguration != null)) {
            //editText.setFilters(FILTERS);
        }

        if(decoratorTextWatcher != null) {
            decoratorTextWatcher.setEditText(editText);
            decoratorTextWatcher.setFilters(FILTERS);
            editText.addTextChangedListener(decoratorTextWatcher);
        }

        editText.setOnFocusChangeListener((View v, boolean hasFocus) -> {
            if( hasFocus ) {
                showKeyboard(v);
                currentInputField = v.getId();
            } else hideKeyboard();});

        editText.setOnClickListener(this::showKeyboard);

        editText.setOnTouchListener((v, event) -> {
                if (Build.VERSION.SDK_INT >= 21) {
                    editText.setShowSoftInputOnFocus(false);
                } else if (Build.VERSION.SDK_INT >= 14) {
                    try {
                        final Method method = EditText.class.getMethod(
                                "setShowSoftInputOnFocus"
                                , new Class[]{boolean.class});
                        method.setAccessible(true);
                        method.invoke(editText, false);
                    } catch (Exception e) {
                        Log.w("IKEY:HEXKB", "Error on hiding system keyboard!", e);
                    }
                }

                editText.onTouchEvent(event); // Call native handler
                return true; // Consume touch event
        });

        inputFields.put(editText.getId(), new InputFieldPair(editText, rmrSupportConfiguration, replaceBehaviour));

        editText.setInputType(editText.getInputType() | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
    }

    /**
     * Returns true if keyboard opened and closes it, use this to
     * implement onBackPressed() keyboard closing
     * @return true if keyboard was opened and successfully was closed
     */
    public boolean onBackPressed() {
        if(state.isVisible()) {
            isVisible = hideKeyboard.hideKeyboard();
            return true;
        }
        return false;
    }
}
