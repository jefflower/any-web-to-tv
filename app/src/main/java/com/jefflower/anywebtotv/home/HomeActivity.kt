package com.jefflower.anywebtotv.home

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jefflower.anywebtotv.App
import com.jefflower.anywebtotv.R
import com.jefflower.anywebtotv.data.Bookmark
import com.jefflower.anywebtotv.data.FaviconLoader
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HomeActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_FORWARD_URL = "url"
        const val EXTRA_FORWARD_TITLE = "title"

        // Preferred packages (in priority order). When a bookmark is opened we
        // pick the first one that is installed, so users with Bromite installed
        // get the modern Chromium engine; users without it fall through to the
        // system browser/WebView.
        //
        // Why these:
        //   - org.bromite.bromite       → Bromite (Chromium 108, modern JS)
        //   - org.mozilla.firefox       → Firefox (also GeckoView, modern JS)
        //   - org.bromite.webview       → Bromite WebView host activity (rare)
        // Mi TV's tvhome compliance scan whitelists these org.bromite.* /
        // org.mozilla.* packages, so they survive while custom apps with
        // multi-process renderers do not. Our own app is a tiny launcher
        // (no rendering, no native services), so MIUI doesn't kill us either.
        val PREFERRED_BROWSERS = listOf(
            "org.bromite.bromite",
            "org.mozilla.firefox",
            "org.bromite.webview"
        )
    }

    private lateinit var grid: RecyclerView
    private lateinit var subtitle: TextView
    private lateinit var adapter: BookmarkAdapter
    private val dao by lazy { App.instance.db.bookmarkDao() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Debug / sideload entry: `adb shell am start -n .../.home.HomeActivity --es url "http://..."`.
        // SharedPref + setIntent() guard so a process restart with the same
        // launching intent does NOT re-fire the forward and create a loop.
        val sp = getSharedPreferences("home_state", MODE_PRIVATE)
        val forwardCookie = intent.getStringExtra(EXTRA_FORWARD_URL)?.takeIf { it.isNotBlank() }
        val lastForwarded = sp.getString("last_forwarded_url", null)
        if (forwardCookie != null && forwardCookie != lastForwarded) {
            sp.edit().putString("last_forwarded_url", forwardCookie).apply()
            val clean = Intent(intent).apply {
                removeExtra(EXTRA_FORWARD_URL)
                removeExtra(EXTRA_FORWARD_TITLE)
            }
            setIntent(clean)
            launchInBrowser(forwardCookie)
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
        launchInBrowser(b.url)
    }

    /**
     * Hand off the URL to a real browser via Intent.ACTION_VIEW.
     * Tries [PREFERRED_BROWSERS] first; falls back to the system default browser.
     */
    private fun launchInBrowser(url: String) {
        val uri = Uri.parse(url)
        for (pkg in PREFERRED_BROWSERS) {
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage(pkg)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
                return
            }
        }
        // Fallback: any handler the system has (Chrome 83 system WebView via default browser).
        val fallback = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            startActivity(fallback)
        } catch (_: Exception) {
            Toast.makeText(this, "未找到可用浏览器，请安装 Bromite 或 Firefox", Toast.LENGTH_LONG).show()
        }
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
