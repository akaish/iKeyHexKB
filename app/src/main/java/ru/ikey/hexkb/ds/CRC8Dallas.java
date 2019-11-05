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

public class CRC8Dallas extends CRC8 {

    private static final int DALLAS_MAXIM_POLYNOMIAL = 0x8c;
    private static final short INITIAL_VALUE = 0x0;
    private final boolean reverseOrder;

    /**
     * Construct a CRC8 specifying the polynomial and initial value.
     */
    public CRC8Dallas(boolean reverse) {
        super(DALLAS_MAXIM_POLYNOMIAL, INITIAL_VALUE);
        reverseOrder = reverse;
    }


    /**
     * Updates the current checksum with the specified array of bytes.
     * Equivalent to calling <code>update(buffer, 0, buffer.length)</code>.
     * @param buffer the byte array to update the checksum with
     */
    public void update(byte[] buffer) {
        if(!reverseOrder) {
            for(byte b : buffer)
                update(b);
        } else {
            for(int i = buffer.length - 1; i > -1; i--)
                update(buffer[i]);
        }
    }
}
