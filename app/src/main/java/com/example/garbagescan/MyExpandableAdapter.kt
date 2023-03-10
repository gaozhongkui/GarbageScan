package com.example.garbagescan

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.gaozhongkui.garbagescanner.base.BaseScanInfo
import com.gaozhongkui.garbagescanner.model.*
import kotlinx.coroutines.*
import pokercc.android.expandablerecyclerview.ExpandableAdapter

class MyExpandableAdapter(private val layoutInflater: LayoutInflater) : ExpandableAdapter<ExpandableAdapter.ViewHolder>() {
    private var mapTypes: Map<ScanItemType, List<BaseScanInfo>>? = null
    fun setData(mapTypes: Map<ScanItemType, List<BaseScanInfo>>) {
        this.mapTypes = mapTypes
        notifyDataSetChanged()
    }

    override fun getGroupCount(): Int {
        return 5
    }

    private fun getScanItemTypeByPosition(position: Int): ScanItemType {
        return ScanItemType.values()[position]
    }

    override fun getChildCount(groupPosition: Int): Int {
        return mapTypes?.get(getScanItemTypeByPosition(groupPosition))?.size ?: 0
    }

    override fun onCreateChildViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        return NormalViewHolder(layoutInflater.inflate(R.layout.item_child_layout, null, false))
    }

    override fun onCreateGroupViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        return GroupViewHolder(layoutInflater.inflate(R.layout.item_group_layout, null, false))
    }

    override fun onGroupViewHolderExpandChange(holder: ViewHolder, groupPosition: Int, animDuration: Long, expand: Boolean) {
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onBindGroupViewHolder(holder: ViewHolder, groupPosition: Int, expand: Boolean, payloads: List<Any>) {
        if (holder is GroupViewHolder) {
            mapTypes?.let {
                val scanItemType = getScanItemTypeByPosition(groupPosition)
                val infoList = it[scanItemType]
                holder.titleTxt.text = scanItemType.name + " ："
                infoList?.let {
                    GlobalScope.launch {
                        val formatFileSize = DiskUtils.formatFileSize(getTotalSize(infoList), false)
                        withContext(Dispatchers.Main) {
                            holder.sizeTxt.text = formatFileSize
                        }
                    }

                }
            }

        }
    }

    @SuppressLint("SetTextI18n")
    override fun onBindChildViewHolder(holder: ViewHolder, groupPosition: Int, childPosition: Int, payloads: List<Any>) {
        if (holder is NormalViewHolder) {
            mapTypes?.let { it ->
                val text = it[getScanItemTypeByPosition(groupPosition)]?.get(childPosition)
                text?.let {
                    holder.titleTxt.text = "文件名：" + it.name
                    holder.sizeTxt.text = "文件大小：" + DiskUtils.formatFileSize(it.fileSize, false)

                    when (it) {
                        is AdGarbageInfo -> {
                            holder.packageTxt.text = "包名：" + it.packageName
                            holder.pathTxt.text = "路径：" + it.filePath
                        }
                        is ApkFileInfo -> {
                            holder.packageTxt.text = "包名："
                            holder.pathTxt.text = "路径：" + it.filePath
                        }
                        is AppCacheInfo -> {
                            holder.packageTxt.text = "包名：" + it.packageName
                            holder.pathTxt.text = "路径："
                        }
                        is GarbagePathInfo -> {
                            holder.packageTxt.text = "包名：" + it.packageName
                            holder.pathTxt.text = "路径：" + it.filePath
                        }
                        is NormalGarbageInfo -> {
                            holder.packageTxt.text = "包名："
                            holder.pathTxt.text = "路径：" + it.filePath
                        }
                        is UnloadResidueInfo -> {
                            holder.packageTxt.text = "包名：" + it.packageName
                            holder.pathTxt.text = "路径：" + it.filePath
                        }
                    }


                }
            }

        }
    }

    override fun getGroupItemViewType(groupPosition: Int): Int {
        return 100
    }


    class NormalViewHolder(itemView: View) : ViewHolder(itemView) {
        val titleTxt: TextView = itemView.findViewById(R.id.tv_title)
        val sizeTxt: TextView = itemView.findViewById(R.id.tv_size)
        val packageTxt: TextView = itemView.findViewById(R.id.tv_package)
        val pathTxt: TextView = itemView.findViewById(R.id.tv_path)
    }

    class GroupViewHolder(itemView: View) : ViewHolder(itemView) {
        val titleTxt: TextView = itemView.findViewById(R.id.tv_title)
        val sizeTxt: TextView = itemView.findViewById(R.id.tv_size)

    }

    private fun getTotalSize(list: List<BaseScanInfo>): Long {
        var total = 0L
        list.forEach {
            total += it.fileSize
        }
        return total
    }
}