package app.lumi.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import org.json.JSONArray
import org.json.JSONObject

/** Расписание напоминаний: хранится в SharedPreferences, ставится через AlarmManager (работает при закрытом Obsidian). */
object Scheduler {
    private const val PREFS = "lumi"
    private const val KEY = "items"

    data class Item(val t: Long, val title: String, val text: String, val force: Boolean = false) {
        val id: Int get() = (t.toString() + title + text).hashCode()
    }

    fun parse(json: String): List<Item> {
        val arr = JSONObject(json).getJSONArray("items")
        val out = ArrayList<Item>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out.add(Item(o.getLong("t"), o.optString("title", "Люми"), o.optString("text", ""), o.optInt("f", 0) == 1))
        }
        return out
    }

    private fun load(ctx: Context): List<Item> {
        val s = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null) ?: return emptyList()
        return try { parse("{\"items\":$s}") } catch (e: Exception) { emptyList() }
    }

    private fun save(ctx: Context, items: List<Item>) {
        val a = JSONArray()
        for (it in items) a.put(JSONObject().put("t", it.t).put("title", it.title).put("text", it.text).put("f", if (it.force) 1 else 0))
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, a.toString()).apply()
        LumiWidget.refresh(ctx)
    }

    private fun pi(ctx: Context, it: Item): PendingIntent {
        val i = Intent(ctx, AlarmReceiver::class.java)
            .putExtra("id", it.id).putExtra("title", it.title).putExtra("text", it.text).putExtra("f", it.force)
        return PendingIntent.getBroadcast(ctx, it.id, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun set(ctx: Context, it: Item) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        try {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, it.t, pi(ctx, it))
        } catch (e: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, it.t, pi(ctx, it))
        }
    }

    private fun cancelAll(ctx: Context) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        for (it in load(ctx)) am.cancel(pi(ctx, it))
    }

    /** Заменяет всё расписание новым (старые будильники отменяются). Возвращает, сколько напоминаний поставлено. */
    fun replace(ctx: Context, json: String): Int {
        val now = System.currentTimeMillis()
        val root = JSONObject(json)
        Prefs.apply(ctx, root)
        if (!root.has("items")) { LumiWidget.refresh(ctx); return pending(ctx).size }   // команда только с настройками — расписание не трогаем
        val items = parse(json).filter { it.t > now }.sortedBy { it.t }.take(60)
        cancelAll(ctx)
        save(ctx, items)
        for (it in items) set(ctx, it)
        return items.size
    }

    /** После перезагрузки телефона будильники сбрасываются — ставим заново. */
    fun reschedule(ctx: Context) {
        val now = System.currentTimeMillis()
        val items = load(ctx).filter { it.t > now }
        save(ctx, items)
        for (it in items) set(ctx, it)
    }

    fun pending(ctx: Context): List<Item> {
        val now = System.currentTimeMillis()
        return load(ctx).filter { it.t > now }.sortedBy { it.t }
    }

    fun addOne(ctx: Context, it: Item) {
        val items = load(ctx) + it
        save(ctx, items)
        set(ctx, it)
    }

    fun remove(ctx: Context, item: Item) {
        (ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(pi(ctx, item))
        save(ctx, load(ctx).filter { it != item })
    }

    fun clear(ctx: Context) {
        cancelAll(ctx)
        save(ctx, emptyList())
    }
}
