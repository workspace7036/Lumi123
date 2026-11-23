package app.lumi.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlin.math.abs

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, i: Intent) {
        val id = i.getIntExtra("id", 1)
        val title = i.getStringExtra("title") ?: "Люми"
        val text = i.getStringExtra("text") ?: ""
        val force = i.getBooleanExtra("f", false)
        val now = System.currentTimeMillis()
        if (!force) {
            if (Prefs.pausedUntil(ctx) > now) { LumiWidget.refresh(ctx); return }   // пауза: напоминание пропускается
            if (Prefs.inQuiet(ctx, now)) {   // тихие часы: переносим на конец тихих часов (с небольшим разбросом, чтобы не пришли пачкой)
                Scheduler.addOne(ctx, Scheduler.Item(Prefs.quietEnd(ctx, now) + (abs(id) % 600) * 1000L, title, text))
                return
            }
            val cap = Prefs.cap(ctx)
            if (cap > 0 && Prefs.shownToday(ctx, now) >= cap) { LumiWidget.refresh(ctx); return }   // лимит в день
        }
        Notifier.show(ctx, id, title, text)
        if (!force) Prefs.countShown(ctx, now)
        LumiWidget.refresh(ctx)
    }
}
