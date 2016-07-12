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

import android.util.SparseArray;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

public final class BarcodeDetector extends Detector<Barcode> {

    private static final String TAG = BarcodeDetector.class.getName();

    private final MultiFormatReader multiFormatReader;
    private final float offsetPercentage;

    private BarcodeDetector() {
        throw new IllegalStateException("Default constructor called");
    }

    private BarcodeDetector(MultiFormatReader multiFormatReader, float percentage) {
        this.multiFormatReader = multiFormatReader;
        this.offsetPercentage = percentage / 2.f;
    }

    @Override
    public SparseArray<Barcode> detect(Frame frame) {
        if(frame == null) {
            throw new IllegalArgumentException("No frame supplied.");
        } else {
            Frame.Metadata frameMetadata = frame.getMetadata();
            ByteBuffer imageData = frame.getGrayscaleImageData();

            byte[] data = imageData.array();
            int width = frameMetadata.getWidth();
            int height = frameMetadata.getHeight();

            int centerX = width / 2;
            int centerY = height / 2;

            int min = Math.min(width, height);
            int halfSize = (int) (offsetPercentage * min);
            int offsetX = centerX - halfSize;
            int offsetY = centerY - halfSize;

            // Go ahead and assume it's YUV rather than die.
            PlanarYUVLuminanceSource source = new PlanarYUVLuminanceSource(data, width, height,
                    offsetX, offsetY, 2*halfSize, 2*halfSize, false);

            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            Result rawResult = null;
            try {
                rawResult = multiFormatReader.decodeWithState(bitmap);
            } catch (ReaderException re) {
                // continue
            } finally {
                multiFormatReader.reset();
            }

            if (rawResult != null) {
                SparseArray<Barcode> results = new SparseArray<>(1);
                Barcode barcode = new Barcode(rawResult, frame.getMetadata(), offsetX, offsetY);
                results.append(rawResult.hashCode(), barcode);
                return results;
            }
            return new SparseArray<>(0);
        }
    }

    public static class Builder {

        private int percentage = 100;
        private Collection<BarcodeFormat> decodeFormats;

        public Builder() {

        }

        public BarcodeDetector.Builder setPercentage(int percentage) {
            this.percentage = percentage;
            return this;
        }

        public BarcodeDetector.Builder setBarcodeFormats(Collection<BarcodeFormat> decodeFormats) {
            this.decodeFormats = decodeFormats;
            return this;
        }

        public BarcodeDetector build() {
            Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
            hints.put(DecodeHintType.POSSIBLE_FORMATS, decodeFormats);

            MultiFormatReader multiFormatReader = new MultiFormatReader();
            multiFormatReader.setHints(hints);

            return new BarcodeDetector(multiFormatReader, 0.01f * percentage);
        }
    }
}
