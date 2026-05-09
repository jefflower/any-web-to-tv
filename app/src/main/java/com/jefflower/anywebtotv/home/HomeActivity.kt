package com.jefflower.anywebtotv.home

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.PopupMenu
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jefflower.anywebtotv.App
import com.jefflower.anywebtotv.R
import com.jefflower.anywebtotv.data.Bookmark
import com.jefflower.anywebtotv.data.FaviconLoader
import com.jefflower.anywebtotv.web.WebActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HomeActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_FORWARD_URL = "url"
        const val EXTRA_FORWARD_TITLE = "title"
    }

    private lateinit var grid: RecyclerView
    private lateinit var subtitle: TextView
    private lateinit var adapter: BookmarkAdapter
    private val dao by lazy { App.instance.db.bookmarkDao() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Sideload entry point: `adb shell am start -n .../.home.HomeActivity --es url "http://..."`.
        // Useful for testing without manually adding a bookmark via remote.
        // SharedPref + setIntent() guard so a process restart with the same intent
        // does not re-fire the forward (would otherwise loop on Mi TV process churn).
        val sp = getSharedPreferences("home_state", MODE_PRIVATE)
        val forwardCookie = intent.getStringExtra(EXTRA_FORWARD_URL)?.takeIf { it.isNotBlank() }
        val lastForwarded = sp.getString("last_forwarded_url", null)
        if (forwardCookie != null && forwardCookie != lastForwarded) {
            val title = intent.getStringExtra(EXTRA_FORWARD_TITLE)
            sp.edit().putString("last_forwarded_url", forwardCookie).apply()
            val clean = Intent(intent).apply {
                removeExtra(EXTRA_FORWARD_URL)
                removeExtra(EXTRA_FORWARD_TITLE)
            }
            setIntent(clean)
            startActivity(Intent(this, WebActivity::class.java).apply {
                putExtra(WebActivity.EXTRA_URL, forwardCookie)
                putExtra(WebActivity.EXTRA_TITLE, title)
            })
        }

        setContentView(R.layout.activity_home)

        grid = findViewById(R.id.grid)
        subtitle = findViewById(R.id.subtitle)

        adapter = BookmarkAdapter(emptyList(),
            onClick = { bm ->
                if (bm == null) showAddDialog(null) else openBookmark(bm)
            },
            onLongClick = { bm, anchor -> showItemMenu(bm, anchor) }
        )
        grid.layoutManager = GridLayoutManager(this, 5)
        grid.adapter = adapter

        lifecycleScope.launch {
            dao.observeAll().collectLatest { list ->
                adapter.submit(list)
                subtitle.text = if (list.isEmpty()) getString(R.string.empty_hint)
                                else getString(R.string.app_name) + " · " + list.size
            }
        }
    }

    private fun openBookmark(b: Bookmark) {
        startActivity(Intent(this, WebActivity::class.java).apply {
            putExtra(WebActivity.EXTRA_URL, b.url)
            putExtra(WebActivity.EXTRA_TITLE, b.name)
        })
    }

    private fun showAddDialog(existing: Bookmark?) {
        val d = AddSiteDialog()
        d.existing = existing
        d.onSave = { url, name -> saveBookmark(existing, url, name) }
        d.show(supportFragmentManager, "add")
    }

    private fun saveBookmark(existing: Bookmark?, url: String, name: String) {
        lifecycleScope.launch {
            val pos = (dao.maxPosition() ?: -1) + 1
            val toSave = existing?.copy(url = url, name = name)
                ?: Bookmark(name = name, url = url, position = pos)
            val id = if (existing == null) dao.insert(toSave) else { dao.update(toSave); toSave.id }
            // fetch favicon async
            val iconPath = FaviconLoader.fetch(this@HomeActivity, url)
            if (iconPath != null) dao.setIconPath(id, iconPath)
        }
    }

    private fun showItemMenu(b: Bookmark, anchor: View) {
        val menu = PopupMenu(this, anchor)
        menu.menu.add(0, 1, 0, getString(R.string.action_open))
        menu.menu.add(0, 2, 1, getString(R.string.action_edit))
        menu.menu.add(0, 3, 2, getString(R.string.action_delete))
        menu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> { openBookmark(b); true }
                2 -> { showAddDialog(b); true }
                3 -> { lifecycleScope.launch { dao.delete(b) }; true }
                else -> false
            }
        }
        menu.show()
    }
}
