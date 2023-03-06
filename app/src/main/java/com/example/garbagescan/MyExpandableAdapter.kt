package com.example.garbagescan

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.gaozhongkui.garbagescanner.base.BaseScanInfo
import com.gaozhongkui.garbagescanner.model.ScanItemType
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
        return GroupViewHolder(layoutInflater.inflate(R.layout.item_group_layout, null, false))
    }

    override fun onCreateGroupViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        return NormalViewHolder(layoutInflater.inflate(R.layout.item_child_layout, null, false))
    }

    override fun onGroupViewHolderExpandChange(holder: ViewHolder, groupPosition: Int, animDuration: Long, expand: Boolean) {
    }

    override fun onBindGroupViewHolder(holder: ViewHolder, groupPosition: Int, expand: Boolean, payloads: List<Any>) {
        if (holder is GroupViewHolder) {
            mapTypes?.let {
                holder.titleTxt.text = getScanItemTypeByPosition(groupPosition).name
            }

        }
    }

    override fun onBindChildViewHolder(holder: ViewHolder, groupPosition: Int, childPosition: Int, payloads: List<Any>) {
        if (holder is NormalViewHolder) {
            mapTypes?.let { it ->
                val text = it[getScanItemTypeByPosition(groupPosition)]?.get(childPosition)
                text?.let {
                    holder.titleTxt.text = it.toString()
                }
            }

        }
    }


    class NormalViewHolder(itemView: View) : ViewHolder(itemView) {
        val titleTxt = itemView.findViewById<TextView>(R.id.tv_title)
    }

    class GroupViewHolder(itemView: View) : ViewHolder(itemView) {
        val titleTxt = itemView.findViewById<TextView>(R.id.tv_title)
    }
}