package com.aowtest.myapplication

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.widget.Toast
import java.lang.reflect.Field


/**
 * @author oukanggui
 * @date 2018/11/7
 * @describe  ToastUtil：
 * 1、Toast manager：Design single Toast object to show non-blocking Toast and cancel Toast easily
 * 2、Fix the BadTokenException happened on the device 7.x while showing Toast which will cause your app to crash
 */
object ToastUtil {
    private const val TAG = "ToastUtil"
    private var mToast: Toast? = null
    private var sField_TN: Field? = null
    private var sField_TN_Handler: Field? = null
    private var sIsHookFieldInit = false
    private const val FIELD_NAME_TN = "mTN"
    private const val FIELD_NAME_HANDLER = "mHandler"
    /**
     * Non-blocking showing Toast
     * @param context  context，Application or Activity
     * @param text     the text show on the Toast
     * @param duration Toast.LENGTH_SHORT（default,2s） or Toast.LENGTH_LONG（3.5s）
     */
    @JvmOverloads
    fun showToast(
        context: Context,
        text: CharSequence?,
        duration: Int = Toast.LENGTH_SHORT
    ) {
        val toastRunnable = ToastRunnable(context, text, duration)
        if (context is Activity) {
            if (!context.isFinishing) {
                context.runOnUiThread(toastRunnable)
            }
        } else {
            val handler = Handler(context.mainLooper)
            handler.post(toastRunnable)
        }
    }

    /**
     * cancel the toast
     */
    fun cancelToast() {
        val looper = Looper.getMainLooper()
        if (looper.thread === Thread.currentThread()) {
            mToast!!.cancel()
        } else {
            Handler(looper).post { mToast!!.cancel() }
        }
    }

    /**
     * Hook Toast,fix the BadTokenException happened on the device 7.x while showing Toast which will cause your app to crash
     *
     * @param toast
     */
    private fun hookToast(toast: Toast?) {
        if (!isNeedHook) {
            return
        }
        try {
            if (!sIsHookFieldInit) {
                sField_TN = Toast::class.java.getDeclaredField(FIELD_NAME_TN)
                sField_TN?.isAccessible = true
                sField_TN_Handler =
                    sField_TN?.type?.getDeclaredField(FIELD_NAME_HANDLER)
                sField_TN_Handler?.isAccessible = true
                sIsHookFieldInit = true
            }
            val tn = sField_TN!![toast]
            val originHandler =
                sField_TN_Handler!![tn] as Handler
            sField_TN_Handler!![tn] = SafelyHandlerWarpper(originHandler)
        } catch (e: Exception) {
            Log.e(TAG, "Hook toast exception=$e")
        }
    }

    /**
     * Check if Toast need hook，only hook the device 7.x(api = 24/25)
     *
     * @return true for need hook to fit system bug,false for don't need hook
     */
    private val isNeedHook: Boolean
        get() = Build.VERSION.SDK_INT == Build.VERSION_CODES.N_MR1 ||
                Build.VERSION.SDK_INT == Build.VERSION_CODES.N

    private class ToastRunnable(
        private val context: Context,
        private val text: CharSequence?,
        private val duration: Int
    ) :
        Runnable {
        @SuppressLint("ShowToast")
        override fun run() {
            mToast?.cancel()
            mToast = Toast.makeText(context, text, duration).apply {
                hookToast(this)
                show()
            }
        }

    }

    /**
     * Safe outside Handler class which just warps the system origin handler object in the Toast.class
     */
    private class SafelyHandlerWarpper(private val originHandler: Handler?) :
        Handler(Looper.getMainLooper()) {
        override fun dispatchMessage(msg: Message) {
            // The outside hanlder SafelyHandlerWarpper object just catches the Exception while dispatch the message
            // if the the inside system origin hanlder object throw the BadTokenException，the outside safe SafelyHandlerWarpper object
            // just catches the exception here to avoid the app crashing
            try {
                super.dispatchMessage(msg)
            } catch (e: Exception) {
                Log.e(TAG, "Catch system toast exception:$e")
            }
        }

        override fun handleMessage(msg: Message) {
            //just pass the Message to the origin handler object to handle
            originHandler?.handleMessage(msg)
        }

    }
}