package com.ihs.device.clean.junk.cache.nonapp.pathrule;

import android.os.Parcel;
import android.os.Parcelable;

public class HSPathFileCache implements Parcelable {
    public static final Creator<HSPathFileCache> CREATOR = new Creator<HSPathFileCache>() {
        @Override
        public HSPathFileCache createFromParcel(Parcel source) {
            return new HSPathFileCache(source);
        }

        @Override
        public HSPathFileCache[] newArray(int size) {
            return new HSPathFileCache[size];
        }
    };
    private long dataSize = 0;
    private String path;
    private String pathType;

    public HSPathFileCache(Parcel source) {
        dataSize = source.readLong();
        path = source.readString();
        pathType = source.readString();
    }

    public HSPathFileCache(long dataSize, String path, String pathType) {
        this.dataSize = dataSize;
        this.path = path;
        this.pathType = pathType;
    }

    /**
     * @return 垃圾大小
     */
    public long getSize() {
        return dataSize;
    }

    public void setSize(long mDataSize) {
        this.dataSize = mDataSize;
    }

    /**
     * @return 垃圾路径
     */
    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    /**
     * @return 垃圾类型
     */
    public String getPathType() {
        return pathType;
    }

    public void setPathType(String pathType) {
        this.pathType = pathType;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(dataSize);
        dest.writeString(path);
        dest.writeString(pathType);
    }
}
