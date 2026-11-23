package app.lumi.notify

import android.app.Activity
import android.os.Bundle
import android.util.Base64
import android.widget.Toast

/** Прозрачная «прокладка» для lumi://schedule: принимает расписание от плагина и сразу закрывается, экран приложения не показывается. */
class ScheduleActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        try {
            val d = intent?.data?.getQueryParameter("d")
            if (d != null) {
                val json = String(Base64.decode(d, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING), Charsets.UTF_8)
                val n = Scheduler.replace(this, json)
                Toast.makeText(this, "Люми: напоминаний запланировано — $n", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Люми: не получилось разобрать расписание", Toast.LENGTH_LONG).show()
        }
        finish()
        overridePendingTransition(0, 0)
    }
}
