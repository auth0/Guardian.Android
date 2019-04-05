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

package com.auth0.guardian.sample.scanner;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.LinearLayout;

import com.auth0.guardian.sample.scanner.camera.CameraSource;
import com.auth0.guardian.sample.scanner.camera.CameraSourceCropPreview;
import com.auth0.guardian.sample.scanner.camera.GraphicOverlay;
import com.auth0.guardian.sample.scanner.utils.Barcode;
import com.auth0.guardian.sample.scanner.utils.BarcodeDetector;
import com.auth0.guardian.sample.scanner.utils.MultiProcessor;
import com.google.zxing.BarcodeFormat;

import java.io.IOException;
import java.util.Collection;
import java.util.EnumSet;

public class CaptureView extends LinearLayout
        implements BarcodeTrackerListener {

    private static final String TAG = CaptureView.class.getName();

    private CameraSourceCropPreview preview;
    private GraphicOverlay<BarcodeGraphic> graphicOverlay;

    private CameraSource cameraSource;

    private Listener listener;

    private Handler mainThreadHandler;

    public CaptureView(Context context) {
        super(context);
        init(context, null, 0);
    }

    public CaptureView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0);
    }

    public CaptureView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    private void init(Context context, AttributeSet attrs, int defStyle) {
        mainThreadHandler = new Handler(Looper.getMainLooper());

        preview = new CameraSourceCropPreview(context, attrs);
        graphicOverlay = new GraphicOverlay<>(context, attrs);

        LayoutParams matchParentLayoutParams = new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT);

        preview.addView(graphicOverlay, matchParentLayoutParams);

        addView(preview, matchParentLayoutParams);
    }

    /**
     * Creates and starts the camera.  Note that this uses a higher resolution in comparison
     * to other detection examples to enable the barcode detector to detect small barcodes
     * at long distances.
     *
     * Suppressing InlinedApi since there is a check that the minimum version is met before using
     * the constant.
     */
    @SuppressLint("InlinedApi")
    private void createCameraSource() {
        Context context = getContext().getApplicationContext();

        // A barcode detector is created to track barcodes.  An associated multi-processor instance
        // is set to receive the barcode detection results, track the barcodes, and maintain
        // graphics for each barcode on screen.  The factory is used by the multi-processor to
        // create a separate tracker instance for each barcode.

        Collection<BarcodeFormat> decodeFormats = EnumSet.noneOf(BarcodeFormat.class);
        decodeFormats.add(BarcodeFormat.QR_CODE);

        BarcodeDetector barcodeDetector = new BarcodeDetector.Builder()
                .setBarcodeFormats(decodeFormats)
                .setPercentage(70)
                .build();
        BarcodeTrackerFactory barcodeFactory = new BarcodeTrackerFactory(graphicOverlay, this);
        barcodeDetector.setProcessor(
                new MultiProcessor.Builder<>(barcodeFactory).build());

        // Creates and starts the camera.  Note that this uses a higher resolution in comparison
        // to other detection examples to enable the barcode detector to detect small barcodes
        // at long distances.
        CameraSource.Builder builder = new CameraSource.Builder(context, barcodeDetector)
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setRequestedPreviewSize(1024, 600)
                .setRequestedFps(15.0f)
                .setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);

        cameraSource = builder
                .setFlashMode(null)
                .build();
    }

    public void start(Listener listener) {
        Log.d(TAG, "start");
        this.listener = listener;
        createCameraSource();
    }

    /**
     * Restarts the camera.
     */
    public void resume() {
        Log.d(TAG, "resume");
        startCameraSource();
    }

    /**
     * Stops the camera.
     */
    public void pause() {
        Log.d(TAG, "pause");
        if (preview != null) {
            preview.stop();
        }
    }

    /**
     * Releases the resources associated with the camera source, the associated detectors, and the
     * rest of the processing pipeline.
     */
    public void stop() {
        Log.d(TAG, "stop");
        if (preview != null) {
            preview.release();
        }
    }

    /**
     * Starts or restarts the camera source, if it exists.  If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    private void startCameraSource() throws SecurityException {
        if (cameraSource != null) {
            try {
                preview.start(cameraSource, graphicOverlay);
            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source.", e);
                cameraSource.release();
                cameraSource = null;
            }
        }
    }

    @Override
    public void onBarcodeDetected(final Barcode item) {
        Log.d(TAG, "detected barcode with data: " + item.getText());
        mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                pause();
                listener.onCodeScanned(item.getText());
            }
        });
    }

    public interface Listener {

        void onCodeScanned(String data);
    }
}
