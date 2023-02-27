package com.ihs.device.clean.junk.util;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;

import com.ihs.app.framework.HSApplication;
import com.ihs.commons.utils.HSLog;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Author JackSparrow
 * Create Date 07/08/2017.
 */

public class SUtils { //proguard ScanUtil
    public static final String EXTERNAL_STORAGE_DIRECTORY_ABSOLUTE_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator;

    public static final String WRITE_EXTERNAL_STORAGE = "android.permission.WRITE_EXTERNAL_STORAGE";
    public static final String READ_EXTERNAL_STORAGE = "android.permission.READ_EXTERNAL_STORAGE";

    public static byte[] a(byte[] encryptBytes, byte[] key, byte[] iv) { //proguard decryptBytes
        if (encryptBytes == null || key == null || iv == null) {
            HSLog.e("JDC", "encryptBytes or key or iv can't be null");
            return null;
        }

        try {
            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(iv));

            byte[] decryptBytesNoPadding = cipher.doFinal(encryptBytes);

            int paddingLength = decryptBytesNoPadding[decryptBytesNoPadding.length - 1];

            byte[] decryptBytes = new byte[decryptBytesNoPadding.length - paddingLength];

            System.arraycopy(decryptBytesNoPadding, 0, decryptBytes, 0, decryptBytes.length);

            return decryptBytes;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static byte[] b(Context context, String res) { //proguard readAssetResource
        InputStream stream = null;
        byte[] buffer = null;
        try {
            stream = context.getAssets().open(res);
            buffer = new byte[stream.available()];
            while (stream.read(buffer) != -1);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return buffer;
    }

    public static HashSet<String> c() { //proguard getInstalledPackages
        List<PackageInfo> packageInfos = null;
        try {
            packageInfos = HSApplication.getContext().getPackageManager().getInstalledPackages(PackageManager.GET_META_DATA);
        } catch (Exception e) {
            if (HSLog.isDebugging()) {
                throw e;
            }
            e.printStackTrace();
        }

        HashSet<String> pkgs = new HashSet<>();
        if (packageInfos == null) {
            return pkgs;
        }

        for (PackageInfo info : packageInfos) {
            pkgs.add(info.packageName);
        }
        return pkgs;
    }

    public static byte[] d(File file) {
        byte[] bytes = null;
        try {
            if (file == null) {
                return null;
            }
            FileInputStream in = new FileInputStream(file); //proguard getBytesFromFile
            ByteArrayOutputStream out = new ByteArrayOutputStream(4096);
            byte[] b = new byte[4096];
            int n;
            while ((n = in.read(b)) != -1) {
                out.write(b, 0, n);
            }
            in.close();
            out.close();
            bytes = out.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bytes;
    }

    public static boolean isStoragePermissionGranted(Context context) {
        return ActivityCompat.checkSelfPermission(context, WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }
}
