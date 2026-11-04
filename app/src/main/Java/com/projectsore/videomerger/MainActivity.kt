package com.projectsore.videomerger

import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Presentation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import java.io.File
import java.io.FileInputStream

@UnstableApi
class MainActivity : ComponentActivity() {
    private var firstUri: Uri? = null
    private var secondUri: Uri? = null
    private var transformer: Transformer? = null
    private var exporting = false

    private lateinit var firstLabel: TextView
    private lateinit var secondLabel: TextView
    private lateinit var status: TextView
    private lateinit var mergeButton: Button
    private lateinit var progress: ProgressBar

    private val handler = Handler(Looper.getMainLooper())

    private val pickFirst = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            firstUri = it
            runCatching { contentResolver.takePersistableUriPermission(it, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            firstLabel.text = "Video 1 dipilih\n${displayName(it)}"
            updateButton()
        }
    }

    private val pickSecond = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            secondUri = it
            runCatching { contentResolver.takePersistableUriPermission(it, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            secondLabel.text = "Video 2 dipilih\n${displayName(it)}"
            updateButton()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installCrashLogger()
        try {
            buildInterface()
            showPreviousCrash()
        } catch (t: Throwable) {
            showFatal(t)
        }
    }

    // ---- Pelacak crash: tampilkan error di layar supaya mudah dilaporkan ----
    private fun crashFile() = File(filesDir, "last_crash.txt")

    private fun installCrashLogger() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            try {
                crashFile().writeText(android.util.Log.getStackTraceString(error))
            } catch (_: Throwable) {
            }
            previous?.uncaughtException(thread, error)
        }
    }

    private fun showPreviousCrash() {
        val file = crashFile()
        if (!file.exists()) return
        val text = file.readText().take(3500)
        file.delete()
        status.text = "CRASH SEBELUMNYA:\n$text"
        status.setTextIsSelectable(true)
        status.gravity = Gravity.START
    }

    private fun showFatal(error: Throwable) {
        val view = TextView(this).apply {
            text = "Gagal membuka layar:\n" + android.util.Log.getStackTraceString(error).take(3500)
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0xFF0B1220.toInt())
            setPadding(dp(16), dp(48), dp(16), dp(16))
            textSize = 12f
            setTextIsSelectable(true)
        }
        setContentView(android.widget.ScrollView(this).apply { addView(view) })
    }

    private fun buildInterface() {
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(24), dp(16), dp(24))
            setBackgroundColor(0xFF0B1220.toInt())
        }

        val title = TextView(this).apply {
            text = "VIDEO MERGER 1080p"
            textSize = 25f
            setTextColor(0xFFF3F6FB.toInt())
            gravity = Gravity.CENTER
        }
        page.addView(title, matchWrap())

        val subtitle = TextView(this).apply {
            text = "Gabungkan dua video menjadi satu file MP4 Full HD."
            textSize = 14f
            setTextColor(0xFF9EACC0.toInt())
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dp(24))
        }
        page.addView(subtitle, matchWrap())

        firstLabel = label("Video pertama belum dipilih")
        page.addView(firstLabel, matchWrap())
        page.addView(actionButton("PILIH VIDEO PERTAMA") { pickFirst.launch(arrayOf("video/*")) }, matchWrap(12))

        secondLabel = label("Video kedua belum dipilih")
        page.addView(secondLabel, matchWrap(18))
        page.addView(actionButton("PILIH VIDEO KEDUA") { pickSecond.launch(arrayOf("video/*")) }, matchWrap(12))

        val spec = TextView(this).apply {
            text = "OUTPUT  •  MP4 / H.264 / AAC\n1920 × 1080  •  Full HD\nAudio asli dari kedua video dipertahankan jika didukung."
            textSize = 13f
            setTextColor(0xFFB9C7D8.toInt())
            setPadding(dp(14), dp(14), dp(14), dp(14))
            setBackgroundColor(0xFF18263A.toInt())
            setLineSpacing(5f, 1f)
        }
        page.addView(spec, matchWrap(22))

        mergeButton = actionButton("GABUNGKAN VIDEO") { startMerge() }
        mergeButton.isEnabled = false
        page.addView(mergeButton, matchWrap(22))

        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progress = 0
            visibility = View.GONE
        }
        page.addView(progress, matchWrap(12))

        status = TextView(this).apply {
            text = "Pilih dua video untuk memulai."
            textSize = 13f
            setTextColor(0xFF9EACC0.toInt())
            setPadding(0, dp(12), 0, 0)
            gravity = Gravity.CENTER
        }
        page.addView(status, matchWrap())

        val scroll = android.widget.ScrollView(this).apply {
            setBackgroundColor(0xFF0B1220.toInt())
            addView(page)
        }
        // Targetsdk 35+ memaksa tampilan edge-to-edge: beri ruang untuk status bar & navigation bar.
        scroll.setOnApplyWindowInsetsListener { v, insets ->
            if (Build.VERSION.SDK_INT >= 30) {
                val bars = insets.getInsets(android.view.WindowInsets.Type.systemBars())
                v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            } else {
                @Suppress("DEPRECATION")
                v.setPadding(
                    insets.systemWindowInsetLeft,
                    insets.systemWindowInsetTop,
                    insets.systemWindowInsetRight,
                    insets.systemWindowInsetBottom
                )
            }
            insets
        }
        setContentView(scroll)
    }

    private fun label(textValue: String) = TextView(this).apply {
        text = textValue
        textSize = 14f
        setTextColor(0xFFF3F6FB.toInt())
        setPadding(dp(14), dp(12), dp(14), dp(12))
        setBackgroundColor(0xFF18263A.toInt())
    }

    private fun actionButton(textValue: String, action: () -> Unit) = Button(this).apply {
        text = textValue
        isAllCaps = false
        textSize = 14f
        setOnClickListener { action() }
    }

    private fun matchWrap(top: Int = 0) = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    ).apply { if (top > 0) topMargin = dp(top) }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun updateButton() {
        mergeButton.isEnabled = firstUri != null && secondUri != null && !exporting
    }

    private fun displayName(uri: Uri): String {
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            val index = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && it.moveToFirst()) return it.getString(index) ?: "Video dipilih"
        }
        return "Video dipilih"
    }

    private fun startMerge() {
        try {
            startMergeInternal()
        } catch (t: Throwable) {
            finishExport()
            status.text = "Error:\n" + android.util.Log.getStackTraceString(t).take(3000)
            status.setTextIsSelectable(true)
            status.gravity = Gravity.START
        }
    }

    private fun startMergeInternal() {
        val uri1 = firstUri ?: return
        val uri2 = secondUri ?: return
        if (exporting) return

        exporting = true
        updateButton()
        progress.visibility = View.VISIBLE
        progress.progress = 0
        status.text = "Menyiapkan video..."

        val item1 = makeEditedItem(uri1)
        val item2 = makeEditedItem(uri2)
        val sequence = EditedMediaItemSequence.withAudioAndVideoFrom(listOf(item1, item2))
        val composition = Composition.Builder(sequence).build()
        val outputFile = File(cacheDir, "merged-${System.currentTimeMillis()}.mp4")

        val listener = object : Transformer.Listener {
            override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                handler.removeCallbacks(progressRunnable)
                status.text = "Menyimpan hasil ke galeri..."
                saveToGallery(outputFile)
            }

            override fun onError(
                composition: Composition,
                exportResult: ExportResult,
                exportException: ExportException
            ) {
                finishExport()
                status.text = "Gagal: ${exportException.message ?: "format video tidak didukung"}"
                Toast.makeText(this@MainActivity, "Penggabungan gagal.", Toast.LENGTH_LONG).show()
                outputFile.delete()
            }
        }

        transformer = Transformer.Builder(this)
            .setVideoMimeType(MimeTypes.VIDEO_H264)
            .setAudioMimeType(MimeTypes.AUDIO_AAC)
            .addListener(listener)
            .build()

        try {
            transformer?.start(composition, outputFile.absolutePath)
            status.text = "Sedang menggabungkan video..."
            handler.post(progressRunnable)
        } catch (error: Exception) {
            finishExport()
            status.text = "Tidak dapat memulai: ${error.message}"
            outputFile.delete()
        }
    }

    private fun makeEditedItem(uri: Uri): EditedMediaItem {
        val mediaItem = MediaItem.fromUri(uri)
        val effects = androidx.media3.common.Effects(
            emptyList(),
            listOf(Presentation.createForWidthAndHeight(
                1920,
                1080,
                Presentation.LAYOUT_SCALE_TO_FIT
            ))
        )
        return EditedMediaItem.Builder(mediaItem)
            .setEffects(effects)
            .build()
    }

    private val progressRunnable = object : Runnable {
        override fun run() {
            val current = transformer ?: return
            val holder = androidx.media3.transformer.ProgressHolder()
            if (current.getProgress(holder) == Transformer.PROGRESS_STATE_AVAILABLE) {
                progress.progress = holder.progress
                status.text = "Memproses video... ${holder.progress}%"
            }
            if (exporting) handler.postDelayed(this, 500)
        }
    }

    private fun saveToGallery(file: File) {
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                // Android 9 ke bawah: MediaStore butuh izin storage, jadi simpan ke folder app.
                val base = getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: filesDir
                val dir = File(base, "VideoMerger").apply { mkdirs() }
                val target = File(dir, "Video-Gabungan-1080p-${System.currentTimeMillis()}.mp4")
                file.copyTo(target, overwrite = true)
                file.delete()
                finishExport()
                status.text = "Berhasil! Video tersimpan di:\n${target.absolutePath}"
                Toast.makeText(this, "Video berhasil disimpan.", Toast.LENGTH_LONG).show()
                return
            }
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, "Video-Gabungan-1080p-${System.currentTimeMillis()}.mp4")
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/VideoMerger")
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }
            }
            val uri = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                ?: throw IllegalStateException("Tidak dapat membuat file di galeri.")
            contentResolver.openOutputStream(uri)?.use { output ->
                FileInputStream(file).use { input -> input.copyTo(output) }
            } ?: throw IllegalStateException("Tidak dapat menulis file hasil.")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val done = ContentValues().apply { put(MediaStore.Video.Media.IS_PENDING, 0) }
                contentResolver.update(uri, done, null, null)
            }
            file.delete()
            finishExport()
            status.text = "Berhasil! Video tersimpan di Movies/VideoMerger."
            Toast.makeText(this, "Video berhasil disimpan ke galeri.", Toast.LENGTH_LONG).show()
        } catch (error: Exception) {
            finishExport()
            status.text = "Video selesai, tetapi gagal disimpan: ${error.message}"
        }
    }

    private fun finishExport() {
        handler.removeCallbacks(progressRunnable)
        exporting = false
        updateButton()
        progress.visibility = View.GONE
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        if (exporting) transformer?.cancel()
        super.onDestroy()
    }
}
