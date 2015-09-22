/*
 * Copyright (C) 2010 Ken Ellinwood.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.zzzmode.apkeditor.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * Produces the classic hex dump with an address column, hex data
 * section (16 bytes per row) and right-column printable character dislpay.
 */
public class HexDumpEncoder {

    static final HexEncoder encoder = new HexEncoder();

    public static String encode(byte[] data) {
        ByteArrayOutputStream baos =null;
        try {
            baos= new ByteArrayOutputStream();
            encoder.encode(data, 0, data.length, baos);
            byte[] hex = baos.toByteArray();

            StringBuilder hexDumpOut = new StringBuilder();

            StringBuilder hexOut = new StringBuilder();
            StringBuilder chrOut = new StringBuilder();

            for (int i = 0; i < hex.length; i += 32) {

                int max = Math.min(i + 32, hex.length);

                hexOut.setLength(0);
                chrOut.setLength(0);

                //hexOut.append(String.format("%08x: ", (i / 2)));
                //System.out.println(format08x(i / 2));
                hexOut.append(format08x(i / 2)).append(':').append(' ');
                for (int j = i; j < max; j += 2) {
                    hexOut.append((char) hex[j]);
                    hexOut.append((char) hex[j + 1]);
                    if ((j + 2) % 4 == 0) hexOut.append(' ');

                    int dataChar = data[j / 2];
                    if (dataChar >= 32 && dataChar < 127)
                        chrOut.append((char) dataChar);
                    else
                        chrOut.append('.');

                }

                hexDumpOut.append(hexOut.toString());
                for (int k = hexOut.length(); k < 50; k++)
                    hexDumpOut.append(' ');
                hexDumpOut.append("  ");
                hexDumpOut.append(chrOut);
                hexDumpOut.append('\n');
            }

            return hexDumpOut.toString();
        } catch (IOException x) {
            throw new IllegalStateException(x.getClass().getName() + ": " + x.getMessage());
        }finally {
            try {
                if (baos != null) {
                    baos.close();
                }
            }catch (Exception e){
            }
        }
    }

    /**
     * replace String.format("%08x",int),
     * @param value
     * @return
     */
    private static String format08x(int value){
        char[] buf = new char[32];
        final int shift=4;
        int charPos = 32;
        final int fillLength=8;
        final char fillChar='0';
        final int radix = 1 << shift;
        final int mask = radix - 1;
        do {
            buf[--charPos] = HexEncoder.digits[value & mask];
            value >>>= shift;
        } while (value != 0);

        char[] c = new char[fillLength];
        char[] s = Arrays.copyOfRange(buf, charPos, 32);
        final int len = s.length;
        final int fl = fillLength - len;
        for (int i = 0; i < fl; i++) {
            c[i] = fillChar;
        }
        System.arraycopy(s, 0, c, fl, len);
        return new String(c);
    }
}