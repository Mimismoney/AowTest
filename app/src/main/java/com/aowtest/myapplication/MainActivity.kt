package com.aowtest.myapplication

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Activity
import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.Editable
import android.util.DisplayMetrics
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import java.io.File

class MainActivity : Activity() {

    private lateinit var waitAdSecondsText: EditText
    private lateinit var stuckAdSecondsText: EditText
    private lateinit var noAdTimesText: EditText
    private lateinit var gameStuckSecondsText: EditText
    private lateinit var treasurePeriodSecondsText: EditText
    private lateinit var minSelfSoldiersText: EditText
    private lateinit var detectPeriodSecondsText: EditText
    private lateinit var heroDeadQuitCheck: CheckBox
    private lateinit var finishQuitGameCheck: CheckBox
    private lateinit var windowPauseScriptCheck: CheckBox
    private lateinit var crashTestButton: Button

    companion object {
        private const val REQUEST_SCREEN_CAP = 1
        private var mediaProjectionIntent: Intent? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        onIntent(intent)
        setContentView(R.layout.activity_main)
        waitAdSecondsText = findViewById(R.id.wait_ad_seconds)
        stuckAdSecondsText = findViewById(R.id.stuck_ad_seconds)
        noAdTimesText = findViewById(R.id.no_ad_times)
        gameStuckSecondsText = findViewById(R.id.game_stuck_seconds)
        treasurePeriodSecondsText = findViewById(R.id.treasure_period_seconds)
        minSelfSoldiersText = findViewById(R.id.min_self_soldiers)
        detectPeriodSecondsText = findViewById(R.id.detect_period_seconds)
        heroDeadQuitCheck = findViewById(R.id.hero_dead_quit)
        finishQuitGameCheck = findViewById(R.id.finish_quit_game)
        windowPauseScriptCheck = findViewById(R.id.window_pause_script)
        crashTestButton = findViewById(R.id.crash_test)
        val data = getSharedPreferences("setting", Context.MODE_PRIVATE)
        waitAdSecondsText.text = data.getFloat(getString(R.string.wait_ad_seconds), ResourcesCompat.getFloat(resources, R.dimen.wait_ad_seconds)).toString().toEditable()
        stuckAdSecondsText.text = data.getFloat(getString(R.string.stuck_ad_seconds), ResourcesCompat.getFloat(resources, R.dimen.stuck_ad_seconds)).toString().toEditable()
        noAdTimesText.text = data.getInt(getString(R.string.no_ad_times), resources.getInteger(R.integer.no_ad_times)).toString().toEditable()
        gameStuckSecondsText.text = data.getFloat(getString(R.string.game_stuck_seconds), ResourcesCompat.getFloat(resources, R.dimen.game_stuck_seconds)).toString().toEditable()
        treasurePeriodSecondsText.text = data.getFloat(getString(R.string.treasure_period_seconds), ResourcesCompat.getFloat(resources, R.dimen.treasure_period_seconds)).toString().toEditable()
        minSelfSoldiersText.text = data.getInt(getString(R.string.min_self_soldiers), resources.getInteger(R.integer.min_self_soldiers)).toString().toEditable()
        detectPeriodSecondsText.text = data.getFloat(getString(R.string.detect_period_seconds), ResourcesCompat.getFloat(resources, R.dimen.detect_period_seconds)).toString().toEditable()
        heroDeadQuitCheck.isChecked = data.getBoolean(getString(R.string.hero_dead_quit), resources.getBoolean(R.bool.hero_dead_quit))
        finishQuitGameCheck.isChecked = data.getBoolean(getString(R.string.finish_quit_game), resources.getBoolean(R.bool.finish_quit_game))
        windowPauseScriptCheck.isChecked = data.getBoolean(getString(R.string.window_pause_script), resources.getBoolean(R.bool.window_pause_script))
        if (!BuildConfig.DEBUG) {
            crashTestButton.visibility = View.GONE
        }
        val cacheDir = cacheDir
        val tessdataDir = File(cacheDir.absolutePath + File.separator + "tessdata")
        if (!tessdataDir.exists()) {
            tessdataDir.mkdirs()
        }
        val aowTrainDataPath = File(tessdataDir.absolutePath + File.separator + "digits.traineddata")
        if (!aowTrainDataPath.exists()) {
            assets.open("digits.traineddata").copyTo(aowTrainDataPath.outputStream())
        }
        return super.onCreate(savedInstanceState)
    }
    
    private fun String.toEditable(): Editable = Editable.Factory.getInstance().newEditable(this)

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        onIntent(intent)
    }

    private fun onIntent(intent: Intent?) {
        when (intent?.action) {
            "RESTART" -> {
                Looper.myLooper()?.let {
                    Handler(it).postDelayed({
                        (getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).killBackgroundProcesses("com.addictive.strategy.army")
                        Handler(it).postDelayed({
                            applicationContext.startActivity(Intent().apply {
                                component = ComponentName("com.addictive.strategy.army", "com.addictive.strategy.army.UnityPlayerActivity")
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            })
                        }, 1000)
                    }, 1000)
                }
            }
            "QUIT" -> {
                Looper.myLooper()?.let {
                    Handler(it).postDelayed({
                        (getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).killBackgroundProcesses("com.addictive.strategy.army")
                        finish()
                    }, 3000)
                }
            }
        }
    }


    fun onStartServiceClick(@Suppress("UNUSED_PARAMETER") view: View) {
        try {
            getSharedPreferences("setting", Context.MODE_PRIVATE).edit()
                .putFloat(
                    getString(R.string.wait_ad_seconds),
                    waitAdSecondsText.text.toString().toFloat()
                )
                .putFloat(
                    getString(R.string.stuck_ad_seconds),
                    stuckAdSecondsText.text.toString().toFloat()
                )
                .putInt(getString(R.string.no_ad_times), noAdTimesText.text.toString().toInt())
                .putFloat(
                    getString(R.string.game_stuck_seconds),
                    gameStuckSecondsText.text.toString().toFloat()
                )
                .putFloat(
                    getString(R.string.treasure_period_seconds),
                    treasurePeriodSecondsText.text.toString().toFloat()
                )
                .putInt(
                    getString(R.string.min_self_soldiers),
                    minSelfSoldiersText.text.toString().toInt()
                )
                .putFloat(
                    getString(R.string.detect_period_seconds),
                    detectPeriodSecondsText.text.toString().toFloat()
                )
                .putBoolean(
                    getString(R.string.hero_dead_quit),
                    heroDeadQuitCheck.isChecked
                )
                .putBoolean(
                    getString(R.string.finish_quit_game),
                    finishQuitGameCheck.isChecked
                ).putBoolean(getString(R.string.window_pause_script), windowPauseScriptCheck.isChecked)
                .apply()
        } catch (ex: NumberFormatException) {
            ToastUtil.showToast(this, "數值輸入錯誤", Toast.LENGTH_SHORT)
            return
        }
        @Suppress("DEPRECATION")
        val display = (getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
        val metrics = DisplayMetrics()
        display.getRealMetrics(metrics)
        val accessibilityManager = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val serviceInfo = accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC).find {
            val enabledServiceInfo = it.resolveInfo.serviceInfo
            enabledServiceInfo.packageName == packageName && enabledServiceInfo.name == MyService::class.java.name
        }
        if (serviceInfo == null) {
            ToastUtil.showToast(this, "請開啟無障礙設定", Toast.LENGTH_SHORT)
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
            return
        }
        if (mediaProjectionIntent == null) {
            val projectionManager =
                getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            val intent = projectionManager.createScreenCaptureIntent()
            startActivityForResult(intent, REQUEST_SCREEN_CAP)
        }
        else {
            startAFKService()
        }
    }

    private fun startAFKService() {
        val serviceIntent = Intent(this, MyService::class.java).apply {
            action = "START_SERVICE"
        }
        serviceIntent.putExtra("mediaProjectionIntent", mediaProjectionIntent)
        ContextCompat.startForegroundService(this, serviceIntent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        when (requestCode) {
            REQUEST_SCREEN_CAP -> {
                if (resultCode == RESULT_OK) {
                    mediaProjectionIntent = intent
                    startAFKService()
                } else {
                    mediaProjectionIntent = null
                    ToastUtil.showToast(this, "無法取得螢幕權限", Toast.LENGTH_SHORT)
                }
            }
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun onCrashClick(view: View) {
        throw java.lang.Exception("Test firebase")
    }
}