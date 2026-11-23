package app.lumi.notify

import android.content.Context
import org.json.JSONObject
import java.util.Calendar

/** Настройки приложения: тихие часы, лимит в день, пауза. Можно менять в приложении или командой от плагина (ИИ в Obsidian). */
object Prefs {
    private fun p(ctx: Context) = ctx.getSharedPreferences("lumi", Context.MODE_PRIVATE)

    fun quietOn(ctx: Context) = p(ctx).getBoolean("q_on", false)
    fun setQuietOn(ctx: Context, v: Boolean) { p(ctx).edit().putBoolean("q_on", v).apply() }
    fun quietFrom(ctx: Context) = p(ctx).getInt("q_from", 23 * 60)
    fun quietTo(ctx: Context) = p(ctx).getInt("q_to", 8 * 60)
    fun setQuiet(ctx: Context, from: Int?, to: Int?) {
        val e = p(ctx).edit()
        if (from != null) e.putInt("q_from", from)
        if (to != null) e.putInt("q_to", to)
        e.apply()
    }
    fun cap(ctx: Context) = p(ctx).getInt("cap", 0)
    fun setCap(ctx: Context, v: Int) { p(ctx).edit().putInt("cap", v).apply() }
    fun pausedUntil(ctx: Context) = p(ctx).getLong("paused", 0L)
    fun setPausedUntil(ctx: Context, t: Long) { p(ctx).edit().putLong("paused", t).apply() }

    fun fmt(min: Int) = String.format("%02d:%02d", min / 60, min % 60)
    private fun parseHm(s: String): Int? {
        val m = Regex("^(\\d{1,2}):(\\d{2})$").find(s.trim()) ?: return null
        val h = m.groupValues[1].toInt(); val mi = m.groupValues[2].toInt()
        return if (h in 0..23 && mi in 0..59) h * 60 + mi else null
    }

    private fun minutesOfDay(t: Long): Int {
        val c = Calendar.getInstance(); c.timeInMillis = t
        return c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE)
    }

    fun inQuiet(ctx: Context, t: Long): Boolean {
        if (!quietOn(ctx)) return false
        val f = quietFrom(ctx); val to = quietTo(ctx); val m = minutesOfDay(t)
        if (f == to) return false
        return if (f < to) m in f until to else (m >= f || m < to)
    }

    /** Ближайший конец тихих часов после момента t. */
    fun quietEnd(ctx: Context, t: Long): Long {
        val to = quietTo(ctx)
        val c = Calendar.getInstance(); c.timeInMillis = t
        c.set(Calendar.HOUR_OF_DAY, to / 60); c.set(Calendar.MINUTE, to % 60); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
        if (c.timeInMillis <= t) c.add(Calendar.DAY_OF_MONTH, 1)
        return c.timeInMillis
    }

    private fun dayKey(t: Long): String {
        val c = Calendar.getInstance(); c.timeInMillis = t
        return "" + c.get(Calendar.YEAR) + "-" + c.get(Calendar.DAY_OF_YEAR)
    }

    fun shownToday(ctx: Context, now: Long): Int {
        val sp = p(ctx)
        return if (sp.getString("shown_day", "") == dayKey(now)) sp.getInt("shown_n", 0) else 0
    }
    fun countShown(ctx: Context, now: Long) {
        p(ctx).edit().putString("shown_day", dayKey(now)).putInt("shown_n", shownToday(ctx, now) + 1).apply()
    }

    fun endOfToday(now: Long): Long {
        val c = Calendar.getInstance(); c.timeInMillis = now
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
        c.add(Calendar.DAY_OF_MONTH, 1)
        return c.timeInMillis
    }

    /** Настройки из команды плагина: quiet_on, quiet_from, quiet_to, cap, pause_hours. */
    fun apply(ctx: Context, o: JSONObject) {
        if (o.has("quiet_on")) setQuietOn(ctx, o.optInt("quiet_on", 0) == 1)
        val f = if (o.has("quiet_from")) parseHm(o.optString("quiet_from")) else null
        val t = if (o.has("quiet_to")) parseHm(o.optString("quiet_to")) else null
        if (f != null || t != null) setQuiet(ctx, f, t)
        if (o.has("cap")) setCap(ctx, o.optInt("cap", 0).coerceIn(0, 20))
        if (o.has("pause_hours")) {
            val h = o.optDouble("pause_hours", 0.0)
            setPausedUntil(ctx, if (h > 0) System.currentTimeMillis() + (h * 3600000).toLong() else 0L)
        }
    }
}
