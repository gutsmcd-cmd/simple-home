package io.github.gutsmcd.simplehome

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*

/** Simple Home Screen: app grid + search + dock, muted rose/sage, JP/EN, large text, big tiles. */
class MainActivity : Activity() {
    data class App(val label: String, val comp: ComponentName, val icon: Drawable)

    private val ROSE = Color.parseColor("#C9A3A8")
    private val SAGE = Color.parseColor("#9DAE98")
    private val BG = Color.parseColor("#F6F2EE")
    private val INK = Color.parseColor("#4A4043")
    private val CARD = Color.parseColor("#EFE7E3")

    private lateinit var prefs: android.content.SharedPreferences
    private var apps = listOf<App>()
    private lateinit var grid: GridView
    private lateinit var dock: LinearLayout
    private lateinit var search: EditText
    private lateinit var root: LinearLayout
    private val en get() = prefs.getBoolean("en", false)
    private val big get() = prefs.getBoolean("big", false)
    private val large get() = prefs.getBoolean("large", false)
    private fun t(ja: String, enS: String) = if (en) enS else ja
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun sp(base: Float) = if (large) base * 1.3f else base

    private val pkgReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) { loadApps(); refresh() }
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        prefs = getSharedPreferences("home", MODE_PRIVATE)
        loadApps(); build()
        registerReceiver(pkgReceiver, IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED); addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED); addDataScheme("package")
        })
    }
    override fun onDestroy() { unregisterReceiver(pkgReceiver); super.onDestroy() }
    override fun onNewIntent(i: Intent) { super.onNewIntent(i); search.setText(""); grid.smoothScrollToPosition(0) }
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() { if (search.text.isNotEmpty()) search.setText("") }

    private fun loadApps() {
        val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        apps = packageManager.queryIntentActivities(i, 0)
            .filter { it.activityInfo.packageName != packageName }
            .map { App(it.loadLabel(packageManager).toString(),
                ComponentName(it.activityInfo.packageName, it.activityInfo.name), it.loadIcon(packageManager)) }
            .sortedBy { it.label.lowercase() }
    }

    private fun round(color: Int, r: Int) = GradientDrawable().apply { setColor(color); cornerRadius = dp(r).toFloat() }

    private fun chip(label: String, on: Boolean, click: () -> Unit) = TextView(this).apply {
        text = label; setTextColor(if (on) Color.WHITE else INK)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sp(13f))
        background = round(if (on) SAGE else CARD, 16)
        setPadding(dp(12), dp(6), dp(12), dp(6))
        layoutParams = LinearLayout.LayoutParams(-2, -2).apply { marginEnd = dp(6) }
        setOnClickListener { click() }
    }

    private fun build() {
        root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(BG); fitsSystemWindows = true }
        setContentView(root)
        refresh()
    }

    private fun refresh() {
        val q = if (::search.isInitialized) search.text.toString() else ""
        root.removeAllViews()
        // top bar: toggles
        val bar = LinearLayout(this).apply { setPadding(dp(12), dp(10), dp(12), dp(4)); gravity = Gravity.CENTER_VERTICAL }
        bar.addView(chip(if (en) "日本語" else "English", false) { prefs.edit().putBoolean("en", !en).apply(); refresh() })
        bar.addView(chip(t("大きい文字", "Large text"), large) { prefs.edit().putBoolean("large", !large).apply(); refresh() })
        bar.addView(chip(t("大きいアイコン", "Big tiles"), big) { prefs.edit().putBoolean("big", !big).apply(); refresh() })
        bar.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f))
        bar.addView(chip("⚙", false) { startActivity(Intent(Settings.ACTION_HOME_SETTINGS)) })
        root.addView(HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false; addView(bar) })

        search = EditText(this).apply {
            hint = t("アプリを検索", "Search apps"); setText(q); setSingleLine()
            setTextColor(INK); setHintTextColor(Color.parseColor("#9A8F92"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, sp(16f))
            background = round(Color.WHITE, 24); setPadding(dp(18), dp(10), dp(18), dp(10))
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(dp(14), dp(6), dp(14), dp(8)) }
            setSelection(q.length)
            addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) { fill() }
                override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            })
            setOnEditorActionListener { _, _, _ ->
                filtered().firstOrNull()?.let { launch(it) }; true
            }
        }
        root.addView(search)

        grid = GridView(this).apply {
            numColumns = if (big) 3 else 4
            verticalSpacing = dp(if (big) 14 else 8)
            stretchMode = GridView.STRETCH_COLUMN_WIDTH
            setPadding(dp(8), 0, dp(8), dp(8)); clipToPadding = false
            selector = round(Color.TRANSPARENT, 0)
        }
        root.addView(grid, LinearLayout.LayoutParams(-1, 0, 1f))

        dock = LinearLayout(this).apply {
            gravity = Gravity.CENTER; background = round(Color.parseColor("#E8DCDD"), 26)
            setPadding(dp(8), dp(8), dp(8), dp(8))
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(dp(14), dp(4), dp(14), dp(14)) }
        }
        root.addView(dock)
        fill()
    }

    private fun filtered(): List<App> {
        val q = search.text.toString().trim().lowercase()
        return if (q.isEmpty()) apps else apps.filter { it.label.lowercase().contains(q) || it.comp.packageName.contains(q) }
    }

    private fun dockList(): List<String> = prefs.getString("dock", "")!!.split("|").filter { it.isNotEmpty() }

    private fun tile(a: App, inDock: Boolean): View {
        val size = dp(if (big) 72 else 52)
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(4), dp(8), dp(4), dp(8))
            background = round(Color.TRANSPARENT, 16)
            addView(ImageView(this@MainActivity).apply { setImageDrawable(a.icon) }, LinearLayout.LayoutParams(size, size))
            if (!inDock) addView(TextView(this@MainActivity).apply {
                text = a.label; setTextColor(INK); gravity = Gravity.CENTER; maxLines = 2
                setTextSize(TypedValue.COMPLEX_UNIT_SP, sp(if (big) 15f else 12f)); setPadding(0, dp(4), 0, 0)
            })
            setOnClickListener { launch(a) }
            setOnLongClickListener { menu(a, this); true }
        }
    }

    private fun fill() {
        val list = filtered()
        grid.adapter = object : BaseAdapter() {
            override fun getCount() = list.size
            override fun getItem(p: Int) = list[p]
            override fun getItemId(p: Int) = p.toLong()
            override fun getView(p: Int, v: View?, parent: ViewGroup?) = tile(list[p], false)
        }
        dock.removeAllViews()
        val d = dockList().mapNotNull { k -> apps.find { it.comp.flattenToString() == k } }
        if (d.isEmpty()) dock.addView(TextView(this).apply {
            text = t("アイコンを長押しして「ドックに追加」", "Long-press an icon → Add to dock")
            setTextColor(INK); setTextSize(TypedValue.COMPLEX_UNIT_SP, sp(13f)); setPadding(dp(8), dp(12), dp(8), dp(12))
        })
        d.forEach { dock.addView(tile(it, true), LinearLayout.LayoutParams(0, -2, 1f)) }
    }

    private fun launch(a: App) {
        (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(search.windowToken, 0)
        try {
            startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setComponent(a.comp)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED))
        } catch (e: Exception) { Toast.makeText(this, t("開けませんでした", "Couldn't open"), Toast.LENGTH_SHORT).show() }
    }

    private fun menu(a: App, anchor: View) {
        val key = a.comp.flattenToString(); val d = dockList().toMutableList(); val docked = key in d
        PopupMenu(this, anchor).apply {
            menu.add(0, 1, 0, if (docked) t("ドックから外す", "Remove from dock") else t("ドックに追加", "Add to dock"))
            menu.add(0, 2, 0, t("アプリ情報", "App info"))
            setOnMenuItemClickListener {
                when (it.itemId) {
                    1 -> {
                        if (docked) d.remove(key) else if (d.size < 5) d.add(key)
                        else Toast.makeText(this@MainActivity, t("ドックは5個まで", "Dock holds 5 apps"), Toast.LENGTH_SHORT).show()
                        prefs.edit().putString("dock", d.joinToString("|")).apply(); fill()
                    }
                    2 -> startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + a.comp.packageName)))
                }
                true
            }
            show()
        }
    }
}
