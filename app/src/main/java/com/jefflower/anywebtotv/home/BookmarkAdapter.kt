package com.jefflower.anywebtotv.home

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.jefflower.anywebtotv.R
import com.jefflower.anywebtotv.data.Bookmark
import java.io.File

class BookmarkAdapter(
    private var items: List<Bookmark>,
    private val onClick: (Bookmark?) -> Unit,
    private val onLongClick: (Bookmark, View) -> Unit
) : RecyclerView.Adapter<BookmarkAdapter.VH>() {

    fun submit(list: List<Bookmark>) {
        items = list
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = items.size + 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_bookmark_tile, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val isAddTile = position == items.size
        if (isAddTile) {
            holder.name.text = holder.itemView.context.getString(R.string.add_site)
            holder.icon.setImageResource(R.drawable.ic_add)
            holder.itemView.setOnClickListener { onClick(null) }
            holder.itemView.setOnLongClickListener(null)
        } else {
            val b = items[position]
            holder.name.text = b.name
            val iconFile = b.iconPath?.let { File(it) }
            if (iconFile != null && iconFile.exists() && iconFile.length() > 0) {
                runCatching { BitmapFactory.decodeFile(iconFile.absolutePath) }.getOrNull()?.also {
                    holder.icon.setImageBitmap(it)
                } ?: holder.icon.setImageResource(R.drawable.ic_globe)
            } else {
                holder.icon.setImageResource(R.drawable.ic_globe)
            }
            holder.itemView.setOnClickListener { onClick(b) }
            holder.itemView.setOnLongClickListener { onLongClick(b, holder.itemView); true }
        }

        holder.itemView.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
            val target = if (hasFocus) 1.08f else 1f
            v.animate().scaleX(target).scaleY(target)
                .setDuration(140)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()
        }
    }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val icon: ImageView = v.findViewById(R.id.icon)
        val name: TextView = v.findViewById(R.id.name)
    }
}
