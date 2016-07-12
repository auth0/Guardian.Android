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

import java.util.HashSet;
import java.util.Iterator;

public class MultiProcessor<T> implements Detector.Processor<T> {

    private MultiProcessor.Factory<T> factory;
    private SparseArray<TrackerCounter> trackers;
    private int maxGapFrames;

    public void release() {
        for (int i = 0; i < trackers.size(); ++i) {
            Tracker tracker = trackers.valueAt(i).tracker;
            tracker.onDone();
        }

        this.trackers.clear();
    }

    public void receiveDetections(Detector.Detections<T> detections) {
        this.createNewTrackers(detections);
        this.recycleOldTrackers(detections);
        this.updateTrackers(detections);
    }

    private MultiProcessor() {
        this.trackers = new SparseArray();
        this.maxGapFrames = 3;
    }

    private void createNewTrackers(Detector.Detections<T> detections) {
        SparseArray detectedItems = detections.getDetectedItems();

        for (int i = 0; i < detectedItems.size(); ++i) {
            int id = detectedItems.keyAt(i);
            Object item = detectedItems.valueAt(i);
            if (trackers.get(id) == null) {
                TrackerCounter trackerCounter = new TrackerCounter();
                trackerCounter.tracker = factory.create((T) item);
                trackerCounter.tracker.onNewItem(id, (T) item);
                trackers.append(id, trackerCounter);
            }
        }

    }

    private void recycleOldTrackers(Detector.Detections<T> detections) {
        SparseArray detectedItems = detections.getDetectedItems();
        HashSet set = new HashSet();

        for (int i = 0; i < trackers.size(); ++i) {
            int id = trackers.keyAt(i);
            if (detectedItems.get(id) == null) {
                TrackerCounter trackerCounter = trackers.valueAt(i);

                //TODO averiguar que hacia esto
                //TrackerCounter.zzb2(trackerCounter);

                if (trackerCounter.counter >= maxGapFrames) {
                    trackerCounter.tracker.onDone();
                    set.add(Integer.valueOf(id));
                } else {
                    trackerCounter.tracker.onMissing(detections);
                }
            }
        }

        Iterator iterator = set.iterator();
        while (iterator.hasNext()) {
            Integer id = (Integer) iterator.next();
            trackers.delete(id.intValue());
        }

    }

    private void updateTrackers(Detector.Detections<T> detections) {
        SparseArray detectedItems = detections.getDetectedItems();

        for (int i = 0; i < detectedItems.size(); ++i) {
            int id = detectedItems.keyAt(i);
            Object item = detectedItems.valueAt(i);
            TrackerCounter trackerCounter = trackers.get(id);
            trackerCounter.counter = 0;
            trackerCounter.tracker.onUpdate(detections, (T) item);
        }

    }

    private class TrackerCounter {

        private Tracker<T> tracker;
        private int counter;

        private TrackerCounter() {
            counter = 0;
        }
    }

    public static class Builder<T> {

        private MultiProcessor<T> multiProcessor = new MultiProcessor();

        public Builder(MultiProcessor.Factory<T> factory) {
            if (factory == null) {
                throw new IllegalArgumentException("No factory supplied.");
            } else {
                multiProcessor.factory = factory;
            }
        }

        public MultiProcessor.Builder<T> setMaxGapFrames(int maxGapFrames) {
            if (maxGapFrames < 0) {
                throw new IllegalArgumentException("Invalid max gap: " + maxGapFrames);
            } else {
                multiProcessor.maxGapFrames = maxGapFrames;
                return this;
            }
        }

        public MultiProcessor<T> build() {
            return multiProcessor;
        }
    }

    public interface Factory<T> {

        Tracker<T> create(T var1);
    }
}
