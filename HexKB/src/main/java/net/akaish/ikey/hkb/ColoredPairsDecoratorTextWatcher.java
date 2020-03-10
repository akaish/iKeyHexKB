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

import android.text.Spanned;

import androidx.annotation.NonNull;

public class ColoredPairsDecoratorTextWatcher extends HexDecoratorTextWatcher {

    private final ColoredPairsDecoratorConfigurationBuilder.ColoredPairesDecoratorConfiguration config;

    private final String A_DECORATION_START;
    private final String B_DECORATION_START;

    private final static String DECORATION_END = "</font>";

    public ColoredPairsDecoratorTextWatcher(@NonNull ColoredPairsDecoratorConfigurationBuilder.ColoredPairesDecoratorConfiguration config) {
        this.config = config;
        A_DECORATION_START = "<font color=\"" +
                String.format("#%06X", 0xFFFFFF & config.colorA) +
                "\">";
        B_DECORATION_START = "<font color=\"" +
                String.format("#%06X", 0xFFFFFF & config.colorB) +
                "\">";
    }

    @Override
    public Spanned decorateText(CharSequence source, int start, int beforeS, int count) {
        boolean dcType = true;
        StringBuilder sbSpan = new StringBuilder();

        final int segmentsCount = source.length() / config.groupLegnth +
                (source.length() % config.groupLegnth > 0 ? 1 : 0);

        for(int i = 0; i < segmentsCount; i++) {
            int segmentStart = i*config.groupLegnth;
            int segmentEnd = segmentStart + config.groupLegnth;
            if(segmentsCount == (i + 1)) {
                segmentEnd = source.length();
            }

            if(i+1 % 2 != 0) {
                dcType = !dcType;
            }

            if(dcType) {
                sbSpan.append(A_DECORATION_START);
            } else {
                sbSpan.append(B_DECORATION_START);
            }

            char[] segmentData = source
                    .subSequence(segmentStart, segmentEnd)
                    .toString().toCharArray();

            for(int j = 0; j < segmentData.length; j++) {
                sbSpan.append(segmentData[j]);
            }
            sbSpan.append(DECORATION_END);
        }
        return fromHtml(sbSpan.toString());
    }
}
