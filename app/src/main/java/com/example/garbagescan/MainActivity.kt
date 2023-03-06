package com.example.garbagescan

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.gaozhongkui.garbagescanner.GarbageScannerManager
import com.gaozhongkui.garbagescanner.base.BaseScanInfo
import com.gaozhongkui.garbagescanner.callback.IGarbageScannerCallback
import com.gaozhongkui.garbagescanner.model.*
import com.gaozhongkui.garbagescanner.utils.AppPackageUtils
import com.gaozhongkui.garbagescanner.utils.checkStoragePermission
import com.gaozhongkui.garbagescanner.utils.requestStoragePermission
import pokercc.android.expandablerecyclerview.ExpandableRecyclerView

class MainActivity : AppCompatActivity() {
    private lateinit var pathTxt: TextView
    private lateinit var recyclerView: ExpandableRecyclerView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        pathTxt = findViewById(R.id.tv_path)
        recyclerView = findViewById(R.id.recycler_view)
        findViewById<View>(R.id.bt_start).setOnClickListener {
            startScanner()
        }

        recyclerView.layoutManager = LinearLayoutManager(this)

    }

    private fun startScanner() {
        if (!AppPackageUtils.hasPermissionToReadNetworkStats(this)) {
            AppPackageUtils.requestReadNetworkStats(this)
        } else if (!checkStoragePermission(this)) {
            requestStoragePermission(this)
        } else {
            val mFileScanner = GarbageScannerManager()
            mFileScanner.setScannerCallback(object : IGarbageScannerCallback {
                override fun onStart() {
                    Log.d(TAG, "onStart() called")
                    pathTxt.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                }

                override fun onFind(info: BaseScanInfo) {
                    Log.d(TAG, "onFind() called with: info = $info")
                    val filePath = when (info) {
                        is AdGarbageInfo -> {
                            info.filePath
                        }
                        is ApkFileInfo -> {
                            info.filePath
                        }
                        is GarbagePathInfo -> {
                            info.filePath
                        }
                        is NormalGarbageInfo -> {
                            info.filePath
                        }
                        is UnloadResidueInfo -> {
                            info.filePath
                        }
                        else -> {
                            ""
                        }
                    }
                    pathTxt.text = "扫描路径：${filePath}"
                }

                override fun onFinish(mapTypes: Map<ScanItemType, List<BaseScanInfo>>) {
                    Log.d(TAG, "onFinish() called with: mapTypes = $mapTypes")
                    pathTxt.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE

                    val adapter = MyExpandableAdapter(layoutInflater)
                    adapter.setData(mapTypes)
                    recyclerView.adapter = adapter
                }

            })
            mFileScanner.startAllScan(this)
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}