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
package ru.ikey.hexkb;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.InputType;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import net.akaish.ikey.hkb.ArbitraryHexInputEditText;
import net.akaish.ikey.hkb.FixedHexInputEditText;
import net.akaish.ikey.hkb.IKeyHexKeyboard;
import net.akaish.ikey.hkb.KeyboardForegroundColorSpan;
import net.akaish.ikey.hkb.theme.ClassicTheme;
import net.akaish.ikey.hkb.theme.ITheme;
import net.akaish.ikey.hkb.theme.ModernTheme;

import org.apache.commons.codec.binary.Hex;

import java.util.Arrays;

import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import ru.ikey.hexkb.ds.CRC8Dallas;

import static java.text.MessageFormat.format;

import com.android.inputmethodservice.KeyboardView;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.kf_dallas_crc_switch) Switch dallasCrcSwitch;
    @BindView(R.id.kf_dallas_keycode_field) FixedHexInputEditText dallasKeyCodeET;
    @BindView(R.id.simple_1_text) FixedHexInputEditText simpleText1;
    @BindView(R.id.simple_2_text) FixedHexInputEditText simpleText2;
    @BindView(R.id.simple_3_text) FixedHexInputEditText simpleText3;
    @BindView(R.id.simple_4_text) FixedHexInputEditText simpleText4;
    @BindView(R.id.simple_5_text) ArbitraryHexInputEditText simpleText5;
    private static final String LINK_PATTERN = "<a href=\"{0}\">{1}</a>";

    @BindString(R.string.about_app_text) String aboutAppText;
    @BindString(R.string.about_app_link) String aboutAppLink;
    @BindString(R.string.about_app_link_text) String aboutAppLinkText;

    @BindView(R.id.appInfo) TextView aboutAppTV;

    private static final CRC8Dallas crc8Dallas = new CRC8Dallas(true);

    //----------------------------------------------------------------------------------------------
    // Hex keyboard
    //----------------------------------------------------------------------------------------------
    protected IKeyHexKeyboard hexKeyboard = null;

    void initHKB(View rootView) {

        ITheme theme = new ClassicTheme(this);
        KeyboardView keyboardView = new KeyboardView(this, theme.getAttributes());
        RelativeLayout container = rootView.findViewById(R.id.ikey_main_hex_kb_container);
        container.removeAllViews();
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
        keyboardView.setFocusable(true);
        keyboardView.setFocusableInTouchMode(true);
        keyboardView.setVisibility(View.GONE);

        keyboardView.setLayoutParams(layoutParams);


        container.addView(keyboardView);

        hexKeyboard = null;
        hexKeyboard = new IKeyHexKeyboard.Builder()
                .withHost(this)
                .withKeyboardView(keyboardView)
                .withContainerView(container)
                .withTheme(theme)
                .build();
    }

    //----------------------------------------------------------------------------------------------
    // Click listeners
    //----------------------------------------------------------------------------------------------
    @OnCheckedChanged(R.id.kf_dallas_crc_switch)
    void onDallasCRCSwitchChecked() {
        if(dallasCrcSwitch.isChecked()) {
            int selection = dallasKeyCodeET.getSelectionStart();
            if(dallasKeyCodeET.getText() != null) {
                dallasKeyCodeET.setText(dallasKeyCodeET.getText().toString().replace(" ", ""));
                dallasKeyCodeET.setSelection(selection);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        adjustFontScale( getResources().getConfiguration());
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        dallasKeyCodeET.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

        initHKB(findViewById(R.id.main_view));

        hexKeyboard.registerInput(simpleText1);

        simpleText1.setDecorator(editable -> editable.setSpan(new KeyboardForegroundColorSpan(Color.BLUE), 0, 2, Spanned.SPAN_INCLUSIVE_EXCLUSIVE));

        simpleText2.setOnFocusChangeListener((v, onFocused) -> Log.e("134", "BEFORE"));
        hexKeyboard.registerInput(simpleText2);
        hexKeyboard.registerInput(simpleText3);
        simpleText3.setOnFocusChangeListener((v, onFocused) -> Log.e("134", "AFTER"));
        hexKeyboard.registerInput(simpleText4);
        hexKeyboard.registerInput(simpleText5);

        hexKeyboard.registerInput(dallasKeyCodeET);
        dallasKeyCodeET.setPostProcessor( editable -> {
            if(dallasCrcSwitch.isChecked()) {
                String code = editable.toString().replace(" ", "").replace(":", "");
                try {
                    byte[] codeBytes = Hex.decodeHex(code);
                    crc8Dallas.reset();
                    crc8Dallas.update(Arrays.copyOfRange(codeBytes, 1, codeBytes.length));
                    String newCRC = Long.toHexString(crc8Dallas.getValue()).toUpperCase();
                    if(newCRC.length() < 2) newCRC = "0" + newCRC;
                    editable.replace(0, 2, newCRC);
                } catch (Throwable tr) { tr.printStackTrace(); }
            }
        });

        dallasKeyCodeET.setDecorator( editable -> {
            String code = editable.toString().replace(" ", "").replace(":", "");
            try {
                byte[] codeBytes = Hex.decodeHex(code);
                crc8Dallas.reset();
                crc8Dallas.update(Arrays.copyOfRange(codeBytes, 1, codeBytes.length));
                long calculatedCRC = crc8Dallas.getValue();
                long currentCRC = Long.parseLong(editable.subSequence(0, 2).toString(), 16);
                if(currentCRC == calculatedCRC) {
                    editable.setSpan(new KeyboardForegroundColorSpan(Color.BLUE), 0, 2, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                } else {
                    editable.setSpan(new KeyboardForegroundColorSpan(Color.RED), 0, 2, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                }
            } catch (Throwable tr) { tr.printStackTrace(); }
        });

        aboutAppTV.setText(buildHTMLString());
        aboutAppTV.setMovementMethod(LinkMovementMethod.getInstance());
    }

    @Override public void onBackPressed() {
        if(!hexKeyboard.onBackPressed())
            super.onBackPressed();
    }

    //----------------------------------------------------------------------------------------------
    // About app
    //----------------------------------------------------------------------------------------------
    private Spanned buildHTMLString() {
        String link = format(LINK_PATTERN, aboutAppLink, aboutAppLinkText);
        String html = format(aboutAppText, link);
        return fromHtml(html);
    }

    @SuppressWarnings("deprecation")
    private static Spanned fromHtml(String source) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Html.fromHtml(source, Html.FROM_HTML_MODE_LEGACY);
        } else {
            return Html.fromHtml(source);
        }
    }

    //----------------------------------------------------------------------------------------------
    // Misc
    //----------------------------------------------------------------------------------------------
    void adjustFontScale( Configuration configuration) {
        configuration.fontScale = (float) 1.0;
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(metrics);
        metrics.scaledDensity = configuration.fontScale * metrics.density;
        getBaseContext().getResources().updateConfiguration(configuration, metrics);

    }
}

