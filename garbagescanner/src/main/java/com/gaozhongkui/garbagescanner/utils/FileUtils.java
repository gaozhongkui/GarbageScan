package com.gaozhongkui.garbagescanner.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.StatFs;
import android.text.TextUtils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.regex.Pattern;

public class FileUtils {

    /**
     * Regular expression for safe filenames: no spaces or metacharacters
     */
    private static final Pattern SAFE_FILENAME_PATTERN = Pattern.compile("[\\w%+,./=_-]+");

    public static byte[] readAssetFileToByte(Context context, String fileName) {
        try {
            InputStream is = context.getAssets().open(fileName);
            int length = is.available();
            byte[] buffer = new byte[length];
            is.read(buffer);
            is.close();
            return buffer;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String readAssetFile(Context context, String fileName) {
        try {
            StringBuilder sb = new StringBuilder();
            InputStream is = context.getAssets().open(fileName);
            BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            br.close();
            return sb.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Perform an fsync on the given FileOutputStream.  The stream at this
     * point must be flushed but not yet closed.
     */
    public static boolean sync(FileOutputStream stream) {
        try {
            if (stream != null) {
                stream.getFD().sync();
            }
            return true;
        } catch (IOException ignored) {
        }
        return false;
    }

    // copy a file from srcFile to destFile, return true if succeed, return
    // false if fail
    public static boolean copyFile(File srcFile, File destFile) {
        boolean result = false;
        try {
            try (InputStream in = new FileInputStream(srcFile)) {
                result = copyToFile(in, destFile);
            }
        } catch (IOException e) {
            result = false;
        }
        return result;
    }

    /**
     * Copy data from a source stream to destFile.
     * Return true if succeed, return false if failed.
     */
    public static boolean copyToFile(InputStream inputStream, File destFile) {
        try {
            if (destFile.exists()) {
                destFile.delete();
            }
            FileOutputStream out = new FileOutputStream(destFile);
            try {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) >= 0) {
                    out.write(buffer, 0, bytesRead);
                }
            } finally {
                out.flush();
                try {
                    out.getFD().sync();
                } catch (IOException ignored) {
                }
                out.close();
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Check if a filename is "safe" (no metacharacters or spaces).
     *
     * @param file The file to check
     */
    public static boolean isFilenameSafe(File file) {
        // Note, we check whether it matches what's known to be safe,
        // rather than what's known to be unsafe.  Non-ASCII, control
        // characters, etc. are all unsafe by default.
        return SAFE_FILENAME_PATTERN.matcher(file.getPath()).matches();
    }

    /**
     * Read a text file into a String, optionally limiting the length.
     *
     * @param file     to read (will not seek, so things like /proc files are OK)
     * @param max      length (positive for head, negative of tail, 0 for no limit)
     * @param ellipsis to add of the file was truncated (can be null)
     * @return the contents of the file, possibly truncated
     * @throws IOException if something goes wrong reading the file
     */
    public static String readTextFile(File file, int max, String ellipsis) throws IOException {
        InputStream input = new FileInputStream(file);
        // wrapping a BufferedInputStream around it because when reading /proc with unbuffered
        // input stream, bytes read not equal to buffer size is not necessarily the correct
        // indication for EOF; but it is true for BufferedInputStream due to its implementation.
        BufferedInputStream bis = new BufferedInputStream(input);
        try {
            long size = file.length();
            if (max > 0 || (size > 0 && max == 0)) {  // "head" mode: read the first N bytes
                if (size > 0 && (max == 0 || size < max)) max = (int) size;
                byte[] data = new byte[max + 1];
                int length = bis.read(data);
                if (length <= 0) return "";
                if (length <= max) return new String(data, 0, length);
                if (ellipsis == null) return new String(data, 0, max);
                return new String(data, 0, max) + ellipsis;
            } else if (max < 0) {  // "tail" mode: keep the last N
                int len;
                boolean rolled = false;
                byte[] last = null, data = null;
                do {
                    if (last != null) rolled = true;
                    byte[] tmp = last;
                    last = data;
                    data = tmp;
                    if (data == null) data = new byte[-max];
                    len = bis.read(data);
                } while (len == data.length);

                if (last == null && len <= 0) return "";
                if (last == null) return new String(data, 0, len);
                if (len > 0) {
                    rolled = true;
                    System.arraycopy(last, len, last, 0, last.length - len);
                    System.arraycopy(data, 0, last, last.length - len, len);
                }
                if (ellipsis == null || !rolled) return new String(last);
                return ellipsis + new String(last);
            } else {  // "cat" mode: size unknown, read it all in streaming fashion
                ByteArrayOutputStream contents = new ByteArrayOutputStream();
                int len;
                byte[] data = new byte[1024];
                do {
                    len = bis.read(data);
                    if (len > 0) contents.write(data, 0, len);
                } while (len == data.length);
                return contents.toString();
            }
        } finally {
            bis.close();
            input.close();
        }
    }

    /**
     * Writes string to file. Basically same as "echo -n $string > $filename"
     */
    public static void stringToFile(String filename, String string) throws IOException {
        try (FileWriter out = new FileWriter(filename)) {
            out.write(string);
        }
    }

    public static boolean isExists(String file) {
        return new File(file).exists();
    }

    public static boolean delete(String file) {
        return new File(file).delete();
    }

    public static boolean createFile(String filePath) {
        try {
            File file = new File(filePath);
            File parentFile = file.getParentFile();
            if (!parentFile.exists()) {
                parentFile.mkdirs();
            }
            return parentFile.exists() && file.createNewFile();
        } catch (IOException ignore) {
            return false;
        }
    }

    public static void delDirectory(Context context, File file, boolean z) throws IOException {
        if (file.exists() && file.isDirectory()) {
            File[] listFiles = file.listFiles();
            if (listFiles == null) {
                throw new IOException("Failed to list contents of " + file);
            }
            for (File file2 : listFiles) {
                if (file2.isDirectory()) {
                    cleanDirectory(context, file2);
                } else {
                    file2.delete();
                }
            }
            if (z) {
                file.delete();
            }
        }
    }

    public static void cleanDirectory(Context context, File file) {
        if (file.exists() && file.isDirectory()) {
            File[] listFiles = file.listFiles();
            for (File forceDelete : listFiles) {
                forceDelete(context, forceDelete);
            }
            file.delete();
        }
    }

    public static void forceDelete(Context context, File file) {
        if (file.isDirectory()) {
            cleanDirectory(context, file);
            return;
        }
        if (file.exists()) {
            file.delete();
        }
        if (file.exists() && file.getPath().contains("sdcard1")) {
            // TODO: 8/13/20 外置sd卡根目录URI
            String extSdCardUri = "";
            if (!TextUtils.isEmpty(extSdCardUri)) {
                ExtSdUtils.deleteFiles(file, Uri.parse(extSdCardUri), context);
            }
        }
    }

    public static void openFile(Context context, File file) {
        if (file.isFile()) {
            Intent intent = new Intent();
            intent.setAction("android.intent.action.VIEW");
            String lowerCase = getFileEXT(file.getName()).toLowerCase();
            if (file.exists()) {
                if (checkEndsInArray(lowerCase, new String[]{"png", "gif", "jpg", "bmp"})) {
                    intent.setDataAndType(Uri.fromFile(file), "image/*");
                } else {
                    if (checkEndsInArray(lowerCase, new String[]{"apk"})) {
                        intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
                    } else {
                        if (checkEndsInArray(lowerCase, new String[]{"mp3", "amr", "ogg", "mid", "wav"})) {
                            intent.setDataAndType(Uri.fromFile(file), "audio/*");
                        } else {
                            if (checkEndsInArray(lowerCase, new String[]{"mp4", "3gp", "mpeg", "mov", "flv"})) {
                                intent.setDataAndType(Uri.fromFile(file), "video/*");
                            } else {
                                if (checkEndsInArray(lowerCase, new String[]{"txt", "ini", "log", "java", "xml", "html"})) {
                                    intent.setDataAndType(Uri.fromFile(file), "text/*");
                                } else {
                                    if (checkEndsInArray(lowerCase, new String[]{"doc", "docx"})) {
                                        intent.setDataAndType(Uri.fromFile(file), "application/msword");
                                    } else {
                                        if (checkEndsInArray(lowerCase, new String[]{"xls", "xlsx"})) {
                                            intent.setDataAndType(Uri.fromFile(file), "application/vnd.ms-excel");
                                        } else {
                                            if (checkEndsInArray(lowerCase, new String[]{"ppt", "pptx"})) {
                                                intent.setDataAndType(Uri.fromFile(file), "application/vnd.ms-powerpoint");
                                            } else {
                                                if (checkEndsInArray(lowerCase, new String[]{"chm"})) {
                                                    intent.setDataAndType(Uri.fromFile(file), "application/x-chm");
                                                } else {
                                                    intent.setDataAndType(Uri.fromFile(file), "application/" + lowerCase);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                try {
                    context.startActivity(intent);
                } catch (Exception ignored) {
                }
            }
        }
    }

    public static String getFileEXT(String str) {
        if (str.contains(".")) {
            return str.substring(str.lastIndexOf("."));
        }
        return "";
    }

    public static boolean checkEndsInArray(String str, String[] strArr) {
        for (String equals : strArr) {
            if (str.equals(equals)) {
                return true;
            }
        }
        return false;
    }

    private static boolean externalMemoryAvailable() {
        return Environment.getExternalStorageState().equals("mounted");
    }

    public static long getAvailableExternalMemorySize() {
        long size;
        if (FileUtils.externalMemoryAvailable()) {
            StatFs v1 = new StatFs(Environment.getExternalStorageDirectory().getPath());
            size = (((long) v1.getAvailableBlocks())) * (((long) v1.getBlockSize()));
        } else {
            size = FileUtils.getAvailableInternalMemorySize();
        }
        return size;
    }

    private static long getAvailableInternalMemorySize() {
        StatFs size = new StatFs(Environment.getDataDirectory().getPath());
        return size.getBlockSizeLong() * size.getAvailableBlocksLong();
    }

    public static long getTotalExternalMemorySize() {
        long size;
        try {
            if (FileUtils.externalMemoryAvailable()) {
                StatFs v1 = new StatFs(Environment.getExternalStorageDirectory().getPath());
                size = (((long) v1.getBlockCount())) * (((long) v1.getBlockSize()));
                return size;
            }
            size = FileUtils.getInternalMemorySize();
        } catch (Exception e) {
            e.printStackTrace();
            size = FileUtils.getInternalMemorySize();
        }
        return size;
    }

    private static long getInternalMemorySize() {
        StatFs size = new StatFs(Environment.getDataDirectory().getPath());
        return size.getBlockCountLong() * size.getBlockSizeLong();
    }

    public static String formatFileSize(long size) {
        return new DecimalFormat("0.00").format((((double) size)) / (1024 * 1024 * 1024));
    }

}
