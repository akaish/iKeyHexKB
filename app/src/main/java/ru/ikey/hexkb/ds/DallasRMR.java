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
package ru.ikey.hexkb.ds;

import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.redmadrobot.inputmask.MaskedTextChangedListener;

import org.apache.commons.codec.binary.Hex;

public class DallasRMR implements MaskedTextChangedListener.ValueListener {

    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();

    private static String toPrettyHexString(final byte[] bytes, @Nullable Character delimiter) {
        if (bytes == null)
            return "";
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < hexChars.length; i += 2) {
            sb.append(hexChars[i]);
            sb.append(hexChars[i + 1]);
            if (i + 2 != hexChars.length && delimiter != null)
                sb.append(delimiter);
        }

        return sb.toString();
    }

    private CRC8Dallas crc8;

    private IDallasInput dktf;

    private final static int KEY_LENGTH_IN_BYTES = 8;
    private final static String KEY_CODE_FILL = "0000000000000000";

    public DallasRMR(CRC8Dallas crc8) {
        this.crc8 = crc8;
    }

    public void setDallasInput(IDallasInput dktf) {
        this.dktf = dktf;
    }

    @Override
    public void onTextChanged(boolean b, @NonNull String s, @NonNull String s1) {
        if(dktf == null)
            return;
        if (!dktf.onResumeInitialized())
            return;
        if (!dktf.crcIsON()) {
            if (s.length() < KEY_LENGTH_IN_BYTES * 2)
                dktf.keyCodeEditText().append(KEY_CODE_FILL);
            return;
        }
        String fixedValue = s;
        if (fixedValue.length() < KEY_LENGTH_IN_BYTES * 2) {
            if (!dktf.shouldUpdateCRC()) {
                dktf.keyCodeEditText().append(KEY_CODE_FILL);
            } else {
                StringBuilder sb = new StringBuilder().append(fixedValue);
                while (sb.length() <= KEY_LENGTH_IN_BYTES * 2)
                    sb.append('0');
                fixedValue = sb.toString();
            }
        }
        if (dktf.crcIsON() && dktf.shouldUpdateCRC()) {
            String data = fixedValue;
            data = data.replace(" ", "").substring(2, 16);
            crc8.reset();
            try {
                crc8.update(Hex.decodeHex(data.toCharArray()));
            } catch (Throwable tr) {
                tr.printStackTrace();
            }
            fixedValue = toPrettyHexString(new byte[]{(byte) crc8.getValue()}, ' ') + data;
            dktf.shouldUpdateCRC(false);
            dktf.keyCodeEditText().setText(fixedValue);
        } else {
            dktf.shouldUpdateCRC(true);
        }
    }

    public interface IDallasInput {
        boolean onResumeInitialized();
        boolean shouldUpdateCRC();
        void shouldUpdateCRC(boolean shouldUpdateCRC);
        boolean crcIsON();
        EditText keyCodeEditText();
    }


}