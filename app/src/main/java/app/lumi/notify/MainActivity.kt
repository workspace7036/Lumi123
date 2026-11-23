package app.lumi.notify

import android.Manifest
import android.app.Activity
import android.app.TimePickerDialog
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Base64
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {
    private var robot: ImageView? = null
    private var summary: TextView? = null
    private var list: LinearLayout? = null
    private val ui = Handler(Looper.getMainLooper())
    private var step = 0
    private val seq = intArrayOf(R.drawable.lumi_f_c, R.drawable.lumi_f_l, R.drawable.lumi_f_l, R.drawable.lumi_f_c,
        R.drawable.lumi_f_r, R.drawable.lumi_f_r, R.drawable.lumi_f_c, R.drawable.lumi_f_b)
    private val ticker = object : Runnable {
        override fun run() { robot?.setImageResource(seq[step++ % seq.size]); ui.postDelayed(this, 420) }
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        if (handle(intent)) { finish(); return }
        buildUi()
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }

    override fun onNewIntent(i: Intent) {
        super.onNewIntent(i)
        setIntent(i)
        if (handle(i)) finish() else refresh()
    }

    override fun onResume() { super.onResume(); refresh(); ui.post(ticker) }
    override fun onPause() { super.onPause(); ui.removeCallbacks(ticker) }

    /** lumi://schedule?d=<base64url(json)> от плагина Obsidian: ставит расписание и закрывается — ты возвращаешься в Obsidian. Без d — просто открывает приложение. */
    private fun handle(i: Intent?): Boolean {
        val u = i?.data ?: return false
        if (u.scheme != "lumi") return false
        val d = u.getQueryParameter("d") ?: return false
        try {
            val json = String(Base64.decode(d, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING), Charsets.UTF_8)
            val n = Scheduler.replace(this, json)
            Toast.makeText(this, "Люми: напоминаний запланировано — $n", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Люми: не получилось разобрать расписание", Toast.LENGTH_LONG).show()
        }
        return true
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun pill(color: Int, r: Int = 14): GradientDrawable {
        val g = GradientDrawable(); g.cornerRadius = dp(r).toFloat(); g.setColor(color); return g
    }

    private fun btn(t: String, main: Boolean = true, click: () -> Unit): Button {
        val b = Button(this)
        b.text = t; b.isAllCaps = false; b.setTextColor(Color.WHITE); b.textSize = 15f
        b.background = pill(if (main) 0xFF3B7CF0.toInt() else 0xFF2A2A2E.toInt())
        val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(50)); lp.topMargin = dp(10)
        b.layoutParams = lp
        b.setOnClickListener { click() }
        return b
    }

    private fun label(t: String): TextView {
        val v = TextView(this); v.text = t; v.setTextColor(0xFF8A8A92.toInt()); v.textSize = 13f
        v.setPadding(dp(4), dp(22), 0, dp(6)); return v
    }

    private fun buildUi() {
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(0xFF151515.toInt()); root.setPadding(dp(20), dp(56), dp(20), dp(32))

        val head = LinearLayout(this); head.orientation = LinearLayout.HORIZONTAL; head.gravity = Gravity.CENTER_VERTICAL
        val img = ImageView(this); img.setImageResource(R.drawable.lumi_f_c); robot = img
        head.addView(img, LinearLayout.LayoutParams(dp(72), dp(72)))
        val box = LinearLayout(this); box.orientation = LinearLayout.VERTICAL; box.setPadding(dp(16), 0, 0, 0)
        val title = TextView(this); title.text = "Люми"; title.setTextColor(Color.WHITE); title.textSize = 26f
        val sm = TextView(this); sm.setTextColor(0xFFA0A0A8.toInt()); sm.textSize = 14f; summary = sm
        box.addView(title); box.addView(sm); head.addView(box)
        root.addView(head)

        root.addView(label("БЛИЖАЙШИЕ НАПОМИНАНИЯ"))
        val l = LinearLayout(this); l.orientation = LinearLayout.VERTICAL; list = l; root.addView(l)

        root.addView(label("ДЕЙСТВИЯ"))
        root.addView(btn("Открыть Obsidian") {
            val i = packageManager.getLaunchIntentForPackage("md.obsidian")
            if (i != null) startActivity(i) else Toast.makeText(this, "Obsidian не найден", Toast.LENGTH_SHORT).show()
        })
        root.addView(btn("Тест через 5 секунд", false) {
            Scheduler.addOne(this, Scheduler.Item(System.currentTimeMillis() + 5000, "Люми",
                "Привет! Так будут выглядеть мои напоминания. На этой неделе ты отстаёшь по тренировкам — сегодня есть отличный повод это исправить.", true))
            Toast.makeText(this, "Сверни приложение — через 5 секунд придёт тест", Toast.LENGTH_SHORT).show()
            ui.postDelayed({ refresh() }, 300)
        })
        root.addView(btn("Очистить все напоминания", false) { Scheduler.clear(this); refresh() })

        root.addView(label("ТИХИЕ ЧАСЫ И ЧАСТОТА"))
        fun line(getText: () -> String, onClick: (Button) -> Unit): Button {
            val b = btn("", false) {}
            b.text = getText()
            b.setOnClickListener { onClick(b); b.text = getText(); LumiWidget.refresh(this) }
            return b
        }
        root.addView(line({ "Тихие часы: " + if (Prefs.quietOn(this)) "вкл" else "выкл" }) { Prefs.setQuietOn(this, !Prefs.quietOn(this)) })
        fun timeBtn(title: String, get: () -> Int, set: (Int) -> Unit): Button {
            val b = btn("", false) {}
            b.text = title + ": " + Prefs.fmt(get())
            b.setOnClickListener {
                TimePickerDialog(this, { _, h, m -> set(h * 60 + m); b.text = title + ": " + Prefs.fmt(get()) }, get() / 60, get() % 60, true).show()
            }
            return b
        }
        root.addView(timeBtn("Тихо с", { Prefs.quietFrom(this) }) { Prefs.setQuiet(this, it, null) })
        root.addView(timeBtn("Тихо до", { Prefs.quietTo(this) }) { Prefs.setQuiet(this, null, it) })
        val caps = intArrayOf(0, 1, 2, 3, 5, 8)
        root.addView(line({ "Не больше в день: " + (if (Prefs.cap(this) == 0) "без лимита" else Prefs.cap(this).toString()) }) {
            val i = caps.indexOf(Prefs.cap(this)); Prefs.setCap(this, caps[(i + 1) % caps.size])
        })
        root.addView(line({
            val t = Prefs.pausedUntil(this)
            if (t > System.currentTimeMillis()) "Пауза до " + SimpleDateFormat("dd.MM HH:mm", Locale.getDefault()).format(Date(t)) + " (снять)" else "Пауза до завтра"
        }) {
            val now = System.currentTimeMillis()
            Prefs.setPausedUntil(this, if (Prefs.pausedUntil(this) > now) 0L else Prefs.endOfToday(now))
        })
        root.addView(btn("Добавить виджет на экран", false) {
            val m = getSystemService(AppWidgetManager::class.java)
            if (Build.VERSION.SDK_INT >= 26 && m != null && m.isRequestPinAppWidgetSupported) {
                m.requestPinAppWidget(ComponentName(this, LumiWidget::class.java), null, null)
            } else {
                Toast.makeText(this, "Удерживай палец на главном экране → Виджеты → Люми", Toast.LENGTH_LONG).show()
            }
        })

        root.addView(label("НАСТРОЙКИ"))
        val animBtn = btn("", false) {}
        fun animLabel() = "Анимированный шарик в шторке: " + if (Notifier.anim(this)) "вкл" else "выкл"
        animBtn.text = animLabel()
        animBtn.setOnClickListener { Notifier.setAnim(this, !Notifier.anim(this)); animBtn.text = animLabel() }
        root.addView(animBtn)
        root.addView(btn("Разрешения уведомлений", false) {
            if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
            } else {
                startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
            }
        })
        root.addView(btn("Автозапуск и батарея", false) {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
        })

        val hint = TextView(this)
        hint.text = "Напоминания пишет ИИ в плагине Obsidian по отстающим пунктам недели и передаёт сюда. Дальше телефон показывает их сам, даже при закрытом Obsidian. ИИ в чате Obsidian может сам ставить напоминания и менять тихие часы, лимит и паузу."
        hint.setTextColor(0xFF6E6E76.toInt()); hint.textSize = 12f; hint.setPadding(dp(4), dp(24), dp(4), 0)
        root.addView(hint)

        val sv = ScrollView(this); sv.setBackgroundColor(0xFF151515.toInt()); sv.addView(root)
        setContentView(sv)
    }

    private fun refresh() {
        val l = list ?: return
        val p = Scheduler.pending(this)
        val df = SimpleDateFormat("dd.MM HH:mm", Locale.getDefault())
        summary?.text = if (p.isEmpty()) "Ничего не запланировано" else "Запланировано: ${p.size} · ближайшее ${df.format(Date(p[0].t))}"
        l.removeAllViews()
        if (p.isEmpty()) {
            val e = TextView(this); e.text = "Открой Obsidian — плагин сам пришлёт расписание."
            e.setTextColor(0xFF6E6E76.toInt()); e.textSize = 14f; e.setPadding(dp(4), 0, 0, 0); l.addView(e); return
        }
        for (item in p.take(20)) {
            val row = LinearLayout(this); row.orientation = LinearLayout.HORIZONTAL; row.gravity = Gravity.CENTER_VERTICAL
            row.background = pill(0xFF202024.toInt(), 14); row.setPadding(dp(14), dp(10), dp(6), dp(10))
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT); lp.bottomMargin = dp(8)
            row.layoutParams = lp
            val tx = TextView(this)
            tx.text = df.format(Date(item.t)) + "\n" + item.text
            tx.setTextColor(Color.WHITE); tx.textSize = 14f; tx.maxLines = 3
            row.addView(tx, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            val x = Button(this); x.text = "✕"; x.setTextColor(0xFFB0B0B8.toInt()); x.background = null
            x.setOnClickListener { Scheduler.remove(this, item); refresh() }
            row.addView(x, LinearLayout.LayoutParams(dp(48), dp(48)))
            l.addView(row)
        }
    }
}
