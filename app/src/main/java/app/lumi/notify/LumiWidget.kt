package app.lumi.notify

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Виджет на главный экран: Люми и ближайшее напоминание. */
class LumiWidget : AppWidgetProvider() {
    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        for (id in ids) mgr.updateAppWidget(id, build(ctx))
    }

    companion object {
        fun build(ctx: Context): RemoteViews {
            val rv = RemoteViews(ctx.packageName, R.layout.widget_lumi)
            val now = System.currentTimeMillis()
            val p = Scheduler.pending(ctx)
            val df = SimpleDateFormat("dd.MM HH:mm", Locale.getDefault())
            val paused = Prefs.pausedUntil(ctx) > now
            if (p.isEmpty()) {
                rv.setTextViewText(R.id.w_time, if (paused) "Пауза" else "Люми")
                rv.setTextViewText(R.id.w_text, "Ничего не запланировано")
            } else {
                rv.setTextViewText(R.id.w_time, (if (paused) "Пауза · " else "") + "Дальше: " + df.format(Date(p[0].t)) + " · всего " + p.size)
                rv.setTextViewText(R.id.w_text, p[0].text.ifBlank { p[0].title })
            }
            val open = PendingIntent.getActivity(ctx, 0, Intent(ctx, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            rv.setOnClickPendingIntent(R.id.w_root, open)
            return rv
        }

        fun refresh(ctx: Context) {
            try {
                val m = AppWidgetManager.getInstance(ctx)
                val ids = m.getAppWidgetIds(ComponentName(ctx, LumiWidget::class.java))
                for (id in ids) m.updateAppWidget(id, build(ctx))
            } catch (e: Exception) { }
        }
    }
}
