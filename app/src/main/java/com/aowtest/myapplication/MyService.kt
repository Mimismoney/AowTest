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
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.Looper
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
import kotlin.IllegalStateException

class MyService : AccessibilityService() {

    private var channelId = ""
    private var mediaProjection: MediaProjection? = null
    private var running = false
    private var displayMetrics = DisplayMetrics()

    var currentActivity: String? = null
    var currentPackage: String? = null
    var pauseUntil = Calendar.getInstance(TimeZone.getTimeZone("UTC")).time.time

    private var handler = Looper.myLooper()?.let { Handler(it) }
    private var task = ScriptRunner()
    private var imageReader: ImageReader? = null
    private var script: AowScript? = null
    private var virtualDisplay: VirtualDisplay? = null

    companion object {
        private var mediaProjectionIntent: Intent? = null
    }

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
                    intent.getParcelableExtra<Intent>("mediaProjectionIntent")?.let { startService1(it) }
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
        ToastUtil.showToast(applicationContext, charSequence, duration)
    }

    @SuppressLint("WrongConstant")
    private fun startService1(intent: Intent) {
        mediaProjectionIntent = intent

        @Suppress("DEPRECATION") val display = (getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
        display.getRealMetrics(displayMetrics)
        val width = displayMetrics.widthPixels
        val height = displayMetrics.heightPixels
        val imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 3).also { imageReader = it }
        val data = PointDataParser.deserializeJson(InputStreamReader(assets.open("config.json")).use { it.readText() }, width, height)
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
            finishQuitGame = sp.getBoolean(getString(R.string.finish_quit_game), resources.getBoolean(R.bool.finish_quit_game))
        }

        showToast("已開啟服務，點選通知欄「掛機」或按下「\uD83D\uDD08音量-」開始動作", Toast.LENGTH_LONG)
        updateService()
    }

    private fun stopService1() {
        stop()
        script?.close()
        script = null
        imageReader?.close()
        imageReader = null
        showToast("已關閉服務")
        stopForeground(true)
    }

    @SuppressLint("WrongConstant")
    fun startProjection() {
        if (mediaProjection != null) return
        val mediaProjectionIntent = mediaProjectionIntent ?: throw IllegalStateException("MediaProjectionIntent should not be null in here")
        val mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = mediaProjectionManager.getMediaProjection(Activity.RESULT_OK, mediaProjectionIntent)
        virtualDisplay = mediaProjection?.createVirtualDisplay("ScreenCapture", displayMetrics.widthPixels, displayMetrics.heightPixels, displayMetrics.densityDpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, imageReader?.surface, null, null)
    }

    fun stopProjection() {
        virtualDisplay?.release()
        virtualDisplay = null
        mediaProjection?.stop()
        mediaProjection = null
    }

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        if (BuildConfig.DEBUG)
            Log.d("KEY", "${event?.keyCode}")
        if (imageReader != null && event != null && event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN && event.action == KeyEvent.ACTION_DOWN)
            if (running) stop() else afk()
        return false
    }

    fun stop() {
        running = false
        handler?.removeCallbacks(task)
        stopProjection()
        showToast("腳本已停止")
        updateService()
    }

    private fun afk() {
        if (mediaProjectionIntent == null) {
            startActivity(Intent(this, MainActivity::class.java))
            stopService1()
            showToast("無法取得螢幕權限，請重新開啟服務")
            return
        }
        running = true
        script?.init()
        task.run()
        showToast("腳本開始執行")
        updateService()
    }

    private fun updateService() {
        NotificationCompat.Builder(this, channelId).also {
            val closeServiceIntent = Intent(this, MyService::class.java).apply {
                action = "CLOSE"
            }
            it.setContentTitle("腳本執行服務")
            it.setContentText("點選「掛機」或按下「\uD83D\uDD08音量-」開始動作")
            it.setSmallIcon(R.drawable.notification_icon_background)
            it.priority = NotificationCompat.PRIORITY_DEFAULT
            if (!running) {
                val afkStartIntent = Intent(this, MyService::class.java).apply {
                    action = "AFK"
                }
                it.addAction(R.drawable.notification_icon_background, "掛機", PendingIntent.getService(this, 0, afkStartIntent, PendingIntent.FLAG_UPDATE_CURRENT))
            }
            else {
                val stopIntent = Intent(this, MyService::class.java).apply {
                    action = "STOP"
                }
                it.addAction(R.drawable.notification_icon_background, "停止", PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT))
            }
            it.addAction(R.drawable.notification_icon_background, "關閉", PendingIntent.getService(this, 0, closeServiceIntent, PendingIntent.FLAG_UPDATE_CURRENT))
        }.build().also {
            startForeground(1, it)
        }
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

    private inner class ScriptRunner : Runnable {
        override fun run() {
            script?.also {script->
                script.tick()
                handler?.postDelayed(this, (script.detectPeriodSeconds * 1000).toLong())
            }
        }
    }
}