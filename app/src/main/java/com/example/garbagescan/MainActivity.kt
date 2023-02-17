package com.example.garbagescan

import android.os.Bundle
import android.os.Environment
import androidx.appcompat.app.AppCompatActivity
import com.gaozhongkui.garbagescanner.scanner.FileScanner

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val sdPath = Environment.getExternalStorageDirectory().absolutePath
        val scanPath = arrayOf(sdPath)
        val mFileScanner = FileScanner()
        mFileScanner.setScanPath(scanPath)
        mFileScanner.startScan(object : FileScanner.ScanCallback {
            override fun onStart() {
            }

            override fun onFind(threadId: Long, path: String?, size: Long, modify: Long) {
            }

            override fun onFinish(isCancel: Boolean) {
            }

        })
    }
}