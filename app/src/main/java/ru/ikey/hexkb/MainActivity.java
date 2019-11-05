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
package ru.ikey.hexkb;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.InputType;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.redmadrobot.inputmask.MaskedTextChangedListener;

import net.akaish.ikey.hkb.ColoredPairsDecoratorConfigurationBuilder;
import net.akaish.ikey.hkb.ColoredPairsDecoratorTextWatcher;
import net.akaish.ikey.hkb.IKeyHexKeyboard;
import net.akaish.ikey.hkb.IKeyHexKeyboardBuilder;
import net.akaish.ikey.hkb.IReplaceBehaviour;
import net.akaish.ikey.hkb.RMRSupportConfiguration;

import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import ru.ikey.hexkb.ds.CRC8Dallas;
import ru.ikey.hexkb.ds.DallasDecoratorConfiguration;
import ru.ikey.hexkb.ds.DallasDecoratorConfigurationBuilder;
import ru.ikey.hexkb.ds.DallasDecoratorTextWatcher;
import ru.ikey.hexkb.ds.DallasRMR;

import static java.text.MessageFormat.format;

public class MainActivity extends AppCompatActivity implements DallasRMR.IDallasInput {

    @BindView(R.id.kf_dallas_crc_switch) Switch dallasCrcSwitch;
    @BindView(R.id.kf_dallas_keycode_field) TextInputEditText dallasKeyCodeET;
    @BindView(R.id.simple_text) TextInputEditText simpleText;
    @BindView(R.id.coloured_text) TextInputEditText decoratedText;

    private static final String LINK_PATTERN = "<a href=\"{0}\">{1}</a>";

    @BindString(R.string.about_app_text) String aboutAppText;
    @BindString(R.string.about_app_link) String aboutAppLink;
    @BindString(R.string.about_app_link_text) String aboutAppLinkText;

    @BindView(R.id.appInfo) TextView aboutAppTV;

    private static final String DALLAS_INITIAL_VALUE = "3D00000000000001";
    private static final String E8_PATTERN = "[__]:[__] [__] [__] [__] [__] [__] [__]";
    private static final IReplaceBehaviour REPLACE_BEHAVIOUR = (i) -> '0';
    private static final RMRSupportConfiguration RMR_S_CONF = new RMRSupportConfiguration(' ', '&', ':', '-');
    private static final DallasDecoratorConfiguration DALLAS_DECORATOR_CONFIGURATION =
            new DallasDecoratorConfigurationBuilder()
                .setColorCRCOk(Color.parseColor("#0B7509"))
                .setColorCRCWrong(Color.parseColor("#FF0000"))
                .build();
    private static final DallasDecoratorTextWatcher DALLAS_DECORATOR_TEXT_WATCHER =
            new DallasDecoratorTextWatcher(DALLAS_DECORATOR_CONFIGURATION);
    private static final CRC8Dallas CRC_8_DALLAS = new CRC8Dallas(true);
    private static final DallasRMR DALLAS_RMR = new DallasRMR(CRC_8_DALLAS);

    //----------------------------------------------------------------------------------------------
    // DallasRMR.IDallasInput implementation
    //----------------------------------------------------------------------------------------------
    private boolean shouldSetDefaultText = true;

    private boolean initialized = false;
    private boolean shouldUpdateCRC = true;

    public boolean onResumeInitialized() {
        return initialized;
    }

    public boolean shouldUpdateCRC() {
        return shouldUpdateCRC;
    }

    public void shouldUpdateCRC(boolean shouldUpdateCRC) {
        this.shouldUpdateCRC = shouldUpdateCRC;
    }

    public boolean crcIsON() {
        return dallasCrcSwitch.isChecked();
    }

    public EditText keyCodeEditText() {
        return dallasKeyCodeET;
    }

    private MaskedTextChangedListener listener = null;

    //----------------------------------------------------------------------------------------------
    // Hex keyboard
    //----------------------------------------------------------------------------------------------
    protected IKeyHexKeyboard iKeyHexKeyboard = null;

    void initHKB() {
        iKeyHexKeyboard = null;
        iKeyHexKeyboard = new IKeyHexKeyboardBuilder()
                .setHostActivity(this)
                .setKeyboardViewId(R.id.ikey_main_hex_kb)
                .setContainerViewId(R.id.ikey_main_hex_kb_container)
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
        DALLAS_RMR.setDallasInput(this);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        dallasKeyCodeET.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        dallasKeyCodeET.setText(DALLAS_INITIAL_VALUE);
        if(listener != null)
            dallasKeyCodeET.removeTextChangedListener(listener);
        initHKB();

        // RED masks
        listener = MaskedTextChangedListener.Companion.installOn(dallasKeyCodeET, E8_PATTERN, DALLAS_RMR);

        iKeyHexKeyboard.registerEditText(dallasKeyCodeET, DALLAS_DECORATOR_TEXT_WATCHER, RMR_S_CONF, REPLACE_BEHAVIOUR);
        iKeyHexKeyboard.registerEditText(simpleText);
        iKeyHexKeyboard.registerEditText(decoratedText, new ColoredPairsDecoratorTextWatcher(
                new ColoredPairsDecoratorConfigurationBuilder()
                        .setColorA(Color.parseColor("#0B7509"))
                .setColorB(Color.parseColor("#333333"))
                .setGroupLegnth(2).build()
        ));
        decoratedText.append("FF");

        if (shouldSetDefaultText) {
            dallasKeyCodeET.setText(DALLAS_INITIAL_VALUE);
            shouldSetDefaultText = false;
        }
        initialized = true;

        aboutAppTV.setText(buildHTMLString());
        aboutAppTV.setMovementMethod(LinkMovementMethod.getInstance());
    }

    @Override public void onResume() {
        super.onResume();
    }

    @Override public void onPause() {
        super.onPause();
    }

    @Override public void onBackPressed() {
        if(!iKeyHexKeyboard.onBackPressed())
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

