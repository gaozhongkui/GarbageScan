package com.ihs.device.clean.junk.cache.app.nonsys.junk;

import android.os.Parcel;
import android.os.Parcelable;

import com.ihs.device.common.HSAppInfo;

public class HSAppJunkCache extends HSAppInfo implements Parcelable {
    public static final Creator<HSAppJunkCache> CREATOR = new Creator<HSAppJunkCache>() {
        @Override
        public HSAppJunkCache createFromParcel(Parcel source) {
            return new HSAppJunkCache(source);
        }

        @Override
        public HSAppJunkCache[] newArray(int size) {
            return new HSAppJunkCache[size];
        }
    };
    private String path;
    private String pathType;
    private boolean isInstalled;

    public HSAppJunkCache(Parcel source) {
        super(source);
        path = source.readString();
        pathType = source.readString();
        isInstalled = source.readByte() != 0;
    }

    public HSAppJunkCache(String packageName, String appName, long dataSize, String path, String pathType) {
        super(packageName, dataSize, appName);
        this.path = path;
        this.pathType = pathType;
    }

    /**
     * @return 返回垃圾路径所属类型
     */
    public String getPathType() {
        return pathType;
    }

    public void setPathType(String pathType) {
        this.pathType = pathType;
    }

    /**
     * @return 返回垃圾路径
     */
    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    /**
     * @return 产生该垃圾的app是否已安装
     */
    public boolean isInstalled() {
        return isInstalled;
    }

    public void setInstalled(boolean isInstalled) {
        this.isInstalled = isInstalled;
    }

    /**
     * @return 该垃圾是否为卸载残留
     */
    public boolean isResidual() {
        return !isInstalled;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(path);
        dest.writeString(pathType);
        dest.writeByte((byte) (isInstalled ? 1 : 0));
    }
}
