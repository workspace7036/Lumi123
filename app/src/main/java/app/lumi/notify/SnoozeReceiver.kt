package app.lumi.notify

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Кнопка «Через час» в уведомлении: убирает его и ставит такое же ещё раз через час. */
class SnoozeReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, i: Intent) {
        val id = i.getIntExtra("id", 0)
        (ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(id)
        Scheduler.addOne(ctx, Scheduler.Item(System.currentTimeMillis() + 3600000L, i.getStringExtra("title") ?: "Люми", i.getStringExtra("text") ?: ""))
    }
}
