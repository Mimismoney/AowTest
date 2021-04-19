package com.aowtest.myapplication

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.app.*
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import android.view.KeyEvent
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.res.ResourcesCompat
import java.io.InputStreamReader
import java.util.*

class MyService : AccessibilityService() {

    private var channelId = ""
    private var script: AowScript? = null
    private lateinit var mediaProjection: MediaProjection
    @get:Synchronized
    @set:Synchronized
    var running = false
    private var serviceStarted = false
    private var thread: Thread? = null
    @get:Synchronized
    @set:Synchronized
    var currentActivity: String? = null
    @get:Synchronized
    @set:Synchronized
    var currentPackage: String? = null
    @get:Synchronized
    @set:Synchronized
    var pauseUntil = Calendar.getInstance(TimeZone.getTimeZone("UTC")).time.time
    private var toast: Toast? = null

    override fun onCreate() {
        super.onCreate()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channelId = createChannel()
        }
    }

    override fun onInterrupt() {
        stopService1()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event != null) {
            if (BuildConfig.DEBUG)
                Log.d("EVENT", "Event: $event")
            when (event.eventType) {
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                    try {
                        val packageName = event.packageName?.toString()
                        val className = event.className?.toString()
                        if (packageName != null && className != null) {
                            val componentName = ComponentName(packageName, className)
                            packageManager.getResourcesForActivity(componentName)
                            currentPackage = packageName
                            currentActivity = className
                        }
                    } catch (ex: PackageManager.NameNotFoundException) {
                    }
                }
            }
        }
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            when (intent.action) {
                "START_SERVICE" -> {
                    startService1(intent.getParcelableExtra("mediaProjectionIntent")!!)
                }
                "AFK" -> {
                    afk()
                }
                "STOP" -> {
                    stop()
                }
                "CLOSE" -> {
                    stopService1()
                }
            }
        }
        return Service.START_NOT_STICKY
    }

    private fun showToast(charSequence: CharSequence, duration: Int = Toast.LENGTH_SHORT) {
        toast?.cancel()
        toast = Toast.makeText(this, charSequence, duration).apply { show() }
    }

    private fun startService1(intent: Intent) {
        serviceStarted = true
        showToast("已開啟服務，點選通知欄「掛機」或按下「\uD83D\uDD08音量-」開始動作", Toast.LENGTH_LONG)
        updateService()
        startProjection(intent)
    }

    private fun stopService1() {
        serviceStarted = false
        stop()
        mediaProjection.stop()
        script?.close()
        script = null
        showToast("已關閉服務")
        stopForeground(true)
    }

    @SuppressLint("WrongConstant")
    private fun startProjection(intent: Intent) {
        val mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val display = (getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
        val matrics = DisplayMetrics()
        display.getRealMetrics(matrics)
        mediaProjection = mediaProjectionManager.getMediaProjection(Activity.RESULT_OK, intent)
        val width = matrics.widthPixels
        val height = matrics.heightPixels
        var defaultPointDataJson = ""
        InputStreamReader(assets.open("config.json")).use {
            defaultPointDataJson = getSharedPreferences("pointData", Context.MODE_PRIVATE).getString("pointData", it.readText())!!
        }
        val data = PointDataParser.deserializeJson(defaultPointDataJson, height)
        val imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 3)
        mediaProjection.createVirtualDisplay("ScreenCapture", width, height, matrics.densityDpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, imageReader.surface, null, null)
        val sp = getSharedPreferences("setting", Context.MODE_PRIVATE)
        script = AowScript(this, data, imageReader).apply {
            waitAdSeconds = sp.getFloat(getString(R.string.wait_ad_seconds), ResourcesCompat.getFloat(resources, R.dimen.wait_ad_seconds))
            stuckAdSeconds = sp.getFloat(getString(R.string.stuck_ad_seconds), ResourcesCompat.getFloat(resources, R.dimen.stuck_ad_seconds))
            noAdTimes = sp.getInt(getString(R.string.no_ad_times), resources.getInteger(R.integer.no_ad_times))
            gameStuckSeconds = sp.getFloat(getString(R.string.game_stuck_seconds), ResourcesCompat.getFloat(resources, R.dimen.game_stuck_seconds))
            treasurePeriodSeconds = sp.getFloat(getString(R.string.treasure_period_seconds), ResourcesCompat.getFloat(resources, R.dimen.treasure_period_seconds))
            minSelfSoldiers = sp.getInt(getString(R.string.min_self_soldiers), resources.getInteger(R.integer.min_self_soldiers))
            detectPeriodSeconds = sp.getFloat(getString(R.string.detect_period_seconds), ResourcesCompat.getFloat(resources, R.dimen.detect_period_seconds))
            heroDeadQuit = sp.getBoolean(getString(R.string.hero_dead_quit), resources.getBoolean(R.bool.hero_dead_quit))
        }
    }

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        if (BuildConfig.DEBUG)
            Log.d("KEY", "${event?.keyCode}")
        if (!serviceStarted || event == null || event.keyCode != KeyEvent.KEYCODE_VOLUME_DOWN || event.action != KeyEvent.ACTION_DOWN)
            return super.onKeyEvent(event)
        if (running) {
            stop()
        }
        else {
            afk()
        }
        return false
    }

    private fun stop() {
        running = false
        showToast("腳本已停止")
        thread?.apply {
            interrupt()
            join()
        }
        thread = null
        updateService()
    }

    private fun afk() {
        running = true
        showToast("腳本開始執行")
        updateService()
        thread = Thread(script).apply { start() }
    }

    private fun updateService() {
        val afkStartIntent = Intent(this, MyService::class.java).apply {
            action = "AFK"
        }
        val stopIntent = Intent(this, MyService::class.java).apply {
            action = "STOP"
        }
        val closeServiceIntent = Intent(this, MyService::class.java).apply {
            action = "CLOSE"
        }
        val notification: Notification = NotificationCompat.Builder(this, channelId).also {
            it.setContentTitle("腳本執行服務")
            it.setContentText("點選「掛機」或按下「\uD83D\uDD08音量-」開始動作")
            it.setSmallIcon(R.drawable.notification_icon_background)
            it.priority = NotificationCompat.PRIORITY_DEFAULT
            if (!running) {
                it.addAction(R.drawable.notification_icon_background, "掛機", PendingIntent.getService(this, 0, afkStartIntent, PendingIntent.FLAG_UPDATE_CURRENT))
            }
            else {
                it.addAction(R.drawable.notification_icon_background, "停止", PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT))
            }
            it.addAction(R.drawable.notification_icon_background, "關閉", PendingIntent.getService(this, 0, closeServiceIntent, PendingIntent.FLAG_UPDATE_CURRENT))
        }.build()
        startForeground(1, notification)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createChannel(): String {
        val channelID = "myServiceChannel"
        val channel = NotificationChannel(channelID, "通知", NotificationManager.IMPORTANCE_NONE).apply {
            lightColor = Color.BLUE
            setSound(null, null)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        return channelID
    }
}