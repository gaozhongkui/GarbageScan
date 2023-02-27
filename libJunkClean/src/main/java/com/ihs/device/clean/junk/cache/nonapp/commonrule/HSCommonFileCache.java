package com.ihs.device.clean.junk.cache.nonapp.commonrule;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.CallSuper;
import android.text.TextUtils;

import java.io.File;

public class HSCommonFileCache implements Parcelable {
    public static final Creator<HSCommonFileCache> CREATOR = new Creator<HSCommonFileCache>() {
        @Override
        public HSCommonFileCache createFromParcel(Parcel source) {
            return new HSCommonFileCache(source);
        }

        @Override
        public HSCommonFileCache[] newArray(int size) {
            return new HSCommonFileCache[size];
        }
    };
    private long fileSize;
    private long modifyTime;
    private String fileName;
    private String fileExtension;
    private String filePath;
    private HSApkInfo apkInfo;

    public HSCommonFileCache(Parcel source) {
        filePath = source.readString();
        fileName = source.readString();
        fileExtension = source.readString();
        fileSize = source.readLong();
        modifyTime = source.readLong();
        if (isExtension("apk")) {
            apkInfo = source.readParcelable(HSApkInfo.class.getClassLoader());
        }
    }

    public HSCommonFileCache(File file) {
        filePath = file.getPath();
        fileName = file.getName();
        fileExtension = fileName.substring(fileName.lastIndexOf(".") + 1, fileName.length()).toLowerCase();
        fileSize = file.length();
        modifyTime = file.lastModified();
    }

    /**
     * @return 返回HSApkInfo类型垃圾
     */
    public HSApkInfo getApkInfo() {
        return apkInfo;
    }

    /**
     * @return 返回垃圾大小
     */
    public long getSize() {
        return fileSize;
    }

    /**
     * @return 返回垃圾文件名
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * @return 返回垃圾路径
     */
    public String getFilePath() {
        return filePath;
    }

    /**
     * @return 返回文件后缀
     */
    public String getFileExtension() {
        return fileExtension;
    }

    /**
     * @return 返回文件上一次修改时间
     */
    public long getLastModifyTime() {
        return modifyTime;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    @CallSuper
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(filePath);
        dest.writeString(fileName);
        dest.writeString(fileExtension);
        dest.writeLong(fileSize);
        dest.writeLong(modifyTime);
        if (isExtension("apk")) {
            dest.writeParcelable(apkInfo, flags);
        }
    }

    /**
     * @return 初始化HSCommonFileCache可能带有的具体信息，
     * 例如该文件为apk，则初始化出一个HSApkInfo对象进行更详细的描述
     */
    public HSCommonFileCache initDetailInfo() {
        if (isExtension("apk")) {
            apkInfo = new HSApkInfo(new File(filePath));
        }
        return this;
    }

    /**
     * @param ext
     *         文件后缀
     *
     * @return 文件后缀是否和传入后缀一致
     */
    public boolean isExtension(String ext) {
        return TextUtils.equals(ext.toLowerCase(), fileExtension.toLowerCase());
    }
}
