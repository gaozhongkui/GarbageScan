package com.gaozhongkui.garbagescanner.data.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.gaozhongkui.garbagescanner.data.model.GarbagePathInfo
import com.gaozhongkui.garbagescanner.utils.FileUtils

class GarbageManagerDB(cxt: Context) : SQLiteOpenHelper(cxt, DATABASE_NAME, null, DATABASE_VERSION) {
    init {
        moveFromAssets(cxt)
    }

    override fun onCreate(p0: SQLiteDatabase?) {
    }

    override fun onUpgrade(p0: SQLiteDatabase?, p1: Int, p2: Int) {
    }

    fun getDBGarbagePathInfoList(): List<GarbagePathInfo> {
        val pathList: MutableList<GarbagePathInfo> = mutableListOf()
        readableDatabase.query(
            "file_path_info_clean", null, null, null, null, null, null
        ).use { c ->
            if (c != null && c.moveToFirst()) {
                do {
                    val idIndex = c.getColumnIndex("id")
                    val id = c.getInt(idIndex).toLong()
                    val packageNameIndex = c.getColumnIndex("packageName")
                    val packageName = c.getString(packageNameIndex)
                    val appNameIndex = c.getColumnIndex("appName")
                    val appName = c.getString(appNameIndex)
                    val garbageTypeIndex = c.getColumnIndex("garbagetype")
                    val garbageType = c.getString(garbageTypeIndex)
                    val garbageNameIndex = c.getColumnIndex("garbagename")
                    val garbageName = c.getString(garbageNameIndex)
                    val filePathIndex = c.getColumnIndex("filePath")
                    val filePath = c.getString(filePathIndex)
                    val garbagePathInfo = GarbagePathInfo(id, filePath, packageName, garbageName)
                    garbagePathInfo.name = appName
                    pathList.add(garbagePathInfo)
                } while (c.moveToNext())
            }
        }
        readableDatabase.close()
        return pathList
    }


    companion object {
        private const val TAG = "GarbageManagerDB"
        private const val DATABASE_VERSION = 1
        private const val DATABASE_NAME = "data_paths.db"
        private const val SHARE_PRE_FILE = "garbage_clean_config"
        private const val KEY_CURRENT_PATH_VERSION = "current_path_version"

        private fun moveFromAssets(cxt: Context) {
            val dbFile = cxt.getDatabasePath(DATABASE_NAME)
            if (!dbFile.exists() || versionChanged(cxt)) {
                val parentDir = dbFile.parentFile
                if (parentDir != null && !parentDir.exists()) {
                    parentDir.mkdirs()
                }
                try {
                    if (FileUtils.copyToFile(cxt.assets.open(DATABASE_NAME), dbFile)) {
                        setCurrentVersion(cxt)
                    }
                } catch (ignore: Exception) {

                }
            }
        }

        private fun versionChanged(context: Context): Boolean {
            val sp = context.getSharedPreferences(SHARE_PRE_FILE, Context.MODE_PRIVATE)
            val lastVersion = sp.getInt(KEY_CURRENT_PATH_VERSION, 0)
            return lastVersion < DATABASE_VERSION
        }

        private fun setCurrentVersion(context: Context) {
            val sp = context.getSharedPreferences(SHARE_PRE_FILE, Context.MODE_PRIVATE)
            sp.edit().putInt(KEY_CURRENT_PATH_VERSION, DATABASE_VERSION).apply()
        }
    }
}