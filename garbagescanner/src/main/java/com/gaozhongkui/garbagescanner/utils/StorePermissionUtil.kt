package com.gaozhongkui.garbagescanner.utils

import android.Manifest
import android.app.Activity
import android.app.AppOpsManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

// AppOpsManager.OPSTR_MANAGE_EXTERNAL_STORAGE is a @SystemAPI at the moment
// We should remove the annotation for applications to avoid hardcoded value
const val MANAGE_EXTERNAL_STORAGE_PERMISSION = "android:manage_external_storage"
const val NOT_APPLICABLE = "N/A"
const val MANAGE_EXTERNAL_STORAGE_PERMISSION_REQUEST = 1
const val READ_EXTERNAL_STORAGE_PERMISSION_REQUEST = 2

/**
 * 使用方式
private fun initStorePermission(){
val isHasPermission = checkStoragePermission(requireActivity())
if (!isHasPermission){//假如没有权限则进行授权判断
requestStoragePermission(requireContext())
}else{
//已经授权成功
}
}
 */
fun getStoragePermissionName(): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        MANAGE_EXTERNAL_STORAGE_PERMISSION
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
}

fun openPermissionSettings(activity: AppCompatActivity) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        requestStoragePermissionApi30(activity)
    } else {
        activity.startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", activity.packageName, null)
            )
        )
    }
}

fun getLegacyStorageStatus(): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        Environment.isExternalStorageLegacy().toString()
    } else {
        NOT_APPLICABLE
    }
}

fun getPermissionStatus(activity: AppCompatActivity): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        checkStoragePermissionApi30(activity).toString()
    } else {
        checkStoragePermissionApi19(activity).toString()
    }
}

fun checkStoragePermission(activity: FragmentActivity): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        checkStoragePermissionApi30(activity)
    } else {
        checkStoragePermissionApi19(activity)
    }
}

fun requestStoragePermission(activity: FragmentActivity) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        requestStoragePermissionApi30(activity)
    }
    // If you want to see the default storage behavior on Android Q once the permission is granted
    // Set the "requestLegacyExternalStorage" flag in the AndroidManifest.xml file to false
    else {
        requestStoragePermissionApi19(activity)
    }
}

@RequiresApi(30)
fun checkStoragePermissionApi30(activity: FragmentActivity): Boolean {
    val appOps = activity.getSystemService(AppOpsManager::class.java)
    val mode = appOps.unsafeCheckOpNoThrow(
        MANAGE_EXTERNAL_STORAGE_PERMISSION,
        activity.applicationInfo.uid,
        activity.packageName
    )

    return mode == AppOpsManager.MODE_ALLOWED
}

@RequiresApi(30)
fun requestStoragePermissionApi30(activity: FragmentActivity) {
    try {
        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
        intent.data = Uri.parse("package:" + activity.packageName)
        activity.startActivityForResult(intent, MANAGE_EXTERNAL_STORAGE_PERMISSION_REQUEST)
    } catch (e: Exception) {
        try {
            activity.startActivityForResult(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION), MANAGE_EXTERNAL_STORAGE_PERMISSION_REQUEST)
        } catch (e: Exception) {

        }
    }
}

@RequiresApi(19)
fun checkStoragePermissionApi19(activity: FragmentActivity): Boolean {
    val status =
        ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE)

    return status == PackageManager.PERMISSION_GRANTED
}

@RequiresApi(19)
fun requestStoragePermissionApi19(activity: FragmentActivity) {
    val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    ActivityCompat.requestPermissions(
        activity,
        permissions,
        READ_EXTERNAL_STORAGE_PERMISSION_REQUEST
    )
}

fun applyOpenInBackgroundPermission(context: Activity, requestCode: Int) {
    val packageName = context.packageName
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
    intent.data = Uri.fromParts("package", packageName, null)
    intent.flags = Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY
    context.startActivityForResult(intent, requestCode)
}
