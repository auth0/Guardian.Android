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

package com.auth0.guardian.sample;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import com.auth0.android.guardian.sdk.networking.Callback;

import java.lang.ref.WeakReference;

/**
 * Shows a progress dialog while a Guardian request runs and an error dialog if it fails.
 * <p>
 * Guardian calls back on a background thread, possibly after the Activity was rotated or
 * closed. The dialogs are therefore only touched while the Activity is alive: the progress
 * dialog is dismissed when the Activity is destroyed (no leaked window) and the error dialog is
 * skipped for a dead Activity. The wrapped callback is always invoked.
 */
public class DialogCallback<T> implements Callback<T> {

    private static final String TAG = DialogCallback.class.getName();

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final WeakReference<Context> contextRef;
    private final Callback<T> callback;
    private final AlertDialog progressDialog;

    DialogCallback(@NonNull Context context,
                   @StringRes int titleResId,
                   @StringRes int messageResId,
                   Callback<T> callback) {
        this.contextRef = new WeakReference<>(context);
        this.callback = callback;

        ProgressBar progressBar = new ProgressBar(context);
        progressBar.setIndeterminate(true);
        progressBar.setId(R.id.progress_bar); // need an id to align "right of" this

        RelativeLayout layout = new RelativeLayout(context);
        RelativeLayout.LayoutParams progressBarLayoutParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);

        int margin = context.getResources().getDimensionPixelSize(R.dimen.activity_vertical_margin);
        progressBarLayoutParams.setMargins(margin, margin, margin, margin);
        layout.addView(progressBar, progressBarLayoutParams);

        TextView textView = new TextView(context);
        textView.setText(messageResId);
        RelativeLayout.LayoutParams textViewLayoutParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        textViewLayoutParams.addRule(RelativeLayout.RIGHT_OF, progressBar.getId());
        textViewLayoutParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
        layout.addView(textView, textViewLayoutParams);

        this.progressDialog = new AlertDialog.Builder(context)
                .setTitle(titleResId)
                .setView(layout)
                .setCancelable(false)
                .create();
        this.progressDialog.show();

        if (context instanceof LifecycleOwner) {
            // Dismiss before the window is torn down, otherwise it is reported as leaked and a
            // later dismiss() can throw.
            ((LifecycleOwner) context).getLifecycle().addObserver(new DefaultLifecycleObserver() {
                @Override
                public void onDestroy(@NonNull LifecycleOwner owner) {
                    owner.getLifecycle().removeObserver(this);
                    dismissProgressNow();
                }
            });
        }
    }

    @Override
    public void onSuccess(T response) {
        dismissProgress();
        callback.onSuccess(response);
    }

    @Override
    public void onFailure(final Throwable exception) {
        Log.e(TAG, "Guardian error", exception);
        dismissProgress();
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                Context context = contextRef.get();
                if (!isAlive(context)) {
                    callback.onFailure(exception);
                    return;
                }
                new AlertDialog.Builder(context)
                        .setTitle(R.string.alert_title_error)
                        .setMessage(exception.toString())
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialog) {
                                callback.onFailure(exception);
                            }
                        })
                        .create()
                        .show();
            }
        });
    }

    private static boolean isAlive(Context context) {
        if (context == null) {
            return false;
        }
        if (context instanceof Activity) {
            Activity activity = (Activity) context;
            return !activity.isFinishing() && !activity.isDestroyed();
        }
        return true;
    }

    private void dismissProgress() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            dismissProgressNow();
        } else {
            mainHandler.post(this::dismissProgressNow);
        }
    }

    private void dismissProgressNow() {
        if (progressDialog.isShowing()) {
            try {
                progressDialog.dismiss();
            } catch (IllegalArgumentException e) {
                // the window is already gone
                Log.w(TAG, "Progress dialog was already detached", e);
            }
        }
    }
}
