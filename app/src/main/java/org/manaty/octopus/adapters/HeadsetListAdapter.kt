package org.manaty.octopus.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import net.manaty.octopusync.api.Headset
import org.manaty.octopus.R

class HeadsetListAdapter (val context : Context, val headsetList: MutableList<Headset>) : BaseAdapter(){
    val mInflater: LayoutInflater = LayoutInflater.from(context)

    private class ItemRowHolder(row: View?) {

        val headsetName : TextView

        init {
            headsetName = row?.findViewById(android.R.id.text1) as TextView
        }
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {

        val view: View
        val vh: ItemRowHolder
        if (convertView == null) {
            view = mInflater.inflate(R.layout.support_simple_spinner_dropdown_item, parent, false)
            vh = ItemRowHolder(view)
            view?.tag = vh
        } else {
            view = convertView
            vh = view.tag as ItemRowHolder
        }

        vh.headsetName.text = headsetList[position].code
        return view
    }

    override fun getItem(position: Int): Any {
        return headsetList[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getCount(): Int {
        return headsetList.size
    }
}