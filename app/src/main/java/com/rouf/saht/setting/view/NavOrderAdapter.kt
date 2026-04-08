package com.rouf.saht.setting.view

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.rouf.saht.databinding.ItemNavOrderBinding

data class NavItem(val key: String, val label: String, val iconRes: Int)

class NavOrderAdapter(
    val items: MutableList<NavItem>,
    private val onOrderChanged: () -> Unit
) : RecyclerView.Adapter<NavOrderAdapter.VH>() {

    var touchHelper: ItemTouchHelper? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemNavOrderBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = items.size

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.binding.tvLabel.text = item.label
        holder.binding.ivIcon.setImageResource(item.iconRes)
        holder.binding.ivDrag.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                touchHelper?.startDrag(holder)
            }
            false
        }
    }

    fun onItemMoved(from: Int, to: Int) {
        val item = items.removeAt(from)
        items.add(to, item)
        notifyItemMoved(from, to)
        onOrderChanged()
    }

    inner class VH(val binding: ItemNavOrderBinding) : RecyclerView.ViewHolder(binding.root)
}
