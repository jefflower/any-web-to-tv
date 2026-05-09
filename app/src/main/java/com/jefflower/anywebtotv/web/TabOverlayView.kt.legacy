package com.jefflower.anywebtotv.web

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jefflower.anywebtotv.R

class TabOverlayView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val list: RecyclerView
    private var tabs: List<TabManager.Tab> = emptyList()

    var onTabSelected: ((Int) -> Unit)? = null
    var onTabDeleteRequested: ((Int) -> Unit)? = null

    init {
        LayoutInflater.from(context).inflate(R.layout.view_tab_overview, this, true)
        list = findViewById(R.id.tab_list)
        list.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        isClickable = true  // swallow taps to background
    }

    fun show(allTabs: List<TabManager.Tab>, currentIndex: Int) {
        tabs = allTabs
        list.adapter = Adapter(tabs)
        visibility = View.VISIBLE
        post {
            list.scrollToPosition(currentIndex.coerceAtLeast(0))
            (list.layoutManager as LinearLayoutManager).findViewByPosition(currentIndex)?.requestFocus()
        }
    }

    fun hide() { visibility = View.GONE }
    fun isOpen(): Boolean = visibility == View.VISIBLE

    private inner class Adapter(val data: List<TabManager.Tab>) : RecyclerView.Adapter<VH>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_tab_card, parent, false)
            return VH(v)
        }
        override fun getItemCount(): Int = data.size
        override fun onBindViewHolder(holder: VH, position: Int) {
            val t = data[position]
            holder.title.text = t.title.ifBlank { t.url }
            holder.url.text = t.url
            holder.itemView.setOnClickListener { onTabSelected?.invoke(position) }
            holder.itemView.setOnLongClickListener {
                onTabDeleteRequested?.invoke(position); true
            }
            holder.itemView.onFocusChangeListener = OnFocusChangeListener { v, hasFocus ->
                val s = if (hasFocus) 1.06f else 1f
                v.animate().scaleX(s).scaleY(s).setDuration(120)
                    .setInterpolator(AccelerateDecelerateInterpolator()).start()
            }
        }
    }

    private class VH(v: View) : RecyclerView.ViewHolder(v) {
        val title: TextView = v.findViewById(R.id.tab_title)
        val url: TextView = v.findViewById(R.id.tab_url)
    }
}
