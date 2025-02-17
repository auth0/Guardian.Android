package com.auth0.guardian.sample.parcel.utils;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class ParcelableEntry implements Parcelable {
    private final String key;
    private final Object value;

    public ParcelableEntry(String key, Object value) {
        this.key = key;
        this.value = value;
    }

    protected ParcelableEntry(Parcel in) {
        key = in.readString();
        value = in.readValue(getClass().getClassLoader());
    }

    public static final Creator<ParcelableEntry> CREATOR = new Creator<ParcelableEntry>() {
        @Override
        public ParcelableEntry createFromParcel(Parcel in) {
            return new ParcelableEntry(in);
        }

        @Override
        public ParcelableEntry[] newArray(int size) {
            return new ParcelableEntry[size];
        }
    };

    public String getKey() {
        return key;
    }

    public Object getValue() {
        return value;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int i) {
        dest.writeString(key);
        dest.writeValue(value);
    }
}
