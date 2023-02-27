package com.ihs.device.clean.junk.cache.app.sys;

import android.os.Parcel;
import android.os.Parcelable;

import com.ihs.device.common.HSAppInfo;

public class HSAppSysCache extends HSAppInfo implements Parcelable {
    public static final Creator<HSAppSysCache> CREATOR = new Creator<HSAppSysCache>() {
        @Override
        public HSAppSysCache createFromParcel(Parcel source) {
            return new HSAppSysCache(source);
        }

        @Override
        public HSAppSysCache[] newArray(int size) {
            return new HSAppSysCache[size];
        }
    };
    private long externalCacheSize;  //外部缓存数据
    private long internalCacheSize;  //内部缓存数据

    public HSAppSysCache(Parcel source) {
        super(source);
        internalCacheSize = source.readLong();
        externalCacheSize = source.readLong();
    }

    public HSAppSysCache(String packageName) {
        super(packageName);
    }

    public HSAppSysCache(String packageName, long dataSize) {
        super(packageName, dataSize);
    }

    public HSAppSysCache(String packageName, long dataSize, String appName) {
        super(packageName, dataSize, appName);
    }

    /**
     * @return 返回InternalCache大小
     */
    public long getInternalCacheSize() {
        return internalCacheSize;
    }

    public void setInternalCacheSize(long internalCacheSize) {
        this.internalCacheSize = internalCacheSize;
    }

    /**
     * @return 返回ExternalCache大小
     */
    public long getExternalCacheSize() {
        return externalCacheSize;
    }

    public void setExternalCacheSize(long externalCacheSize) {
        this.externalCacheSize = externalCacheSize;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeLong(internalCacheSize);
        dest.writeLong(externalCacheSize);
    }
}
