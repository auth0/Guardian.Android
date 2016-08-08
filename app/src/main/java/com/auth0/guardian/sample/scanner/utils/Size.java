/*
 * Copyright (c) 2016 Auth0 (http://auth0.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.auth0.guardian.sample.scanner.utils;

/**
 * android.utils.Size is only available from API 21 and above, so use our own copy
 */
public final class Size {

    private final int width;
    private final int height;

    public Size(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        } else if (this == obj) {
            return true;
        } else if (!(obj instanceof Size)) {
            return false;
        } else {
            Size size = (Size) obj;
            return this.width == size.width && this.height == size.height;
        }
    }

    public String toString() {
        return this.width + "x" + this.height;
    }

    private static NumberFormatException newNumberFormatException(String var0) {
        throw new NumberFormatException("Invalid Size: \"" + var0 + "\"");
    }

    public static Size parseSize(String string) throws NumberFormatException {
        if (string == null) {
            throw new IllegalArgumentException("string must not be null");
        } else {
            int var1 = string.indexOf(42);
            if (var1 < 0) {
                var1 = string.indexOf(120);
            }

            if (var1 < 0) {
                throw newNumberFormatException(string);
            } else {
                try {
                    return new Size(Integer.parseInt(string.substring(0, var1)), Integer.parseInt(string.substring(var1 + 1)));
                } catch (NumberFormatException var3) {
                    throw newNumberFormatException(string);
                }
            }
        }
    }

    public int hashCode() {
        return this.height ^ (this.width << 16 | this.width >>> 16);
    }
}