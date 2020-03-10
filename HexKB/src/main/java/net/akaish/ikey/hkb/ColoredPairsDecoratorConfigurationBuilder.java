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

public class ColoredPairsDecoratorConfigurationBuilder {
    private int colorA;
    private int colorB;
    private int groupLegnth;

    public ColoredPairsDecoratorConfigurationBuilder setColorA(int colorA) {
        this.colorA = colorA;
        return this;
    }

    public ColoredPairsDecoratorConfigurationBuilder setColorB(int colorB) {
        this.colorB = colorB;
        return this;
    }

    public ColoredPairsDecoratorConfigurationBuilder setGroupLegnth(int groupLegnth) {
        this.groupLegnth = groupLegnth;
        return this;
    }

    public ColoredPairesDecoratorConfiguration build() {
        return new ColoredPairesDecoratorConfiguration(colorA, colorB, groupLegnth);
    }

    public static class ColoredPairesDecoratorConfiguration {
        final int colorA;
        final int colorB;
        final int groupLegnth;

        ColoredPairesDecoratorConfiguration(int colorA, int colorB, int groupLegnth) {
            this.colorA = colorA;
            this.colorB = colorB;
            this.groupLegnth = groupLegnth;
        }
    }
}