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
import android.util.Log
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
import java.io.InputStreamReader

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
    private lateinit var crashTestButton: Button
    private val REQUEST_SCREEN_CAP = 1
    private val REQUEST_CHOOSE_JSON = 2

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
        if (!BuildConfig.DEBUG) {
            crashTestButton.visibility = View.GONE
        }
        val cacheDir = cacheDir
        val tessdataDir = File(cacheDir.absolutePath + File.separator + "tessdata")
        if (!tessdataDir.exists()) {
            tessdataDir.mkdirs()
        }
        val aowTrainDataPath = File(tessdataDir.absolutePath + File.separator + "aow.traineddata")
        if (!aowTrainDataPath.exists()) {
            assets.open("aow.traineddata").copyTo(aowTrainDataPath.outputStream())
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
                        applicationContext.startActivity(Intent().apply {
                            component = ComponentName("com.addictive.strategy.army", "com.addictive.strategy.army.UnityPlayerActivity")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        })
                    }, 3000)
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

    fun onStartServiceClick(view: View) {
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
                )
                .apply()
        } catch (ex: NumberFormatException) {
            Toast.makeText(this, "數值輸入錯誤", Toast.LENGTH_SHORT).show()
            return
        }
        val display = (getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
        val matrics = DisplayMetrics()
        display.getRealMetrics(matrics)
        var pointDataJson = ""
        InputStreamReader(assets.open("config.json")).use {
            pointDataJson = getSharedPreferences("pointData", Context.MODE_PRIVATE).getString("pointData", it.readText())!!
        }
        val pointData = PointDataParser.deserializeJson(pointDataJson, matrics.heightPixels)
        if (pointData.width != matrics.widthPixels) {
            Toast.makeText(this, "不支援的螢幕解析度 ${matrics.widthPixels}x${matrics.heightPixels}", Toast.LENGTH_SHORT).show()
            return
        }
        val accessibilityManager = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val serviceInfo = accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC).find {
            val enabledServiceInfo = it.resolveInfo.serviceInfo
            enabledServiceInfo.packageName == packageName && enabledServiceInfo.name == MyService::class.java.name
        }
        if (serviceInfo == null) {
            Toast.makeText(this, "請開啟無障礙設定", Toast.LENGTH_SHORT).show()
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
            return
        }
        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val intent = projectionManager.createScreenCaptureIntent()
        startActivityForResult(intent, REQUEST_SCREEN_CAP)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_SCREEN_CAP -> {
                if (resultCode == RESULT_OK) {
                    val serviceIntent = Intent(this, MyService::class.java).apply {
                        action = "START_SERVICE"
                    }
                    serviceIntent.putExtra("mediaProjectionIntent", data)
                    ContextCompat.startForegroundService(this, serviceIntent)
                } else {
                    Toast.makeText(this, "無法取得螢幕權限", Toast.LENGTH_SHORT).show()
                }
            }
            REQUEST_CHOOSE_JSON -> {
                if (resultCode != RESULT_OK || data == null) return
                val uri = data.data ?: return
                try {
                    var input = ""
                    InputStreamReader(contentResolver.openInputStream(uri)).use {
                        input = it.readText()
                    }
                    val metrics = DisplayMetrics()
                    val display = (getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
                    display.getRealMetrics(metrics)
                    PointDataParser.deserializeJson(input, metrics.heightPixels)
                    getSharedPreferences("pointData", Context.MODE_PRIVATE).edit().putString("pointData", input).apply()
                    Toast.makeText(this, "成功載入資料", Toast.LENGTH_SHORT).show()
                } catch (ex: Exception) {
                    Toast.makeText(this, "解析檔案出錯 ${ex.localizedMessage}", Toast.LENGTH_SHORT).show()
                    Log.d("ERROR", if (ex.message != null) ex.message!! else "")
                }
            }
        }
    }

    fun onPointDataClick(view: View) {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
        }
        val chooser = Intent.createChooser(intent, "選擇點位資料Json檔")
        startActivityForResult(chooser, REQUEST_CHOOSE_JSON)
    }

    fun onCrashClick(view: View) {
        throw java.lang.Exception("Test firebase")
    }
}