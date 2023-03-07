package com.gaozhongkui.garbagescanner.utils

import android.annotation.TargetApi
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.io.IOException
import java.util.*

object ExtSdUtils {
    private fun getDocumentFile(
        file: File,
        context: Context,
        uri: Uri?,
        z: Boolean = false
    ): DocumentFile? {
        var str: String?
        var z2: Boolean
        val z3: Boolean
        val str2: String?
        val extSdCardFolder = getExtSdCardFolder(file, context) ?: return null
        try {
            val canonicalPath = file.canonicalPath
            if (extSdCardFolder != canonicalPath) {
                str2 = canonicalPath.substring(extSdCardFolder.length + 1)
                z3 = false
            } else {
                z3 = true
                str2 = null
            }
            z2 = z3
            str = str2
        } catch (e: IOException) {
            return null
        } catch (e2: Exception) {
            str = null
            z2 = true
        }
        if (uri == null) {
            return null
        }
        var fromTreeUri = DocumentFile.fromTreeUri(context, uri)
        if (z2) {
            return fromTreeUri
        }
        val split = str!!.split("\\/").toTypedArray()
        for (i in split.indices) {
            val findFile = fromTreeUri!!.findFile(split[i])
            fromTreeUri = findFile
                ?: if (i >= split.size - 1 && !z) {
                    fromTreeUri.createFile("image", split[i])
                } else if (fromTreeUri.createDirectory(split[i]) == null) {
                    return null
                } else {
                    fromTreeUri.createDirectory(split[i])
                }
        }
        return fromTreeUri
    }

    @TargetApi(19)
    private fun getExtSdCardFolder(file: File, context: Context): String? {
        val extSdCardPaths = getExtSdCardPaths(context)
        var i = 0
        while (i < extSdCardPaths.size) {
            try {
                if (file.canonicalPath.startsWith(extSdCardPaths[i])) {
                    return extSdCardPaths[i]
                }
                i++
            } catch (e: IOException) {
                return null
            }
        }
        return null
    }

    @TargetApi(19)
    private fun getExtSdCardPaths(context: Context): Array<String> {
        val arrayList = ArrayList<String>()
        for (file in context.getExternalFilesDirs("external")) {
            if (file != null && file != context.getExternalFilesDir("external")) {
                val lastIndexOf = file.absolutePath.lastIndexOf("/Android/data")
                if (lastIndexOf < 0) {
                    Log.w("AmazeFileUtils", "Unexpected external file dir: " + file.absolutePath)
                } else {
                    var substring = file.absolutePath.substring(0, lastIndexOf)
                    try {
                        substring = File(substring).canonicalPath
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                    arrayList.add(substring)
                }
            }
        }
        if (arrayList.isEmpty()) {
            arrayList.add(Environment.getExternalStorageDirectory().absolutePath)
        }
        return arrayList.toTypedArray()
    }

    @TargetApi(21)
    @JvmStatic
    fun deleteFiles(file: File, uri: Uri?, context: Context): Boolean {
        return try {
            val documentFile = getDocumentFile(file, context, uri)
            documentFile?.delete()
            true
        } catch (e: Exception) {
            false
        }
    }
}