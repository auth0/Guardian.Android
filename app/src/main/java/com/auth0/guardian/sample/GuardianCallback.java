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

import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.auth0.android.guardian.sdk.networking.Callback;

public class GuardianCallback<T> implements Callback<T> {

    private static final String TAG = GuardianCallback.class.getName();

    private final Context context;
    private final Callback<T> callback;
    private final AlertDialog progressDialog;

    GuardianCallback(@NonNull Context context,
                     @StringRes int titleResId,
                     @StringRes int messageResId,
                     Callback<T> callback) {
        this.context = context;
        this.callback = callback;

        ProgressBar progressBar = new ProgressBar(this.context);
        progressBar.setIndeterminate(true);
        progressBar.setId(7); // need an id to align "right of" this

        RelativeLayout layout = new RelativeLayout(this.context);
        RelativeLayout.LayoutParams progressBarLayoutParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);

        int margin = context.getResources().getDimensionPixelSize(R.dimen.activity_vertical_margin);
        progressBarLayoutParams.setMargins(margin, margin, margin, margin);
        layout.addView(progressBar, progressBarLayoutParams);

        TextView textView = new TextView(this.context);
        textView.setText(messageResId);
        RelativeLayout.LayoutParams textViewLayoutParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        textViewLayoutParams.addRule(RelativeLayout.RIGHT_OF, progressBar.getId());
        textViewLayoutParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
        layout.addView(textView, textViewLayoutParams);

        this.progressDialog = new AlertDialog.Builder(this.context)
                .setTitle(titleResId)
                .setView(layout)
                .setCancelable(false)
                .create();
        this.progressDialog.show();
    }

    @Override
    public void onSuccess(T response) {
        progressDialog.dismiss();
        callback.onSuccess(response);
    }

    @Override
    public void onFailure(final Throwable exception) {
        Log.e(TAG, "Guardian error", exception);
        progressDialog.dismiss();
        new Handler(context.getMainLooper())
                .post(new Runnable() {
                    @Override
                    public void run() {
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
}
