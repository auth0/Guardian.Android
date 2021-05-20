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

package com.auth0.guardian.sample.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.CountDownTimer;
import android.util.AttributeSet;

import com.auth0.android.guardian.sdk.Enrollment;
import com.auth0.android.guardian.sdk.otp.TOTP;
import com.auth0.android.guardian.sdk.otp.utils.Base32;
import com.auth0.guardian.sample.R;

public class TOTPCodeView extends androidx.appcompat.widget.AppCompatTextView {

    private Enrollment enrollment;
    private CountDownTimer timer;
    private TOTP codeGenerator;

    private int defaultTextColor = Color.BLACK;
    private int aboutToExpireColor = Color.RED;
    private int expiringTime = 5000;

    public TOTPCodeView(Context context) {
        super(context);
        init(null, 0);
    }

    public TOTPCodeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public TOTPCodeView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {
        defaultTextColor = getCurrentTextColor();

        // Load attributes
        final TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.TOTPCodeView, defStyle, 0);

        aboutToExpireColor = a.getColor(
                R.styleable.TOTPCodeView_aboutToExpireColor, aboutToExpireColor);

        expiringTime = a.getInteger(
                R.styleable.TOTPCodeView_aboutToExpireTimeMs, expiringTime);

        a.recycle();
    }

    public int getAboutToExpireColor() {
        return aboutToExpireColor;
    }

    public void setAboutToExpireColor(int aboutToExpireColor) {
        this.aboutToExpireColor = aboutToExpireColor;
        postInvalidate();
    }

    public void setEnrollment(Enrollment enrollment) {
        try {
            this.codeGenerator = new TOTP(
                    enrollment.getAlgorithm(),
                    Base32.decode(enrollment.getSecret()),
                    enrollment.getDigits(),
                    enrollment.getPeriod());
        } catch (Base32.DecodingException e) {
            throw new IllegalArgumentException("Unable to generate OTP: could not decode secret", e);
        }

        this.enrollment = enrollment;

        startUpdateTimer();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (enrollment != null) {
            startUpdateTimer();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    private void startUpdateTimer() {
        final long periodMs = enrollment.getPeriod() * 1000;
        long currentTime = System.currentTimeMillis();
        // always start with a little offset (0.1 sec) to  be sure that the next code is the new one
        long nextChange = periodMs - currentTime % periodMs + 100;

        if (nextChange > expiringTime) {
            setTextColor(defaultTextColor);
        } else {
            setTextColor(aboutToExpireColor);
        }

        updateCode();

        if (timer != null) {
            timer.cancel();
            timer = null;
        }

        timer = new CountDownTimer(nextChange, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (System.currentTimeMillis() % periodMs >= periodMs - expiringTime) {
                    setTextColor(aboutToExpireColor);
                }
            }

            @Override
            public void onFinish() {
                timer = null;
                startUpdateTimer();
            }
        };
        timer.start();
    }

    private void updateCode() {
        setTextColor(defaultTextColor);

        String codeStr = codeGenerator.generate();

        setText(codeStr);
    }
}
