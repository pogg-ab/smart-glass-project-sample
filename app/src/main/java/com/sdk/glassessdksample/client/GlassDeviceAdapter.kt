package com.sdk.glassessdksample.client

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sdk.glassessdksample.R

class GlassDeviceAdapter(
    private var devices: List<GlassDevice>,
    private val onItemClick: (GlassDevice) -> Unit
) : RecyclerView.Adapter<GlassDeviceAdapter.ViewHolder>() {

    fun updateDevices(newDevices: List<GlassDevice>) {
        devices = newDevices
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.recycleview_item_device, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val device = devices[position]
        holder.bind(device, onItemClick)
    }

    override fun getItemCount(): Int = devices.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvName: TextView = view.findViewById(R.id.rcv_device_name)
        private val tvAddress: TextView = view.findViewById(R.id.rcv_device_address)
        private val tvRssi: TextView = view.findViewById(R.id.rcv_device_rssi)

        fun bind(device: GlassDevice, onItemClick: (GlassDevice) -> Unit) {
            tvName.text = device.name
            tvAddress.text = device.address
            tvRssi.text = "${device.rssi} dBm"
            itemView.setOnClickListener { onItemClick(device) }
        }
    }
}
