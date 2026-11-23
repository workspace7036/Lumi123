package app.lumi.notify

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.widget.RemoteViews

object Notifier {
    private const val CHANNEL = "lumi"
    /** Анимированный шарик в шторке (необязательно): выкл по умолчанию — тогда остаётся один системный значок Люми. */
    fun anim(ctx: Context) = ctx.getSharedPreferences("lumi", Context.MODE_PRIVATE).getBoolean("anim", false)
    fun setAnim(ctx: Context, v: Boolean) { ctx.getSharedPreferences("lumi", Context.MODE_PRIVATE).edit().putBoolean("anim", v).apply() }

    fun show(ctx: Context, id: Int, title: String, text: String) {
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL) == null) {
            nm.createNotificationChannel(NotificationChannel(CHANNEL, "Напоминания Люми", NotificationManager.IMPORTANCE_HIGH))
        }
        val open = ctx.packageManager.getLaunchIntentForPackage("md.obsidian") ?: Intent(ctx, MainActivity::class.java)
        val tap = PendingIntent.getActivity(ctx, id, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val sn = Intent(ctx, SnoozeReceiver::class.java).putExtra("id", id).putExtra("title", title).putExtra("text", text)
        val snPi = PendingIntent.getBroadcast(ctx, id + 1, sn, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val n = Notification.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_lumi)
            .setColor(0xFF3B7CF0.toInt())
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setContentIntent(tap)
            .addAction(Notification.Action.Builder(Icon.createWithResource(ctx, R.drawable.ic_lumi), "Через час", snPi).build())
        if (anim(ctx)) {
            fun view(max: Int): RemoteViews {
                val rv = RemoteViews(ctx.packageName, R.layout.notif_lumi)
                rv.setTextViewText(R.id.t, title); rv.setTextViewText(R.id.x, text)
                rv.setInt(R.id.x, "setMaxLines", max)
                return rv
            }
            n.setStyle(Notification.DecoratedCustomViewStyle()).setCustomContentView(view(2)).setCustomBigContentView(view(10))
        } else {
            n.setStyle(Notification.BigTextStyle().bigText(text))
        }
        nm.notify(id, n.build())
    }
}
