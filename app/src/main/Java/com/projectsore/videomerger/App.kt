package com.projectsore.videomerger

import android.app.Application
import android.content.Intent
import android.util.Log
import java.io.File
import kotlin.system.exitProcess

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        // Jangan pasang handler di proses ":crash" supaya tidak berulang.
        if (processName().endsWith(":crash")) return

        Thread.setDefaultUncaughtExceptionHandler { _, error ->
            try {
                val trace = Log.getStackTraceString(error)
                val intent = Intent(this, CrashActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    .putExtra("trace", trace)
                startActivity(intent)
                Thread.sleep(400)
            } catch (_: Throwable) {
            }
            android.os.Process.killProcess(android.os.Process.myPid())
            exitProcess(10)
        }
    }

    private fun processName(): String = try {
        File("/proc/self/cmdline").readText().trim('\u0000')
    } catch (_: Throwable) {
        ""
    }
}
