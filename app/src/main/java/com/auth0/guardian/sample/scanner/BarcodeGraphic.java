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

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.animation.DecelerateInterpolator;

import com.auth0.guardian.sample.scanner.camera.GraphicOverlay;
import com.auth0.guardian.sample.scanner.utils.Barcode;

import java.util.ArrayList;

class BarcodeGraphic extends GraphicOverlay.Graphic {

    private static final String TAG = BarcodeGraphic.class.getName();

    private static final int DEFAULT_RIPPLE_COUNT = 1;
    private static final int DEFAULT_DURATION_TIME = 1000;

    private int id;
    private volatile Barcode barcode;

    private final Paint paint;
    private final AnimatorSet animatorSet;
    private ArrayList<Animator> animatorList;
    private final ArrayList<RippleView> rippleViewList = new ArrayList<>();
    private boolean shouldStartAnimation = true;

    BarcodeGraphic(GraphicOverlay overlay) {
        super(overlay);

        int rippleDelay = DEFAULT_DURATION_TIME / DEFAULT_RIPPLE_COUNT;

        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL);

        animatorSet = new AnimatorSet();
        animatorSet.setInterpolator(new DecelerateInterpolator());
        animatorList = new ArrayList<>();

        for (int i = 0; i < DEFAULT_RIPPLE_COUNT; i++) {
            RippleView rippleView = new RippleView();
            rippleViewList.add(rippleView);
            final ObjectAnimator radiusAnimator = ObjectAnimator.ofFloat(rippleView, "RadiusMultiplier", 0.0f, 5.f);
            radiusAnimator.setStartDelay(i * rippleDelay);
            radiusAnimator.setDuration(DEFAULT_DURATION_TIME);
            animatorList.add(radiusAnimator);
            final ObjectAnimator alphaAnimator = ObjectAnimator.ofFloat(rippleView, "Alpha", 0.4f, 0f);
            alphaAnimator.setStartDelay(i * rippleDelay);
            alphaAnimator.setDuration(DEFAULT_DURATION_TIME);
            animatorList.add(alphaAnimator);
        }

        animatorSet.playTogether(animatorList);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    /**
     * Updates the barcode instance from the detection of the most recent frame.  Invalidates the
     * relevant portions of the overlay to trigger a redraw.
     */
    void updateItem(Barcode barcode) {
        this.barcode = barcode;
        postInvalidate();
    }

    /**
     * Draws the barcode annotations for position, size, and raw value on the supplied canvas.
     */
    @Override
    public void draw(Canvas canvas) {
        Barcode barcode = this.barcode;
        if (barcode == null) {
            return;
        }

        if (shouldStartAnimation) {
            shouldStartAnimation = false;

            // Get "on screen" bounding box around the barcode
            RectF rect = barcode.getBoundingBox();

            rect.left = translateX(rect.left);
            rect.top = translateY(rect.top);
            rect.right = translateX(rect.right);
            rect.bottom = translateY(rect.bottom);

            // Set location and size
            for (RippleView view : rippleViewList) {
                view.setLocation(rect.centerX(), rect.centerY(), Math.min(rect.width(), rect.height()) / 2.f);
            }

            // Start animations
            animatorSet.start();
        }

        for (RippleView view : rippleViewList) {
            view.draw(canvas);
        }

        postInvalidate();
    }

    private class RippleView {

        private float radiusMultiplier;
        private float alpha;
        private float centerX;
        private float centerY;
        private float radius;

        public void setLocation(float centerX, float centerY, float radius) {
            this.centerX = centerX;
            this.centerY = centerY;
            this.radius = radius;
        }

        @SuppressWarnings("unused")
        public void setRadiusMultiplier(float radiusMultiplier) {
            this.radiusMultiplier = radiusMultiplier;
        }

        @SuppressWarnings("unused")
        public void setAlpha(float alpha) {
            this.alpha = alpha;
        }

        protected void draw(Canvas canvas) {
            paint.setColor(Color.argb((int) (255 * alpha), 255, 255, 255));
            canvas.drawCircle(centerX, centerY, radiusMultiplier * radius, paint);
        }
    }
}
