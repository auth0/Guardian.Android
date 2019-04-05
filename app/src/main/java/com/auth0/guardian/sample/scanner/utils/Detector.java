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

public abstract class Detector<T> {

    private final Object lock = new Object();
    private Detector.Processor<T> processor;

    public Detector() {
    }

    public void release() {
        Object var1 = lock;
        synchronized(lock) {
            if(processor != null) {
                processor.release();
                processor = null;
            }

        }
    }

    public abstract SparseArray<T> detect(Frame frame);

    public boolean setFocus(int id) {
        return true;
    }

    public void receiveFrame(Frame frame) {
        Object var2 = lock;
        synchronized(lock) {
            if(processor == null) {
                throw new IllegalStateException("Detector processor must first be set with setProcessor in order to receive detection results.");
            } else {
                Frame.Metadata metadata = new Frame.Metadata(frame.getMetadata());
                metadata.autoRotate();
                SparseArray detectedItems = detect(frame);
                boolean isOperational = isOperational();
                Detector.Detections detections = new Detector.Detections(detectedItems, metadata, isOperational);
                processor.receiveDetections(detections);
            }
        }
    }

    public void setProcessor(Detector.Processor<T> processor) {
        this.processor = processor;
    }

    public boolean isOperational() {
        return true;
    }

    public interface Processor<T> {

        void release();

        void receiveDetections(Detector.Detections<T> detections);
    }

    public static class Detections<T> {

        private final SparseArray<T> detectedItems;
        private final Frame.Metadata frameMetadata;
        private final boolean isOperational;

        public Detections(SparseArray<T> detectedItems, Frame.Metadata frameMetadata, boolean isOperational) {
            this.detectedItems = detectedItems;
            this.frameMetadata = frameMetadata;
            this.isOperational = isOperational;
        }

        public SparseArray<T> getDetectedItems() {
            return this.detectedItems;
        }

        public Frame.Metadata getFrameMetadata() {
            return this.frameMetadata;
        }

        public boolean detectorIsOperational() {
            return this.isOperational;
        }
    }
}
