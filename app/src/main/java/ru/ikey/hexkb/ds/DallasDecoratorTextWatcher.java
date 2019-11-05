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

import android.text.Spanned;
import android.text.SpannedString;

import net.akaish.ikey.hkb.HexDecoratorTextWatcher;

import org.apache.commons.codec.binary.Hex;

public class DallasDecoratorTextWatcher extends HexDecoratorTextWatcher {

    private final String OK_DECORATION_START;
    private final String WRONG_DECORATION_START;
    private final static int DALLAS_LENGTH = 8;
    private final static int DALLAS_CRC8_STRING_LENGTH = 2;

    private final static String DECORATION_END = "</font>";

    private final CRC8 crc8;

    public DallasDecoratorTextWatcher(DallasDecoratorConfiguration config) {
        crc8 = getCRCInstance();
        OK_DECORATION_START = "<font color=\"" +
                String.format("#%06X", 0xFFFFFF & config.crcOkColor) +
                "\">";
        WRONG_DECORATION_START = "<font color=\"" +
                String.format("#%06X", 0xFFFFFF & config.crcWrongColor) +
                "\">";
    }

    public CRC8 getCRCInstance() {
        return new CRC8Dallas(true);
    }

    @Override
    public Spanned decorateText(CharSequence source, int start, int beforeS, int count) {
        String sourceStr = source.toString();
        StringBuilder sb = new StringBuilder();
        if(source.length() == 0) return new SpannedString("");
        if(sourceStr.length() <= DALLAS_CRC8_STRING_LENGTH)
            return fromHtmlString(sb.append(WRONG_DECORATION_START)
                    .append(sourceStr)
                    .append(DECORATION_END)
                    .toString());
        String CRC = sourceStr.substring(0, DALLAS_CRC8_STRING_LENGTH);
        if(sourceStr.replace(" ", "").replace(":", "").length() != DALLAS_LENGTH * 2) {
            sb.append(WRONG_DECORATION_START)
                    .append(CRC)
                    .append(DECORATION_END)
                    .append(sourceStr.substring(DALLAS_CRC8_STRING_LENGTH));
            return fromHtmlString(sb.toString());
        }
        long crcValue = Long.parseLong(CRC, 16);
        crc8.reset();
        try {
            crc8.update(Hex.decodeHex(sourceStr
                    .substring(DALLAS_CRC8_STRING_LENGTH + 1).replace(" ", "").replace(":", "").toCharArray()));
        } catch (Throwable tr) {
            tr.printStackTrace();
        }
        long calculatedCrcValue = crc8.getValue();
        if(calculatedCrcValue == crcValue)
            sb.append(OK_DECORATION_START);
        else
            sb.append(WRONG_DECORATION_START);
        sb.append(CRC).append(DECORATION_END).append(sourceStr.substring(DALLAS_CRC8_STRING_LENGTH));
        return fromHtmlString(sb.toString());
    }
}
