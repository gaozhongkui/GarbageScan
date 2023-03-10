package com.example.garbagescan

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
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
    private var adapter: MyExpandableAdapter? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        pathTxt = findViewById(R.id.tv_path)
        recyclerView = findViewById(R.id.recycler_view)
        findViewById<View>(R.id.bt_start).setOnClickListener {
            startScanner()
        }

        findViewById<View>(R.id.bt_delete).setOnClickListener {
            deleteFiles()
        }
        adapter = MyExpandableAdapter(layoutInflater)
        recyclerView.adapter = adapter
//        recyclerView.layoutManager = LinearLayoutManager(this)
        val manager = GridLayoutManager(this, 3)
        manager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
               if( adapter?.getItemViewType(position)==100){
                   return 3
               }
                return 1
            }

        }
        recyclerView.layoutManager = manager


    }

    private fun deleteFiles() {

    }

    private fun startScanner() {
        if (!AppPackageUtils.hasPermissionToReadNetworkStats(this)) {
            AppPackageUtils.requestReadNetworkStats(this)
        } else if (!checkStoragePermission(this)) {
            requestStoragePermission(this)
        } else {
            val mFileScanner = GarbageScannerManager()
            mFileScanner.setScannerCallback(object : IGarbageScannerCallback {
                var startTime = 0L
                override fun onStart() {
                    Log.d(TAG, "onStart() called")
                    pathTxt.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                    startTime = System.currentTimeMillis()
                }

                override fun onFind(info: BaseScanInfo) {
                    Log.d(TAG, "onFind() called with: info = $info")
                    val filePath = when (info) {
                        is AdGarbageInfo -> {
                            info.filePaths
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
                    pathTxt.text = "???????????????${filePath}"
                }

                override fun onFinish(sortList: List<SortScannerInfo>) {
                    Log.d(TAG, "onFinish() called with: mapTypes = $sortList")
                    pathTxt.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                    adapter?.setData(sortList)
                    Log.d(TAG, "onFinish() called with: ?????? = " + (System.currentTimeMillis() - startTime))
                }

            })
            mFileScanner.startAllScan(this)
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}