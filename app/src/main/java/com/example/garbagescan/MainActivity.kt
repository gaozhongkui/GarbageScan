package com.example.garbagescan

import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.gaozhongkui.garbagescanner.GarbageScannerManager
import com.gaozhongkui.garbagescanner.base.BaseScanInfo
import com.gaozhongkui.garbagescanner.callback.IGarbageScannerCallback
import com.gaozhongkui.garbagescanner.model.ScanItemType
import com.gaozhongkui.garbagescanner.scanner.FileScanner

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val mFileScanner = GarbageScannerManager()
        mFileScanner.setScannerCallback(object : IGarbageScannerCallback {
            override fun onStart() {
                Log.d(TAG, "onStart() called")
            }

            override fun onFind(info: BaseScanInfo) {
                Log.d(TAG, "onFind() called with: info = $info")
            }

            override fun onFinish(mapTypes: Map<ScanItemType, List<BaseScanInfo>>) {
                Log.d(TAG, "onFinish() called with: mapTypes = $mapTypes")
            }

        })
        mFileScanner.startAllScan(this)

    }

    companion object {
        private const val TAG = "MainActivity"
    }
}