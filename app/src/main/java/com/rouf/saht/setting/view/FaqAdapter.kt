package com.rouf.saht.setting.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.rouf.saht.R
import com.rouf.saht.common.activity.BaseActivity

sealed class FaqItem {
    data class Section(val title: String) : FaqItem()
    data class Entry(val question: String, val answer: String) : FaqItem()
}

class FaqAdapter(private val items: List<FaqItem>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val expandedSet = mutableSetOf<Int>()

    override fun getItemViewType(position: Int) = when (items[position]) {
        is FaqItem.Section -> TYPE_SECTION
        is FaqItem.Entry -> TYPE_ENTRY
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_SECTION) {
            SectionVH(inflater.inflate(R.layout.item_faq_section, parent, false))
        } else {
            EntryVH(inflater.inflate(R.layout.item_faq, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is FaqItem.Section -> (holder as SectionVH).bind(item)
            is FaqItem.Entry -> (holder as EntryVH).bind(item, position)
        }
    }

    override fun getItemCount() = items.size

    inner class SectionVH(view: View) : RecyclerView.ViewHolder(view) {
        private val tv = view.findViewById<TextView>(R.id.tv_section)
        fun bind(item: FaqItem.Section) {
            tv.text = item.title
            tv.setTextColor(BaseActivity.effectivePrimary(tv.context))
        }
    }

    inner class EntryVH(view: View) : RecyclerView.ViewHolder(view) {
        private val header: View = view.findViewById(R.id.header)
        private val question: TextView = view.findViewById(R.id.tv_question)
        private val answer: TextView = view.findViewById(R.id.tv_answer)
        private val chevron: ImageView = view.findViewById(R.id.iv_chevron)

        fun bind(item: FaqItem.Entry, position: Int) {
            question.text = item.question
            answer.text = item.answer

            val expanded = position in expandedSet
            answer.visibility = if (expanded) View.VISIBLE else View.GONE
            chevron.rotation = if (expanded) 90f else 270f

            header.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos == RecyclerView.NO_POSITION) return@setOnClickListener
                if (pos in expandedSet) {
                    expandedSet.remove(pos)
                    answer.visibility = View.GONE
                    chevron.animate().rotation(270f).setDuration(200).start()
                } else {
                    expandedSet.add(pos)
                    answer.visibility = View.VISIBLE
                    chevron.animate().rotation(90f).setDuration(200).start()
                }
            }
        }
    }

    companion object {
        private const val TYPE_SECTION = 0
        private const val TYPE_ENTRY = 1
    }
}
