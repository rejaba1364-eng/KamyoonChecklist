package ir.kamyoon.checklist

import android.app.*
import android.content.*
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("truck_checklist", MODE_PRIVATE) }
    private lateinit var content: LinearLayout
    private val sections = linkedMapOf(
        "روزانه" to listOf("بررسی سطح روغن موتور", "بررسی آب رادیاتور", "بررسی روغن ترمز و کلاچ", "بازدید باتری و اتصالات", "کنترل باد و سلامت لاستیک‌ها", "بازدید چراغ‌ها و راهنما", "بررسی شلنگ‌ها و نشتی‌ها", "کنترل ترمز و بوق"),
        "ماهانه" to listOf("بررسی فیلتر هوا", "بازدید تسمه‌ها", "بررسی لنت و دیسک ترمز", "کنترل جلوبندی و فرمان", "بازدید سیستم خنک‌کننده", "بررسی لاستیک زاپاس"),
        "سالانه" to listOf("تمدید بیمه شخص ثالث", "انجام معاینه فنی", "تمدید پروانه فعالیت خودرو", "بازدید کامل سیستم ترمز", "سرویس کامل موتور و گیربکس", "بازدید شاسی و اتاق")
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createChannel()
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 41)
        }
        showHome()
    }
    private fun base(title: String) {
        val scroll = ScrollView(this)
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 22, 20, 28)
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setBackgroundColor(Color.rgb(246,248,250))
        }
        scroll.addView(content)
        setContentView(scroll)
        text(title, 25, Color.rgb(24,55,82), true)
    }
    private fun text(s: String, size: Int = 16, color: Int = Color.DKGRAY, bold: Boolean = false) {
        content.addView(TextView(this).apply {
            text = s; textSize = size.toFloat(); setTextColor(color)
            if (bold) setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(4, 10, 4, 10); gravity = Gravity.RIGHT
        })
    }
    private fun button(label: String, action: () -> Unit) {
        content.addView(Button(this).apply {
            text = label; isAllCaps = false; setOnClickListener { action() }
        })
    }
    private fun showHome() {
        base("چک‌لیست کامیونت")
        text("نگهداری منظم خودرو، سفر ایمن‌تر", 15, Color.GRAY)
        val done = sections.values.flatten().count { prefs.getBoolean("check_$it", false) }
        val total = sections.values.sumOf { it.size }
        text("پیشرفت بازدیدها: $done از $total مورد", 18, Color.rgb(20,110,80), true)
        sections.keys.forEach { name -> button("چک‌لیست $name") { showSection(name) } }
        button("یادآور مدارک خودرو") { showReminders() }
        button("اشتراک‌گذاری گزارش") { shareReport() }
        text("اطلاعات در همین گوشی ذخیره می‌شود.", 13, Color.GRAY)
    }
    private fun showSection(name: String) {
        base("بازدید $name")
        sections[name].orEmpty().forEachIndexed { index, item ->
            val key = "check_$item"
            val cb = CheckBox(this).apply {
                text = item; textSize = 16f; isChecked = prefs.getBoolean(key, false)
                setPadding(4, 8, 4, 8)
                setOnCheckedChangeListener { _, checked -> prefs.edit().putBoolean(key, checked).apply() }
            }
            content.addView(cb)
        }
        val note = EditText(this).apply {
            hint = "یادداشت ایرادها یا توضیحات"; minLines = 2
            setText(prefs.getString("note_$name", ""))
        }
        content.addView(note)
        button("ذخیره یادداشت") { prefs.edit().putString("note_$name", note.text.toString()).apply(); toast("ذخیره شد") }
        button("بازگشت") { showHome() }
    }
    private fun showReminders() {
        base("یادآور مدارک خودرو")
        text("تاریخ را شمسی و به شکل ۱۴۰۵/۰۸/۲۰ وارد کن. یادآوری ۷ روز قبل، ساعت ۹ صبح تنظیم می‌شود.", 14, Color.GRAY)
        val items = listOf("معاینه فنی", "بیمه شخص ثالث", "پروانه فعالیت خودرو")
        items.forEachIndexed { i, label ->
            text(label, 18, Color.rgb(24,55,82), true)
            val input = EditText(this).apply {
                hint = "۱۴۰۵/۰۸/۲۰"; setSingleLine(true)
                setText(prefs.getString("date_$i", ""))
            }
            content.addView(input)
            button("ثبت یادآور $label") {
                val raw = input.text.toString().trim()
                val parsed = parseJalali(raw)
                if (parsed == null) toast("تاریخ را به شکل سال/ماه/روز وارد کن")
                else {
                    prefs.edit().putString("date_$i", raw).apply()
                    schedule(i, label, parsed)
                    toast("یادآور ذخیره شد")
                }
            }
        }
        button("بازگشت") { showHome() }
    }
    private fun shareReport() {
        val total = sections.values.sumOf { it.size }
        val done = sections.values.flatten().count { prefs.getBoolean("check_$it", false) }
        val report = buildString {
            append("گزارش چک‌لیست کامیونت\n")
            sections.forEach { (section, items) ->
                append("\n$section:\n")
                items.forEach { append(if (prefs.getBoolean("check_$it", false)) "☑ " else "☐ "); append(it); append("\n") }
                val note = prefs.getString("note_$section", "")
                if (!note.isNullOrBlank()) append("یادداشت: $note\n")
            }
            append("\nپیشرفت کلی: $done از $total")
            append("\n\nیادآورها:\n")
            listOf("معاینه فنی", "بیمه شخص ثالث", "پروانه فعالیت خودرو").forEachIndexed { i, s ->
                append("$s: ${prefs.getString("date_$i", "ثبت نشده")}\n")
            }
        }
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"; putExtra(Intent.EXTRA_TEXT, report)
        }, "اشتراک‌گذاری گزارش"))
    }
    private fun schedule(id: Int, label: String, due: Calendar) {
        val alert = due.clone() as Calendar
        alert.add(Calendar.DAY_OF_YEAR, -7)
        alert.set(Calendar.HOUR_OF_DAY, 9); alert.set(Calendar.MINUTE, 0); alert.set(Calendar.SECOND, 0)
        val now = Calendar.getInstance()
        if (alert.before(now)) {
            // If the seven-day warning date has passed, notify immediately only when the due date is still ahead.
            if (due.after(now)) alert.timeInMillis = now.timeInMillis + 3000 else return
        }
        val intent = Intent(this, ReminderReceiver::class.java).putExtra("label", label).putExtra("id", id)
        val pi = PendingIntent.getBroadcast(this, 500 + id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val am = getSystemService(ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= 23) am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alert.timeInMillis, pi)
        else am.set(AlarmManager.RTC_WAKEUP, alert.timeInMillis, pi)
    }
    private fun parseJalali(s: String): Calendar? {
        return try {
            val p = s.replace('۰','0').replace('۱','1').replace('۲','2').replace('۳','3').replace('۴','4').replace('۵','5').replace('۶','6').replace('۷','7').replace('۸','8').replace('۹','9').split("/")
            if (p.size != 3) return null
            val jy=p[0].toInt(); val jm=p[1].toInt(); val jd=p[2].toInt()
            if (jm !in 1..12 || jd !in 1..31) return null
            val maxDay = if (jm <= 6) 31 else if (jm <= 11) 30 else 30
            if (jd !in 1..maxDay) return null
            val g = jalaliToGregorian(jy,jm,jd) ?: return null
            Calendar.getInstance().apply { set(g[0], g[1]-1, g[2], 9, 0, 0); set(Calendar.MILLISECOND,0) }
        } catch (_: Exception) { null }
    }
    private fun jalaliToGregorian(jy0: Int, jm: Int, jd: Int): IntArray? {
        var jy=jy0
        val breaks = intArrayOf(-61,9,38,199,426,686,756,818,1111,1181,1210,1635,2060,2097,2192,2262,2324,2394,2456,3178)
        if (jy < breaks.first() || jy >= breaks.last()) return null
        var jp=breaks[0]; var jump=0; var leapJ=-14
        for (i in 1 until breaks.size) {
            val jm0=breaks[i]; jump=jm0-jp
            if (jy < jm0) break
            leapJ += (jump/33)*8 + (jump%33)/4
            jp=jm0
        }
        var n=jy-jp
        leapJ += (n/33)*8 + ((n%33)+3)/4
        if (jump%33==4 && jump-n==4) leapJ++
        val leapG=jy/4 - ((jy/100+1)*3)/4 - 150
        val march=20+leapJ-leapG
        if (jump-n < 6) n=n-jump+((jump+4)/33)*33
        var leap=(((n+1)%33)-1)%4
        if (leap == -1) leap=4
        val gy=jy+621
        val days=if (jm <= 7) (jm-1)*31 + jd-1 else 186+(jm-7)*30 + jd-1
        val gcal=GregorianCalendar(gy, Calendar.MARCH, march)
        gcal.add(Calendar.DAY_OF_YEAR, days)
        return intArrayOf(gcal.get(Calendar.YEAR),gcal.get(Calendar.MONTH)+1,gcal.get(Calendar.DAY_OF_MONTH))
    }
    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val ch=NotificationChannel("vehicle_due", "یادآور مدارک خودرو", NotificationManager.IMPORTANCE_HIGH)
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(ch)
        }
    }
    private fun toast(s: String) = Toast.makeText(this,s,Toast.LENGTH_SHORT).show()
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val label=intent.getStringExtra("label") ?: "مدرک خودرو"
        val notification=if (Build.VERSION.SDK_INT >= 26)
            Notification.Builder(context,"vehicle_due").setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("یادآوری خودرو").setContentText("۷ روز تا سررسید $label باقی مانده است").setAutoCancel(true).build()
        else @Suppress("DEPRECATION") Notification.Builder(context).setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("یادآوری خودرو").setContentText("۷ روز تا سررسید $label باقی مانده است").build()
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(700 + intent.getIntExtra("id",0), notification)
    }
}
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val p=context.getSharedPreferences("truck_checklist", Context.MODE_PRIVATE)
        val names=listOf("معاینه فنی","بیمه شخص ثالث","پروانه فعالیت خودرو")
        names.forEachIndexed { i, name ->
            val raw=p.getString("date_$i", null) ?: return@forEachIndexed
            val parts=raw.replace('۰','0').replace('۱','1').replace('۲','2').replace('۳','3').replace('۴','4').replace('۵','5').replace('۶','6').replace('۷','7').replace('۸','8').replace('۹','9').split("/")
            if (parts.size!=3) return@forEachIndexed
            try {
                val y=parts[0].toInt(); val m=parts[1].toInt(); val d=parts[2].toInt()
                // Rebuild the Jalali date through a small temporary conversion helper.
                val g=jalaliToGregorianBoot(y,m,d) ?: return@forEachIndexed
                val due=Calendar.getInstance().apply { set(g[0],g[1]-1,g[2],9,0,0) }
                val alert=due.clone() as Calendar; alert.add(Calendar.DAY_OF_YEAR,-7)
                if (alert.before(Calendar.getInstance()) && due.after(Calendar.getInstance())) alert.timeInMillis=System.currentTimeMillis()+3000
                if (due.before(Calendar.getInstance())) return@forEachIndexed
                val pi=PendingIntent.getBroadcast(context,500+i,Intent(context,ReminderReceiver::class.java).putExtra("label",name).putExtra("id",i),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                val am=context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                if (Build.VERSION.SDK_INT>=23) am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,alert.timeInMillis,pi) else am.set(AlarmManager.RTC_WAKEUP,alert.timeInMillis,pi)
            } catch (_: Exception) {}
        }
    }
    private fun jalaliToGregorianBoot(jy:Int,jm:Int,jd:Int):IntArray? {
        if(jm !in 1..12 || jd !in 1..31) return null
        val breaks=intArrayOf(-61,9,38,199,426,686,756,818,1111,1181,1210,1635,2060,2097,2192,2262,2324,2394,2456,3178)
        if(jy<breaks.first()||jy>=breaks.last()) return null
        var jp=breaks[0]; var leapJ=-14; var jump=0
        for(i in 1 until breaks.size){ val jm0=breaks[i]; jump=jm0-jp; if(jy<jm0) break; leapJ+=(jump/33)*8+(jump%33)/4; jp=jm0 }
        var n=jy-jp; leapJ+=(n/33)*8+((n%33)+3)/4
        if(jump%33==4 && jump-n==4) leapJ++
        val leapG=jy/4-((jy/100+1)*3)/4-150; val march=20+leapJ-leapG
        if(jump-n<6) n=n-jump+((jump+4)/33)*33
        val days=if(jm<=6)(jm-1)*31+jd-1 else 186+(jm-7)*30+jd-1
        val c=GregorianCalendar(jy+621,Calendar.MARCH,march); c.add(Calendar.DAY_OF_YEAR,days)
        return intArrayOf(c.get(Calendar.YEAR),c.get(Calendar.MONTH)+1,c.get(Calendar.DAY_OF_MONTH))
    }
}
