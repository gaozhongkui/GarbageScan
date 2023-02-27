package com.ihs.device.clean.junk.cache.nonapp.commonrule;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.ihs.app.framework.HSApplication;
import com.ihs.commons.utils.HSLog;

import java.io.File;

public class HSApkInfo implements Parcelable {
    public static final Creator<HSApkInfo> CREATOR = new Creator<HSApkInfo>() {
        @Override
        public HSApkInfo createFromParcel(Parcel source) {
            return new HSApkInfo(source);
        }

        @Override
        public HSApkInfo[] newArray(int size) {
            return new HSApkInfo[size];
        }
    };
    private String apkFilePath;
    // is apk file
    private boolean isValidApk = false;
    private String apkPackageName = "";
    private String apkAppName = "";
    private int apkVersionCode = 0;
    private String apkVersionName = "";
    // isInstalled
    private boolean hasInstalled = false;
    private String installedAppName = "";
    private int installedVersionCode = 0;
    private String installedVersionName = "";
    private long firstInstalledTime = 0;
    private long lastUpdatedTime = 0;

    public HSApkInfo(Parcel source) {
        apkFilePath = source.readString();
        isValidApk = source.readInt() == 1;
        apkPackageName = source.readString();
        apkAppName = source.readString();
        apkVersionCode = source.readInt();
        apkVersionName = source.readString();

        hasInstalled = source.readInt() == 1;
        installedAppName = source.readString();
        installedVersionCode = source.readInt();
        installedVersionName = source.readString();
        firstInstalledTime = source.readLong();
        lastUpdatedTime = source.readLong();
    }

    public HSApkInfo(File file) {
        apkFilePath = file.getPath();
        PackageManager pm = HSApplication.getContext().getPackageManager();
        // from apk
        try {
            PackageInfo packageInfo = pm.getPackageArchiveInfo(apkFilePath, 0);
            isValidApk = packageInfo != null;
            if (packageInfo != null) {
                apkPackageName = packageInfo.packageName.trim();
                apkVersionCode = packageInfo.versionCode;
                apkVersionName = packageInfo.versionName;
                if (Build.VERSION.SDK_INT >= 8) {
                    // those two lines do the magic:
                    packageInfo.applicationInfo.sourceDir = apkFilePath;
                    packageInfo.applicationInfo.publicSourceDir = apkFilePath;
                }

                CharSequence label = pm.getApplicationLabel(packageInfo.applicationInfo);
                installedAppName = apkAppName = label != null ? label.toString().trim() : null;
            }
        } catch (Exception ignored) {
            if (HSLog.isDebugging()) {
                throw ignored;
            }
            ignored.printStackTrace();
        }

        // from app - install info
        try {
            PackageInfo pInfo = pm.getPackageInfo(apkPackageName, 0);
            hasInstalled = pInfo != null;
            if (pInfo != null) {
                installedVersionCode = pInfo.versionCode;
                installedVersionName = pInfo.versionName;
                firstInstalledTime = pInfo.firstInstallTime;
                lastUpdatedTime = pInfo.lastUpdateTime;
                installedAppName = pInfo.applicationInfo.loadLabel(pm).toString();
            }
        } catch (Exception ignored) {
            ignored.printStackTrace();
        }
    }

    /**
     * @return 是否为有效apk
     */
    public boolean isValidApk() {
        return isValidApk;
    }

    /**
     * @return 返回apk垃圾图标
     */
    public Drawable getAppIconFromAPKFile() {
        if (!isValidApk || TextUtils.isEmpty(apkPackageName)) {
            return null;
        }
        Drawable apkIcon = null;
        try {
            PackageManager pm = HSApplication.getContext().getPackageManager();
            PackageInfo pi = pm.getPackageArchiveInfo(apkFilePath, 0);
            pi.applicationInfo.sourceDir = apkFilePath;
            pi.applicationInfo.publicSourceDir = apkFilePath;
            apkIcon = pi.applicationInfo.loadIcon(pm);
        } catch (Exception e) {
            if (HSLog.isDebugging()) {
                throw e;
            }
            e.printStackTrace();
        }
        return apkIcon;
    }

    /**
     * @return 返回当前安装应用名
     */
    public String getInstalledAppName() {
        return TextUtils.isEmpty(installedAppName) ? apkAppName : installedAppName;
    }

    /**
     * @return 返回apk的package name
     */
    public String getApkPackageName() {
        return apkPackageName;
    }

    /**
     * @return 返回apk的app name
     */
    public String getApkAppName() {
        return apkAppName;
    }

    /**
     * @return 返回apk的版本号
     */
    public int getApkVersionCode() {
        return apkVersionCode;
    }

    /**
     * @return 返回apk的版本名
     */
    public String getApkVersionName() {
        return apkVersionName;
    }

    /**
     * @return 返回当前安装应用的版本号
     */
    public int getInstalledVersionCode() {
        return installedVersionCode;
    }

    /**
     * @return 返回当前安装应用的版本名
     */
    public String getInstalledVersionName() {
        return installedVersionName;
    }

    /**
     * @return 返回第一次安装应用的时间
     */
    public long getFirstInstalledTime() {
        return firstInstalledTime;
    }

    /**
     * @return 返回上一次更新应用的时间
     */
    public long getLastUpdatedTime() {
        return lastUpdatedTime;
    }

    /**
     * @return 打印apk信息
     */
    public final String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("\n-----APK Info-----");
        if (!TextUtils.isEmpty(apkFilePath)) {
            builder.append("\n{ Path = ").append(apkFilePath).append(" } ");
        }
        if (isValidApk) {
            if (!TextUtils.isEmpty(apkPackageName)) {
                builder.append("\n{ Pkg = ").append(apkPackageName).append(" } ");
            }
            if (!TextUtils.isEmpty(apkAppName)) {
                builder.append("\n{ apkAppName = ").append(apkAppName).append(" } ");
            }
            if (!TextUtils.isEmpty(apkVersionName)) {
                builder.append("\n{ apkVersionName = ").append(apkVersionName).append(" } ");
            }
            builder.append("\n{ apkVersionCode = ").append(apkVersionCode).append(" } ");
        }
        if (hasInstalled) {
            if (!TextUtils.isEmpty(installedAppName)) {
                builder.append("\n{ installedAppName = ").append(installedAppName).append(" } ");
            }
            if (!TextUtils.isEmpty(installedVersionName)) {
                builder.append("\n{ installedVersionName = ").append(installedVersionName).append(" } ");
            }
            builder.append("\n{ installedVersionCode = ").append(installedVersionCode).append(" } ");
            builder.append("\n{ firstInstalledTime = ").append(firstInstalledTime).append(" } ");
            builder.append("\n{ lastUpdatedTime = ").append(lastUpdatedTime).append(" } ");
        }
        builder.append("\n-------------------------------");
        return builder.toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(apkFilePath);
        dest.writeInt(isValidApk ? 1 : 0);
        dest.writeString(apkPackageName);
        dest.writeString(apkAppName);
        dest.writeInt(apkVersionCode);
        dest.writeString(apkVersionName);

        dest.writeInt(hasInstalled ? 1 : 0);
        dest.writeString(installedAppName);
        dest.writeInt(installedVersionCode);
        dest.writeString(installedVersionName);
        dest.writeLong(firstInstalledTime);
        dest.writeLong(lastUpdatedTime);
    }

    /**
     * @return 返回当前apk是否被安装过
     */
    public boolean hasInstalled() {
        return hasInstalled;
    }
}
