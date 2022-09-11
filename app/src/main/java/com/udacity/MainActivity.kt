package com.udacity

import android.app.DownloadManager
import android.app.DownloadManager.COLUMN_STATUS
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import com.udacity.databinding.ActivityMainBinding
import com.udacity.utils.sendNotification


class MainActivity : AppCompatActivity() {


    private lateinit var downloadManager: DownloadManager
    private var downloadID: Long = 0

    private lateinit var notificationManager: NotificationManager
    private var downloadContentObserver: ContentObserver? = null

    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))


        createChannel(
            getString(R.string.download_notification_channel_id),
            getString(R.string.download_notification_channel_name)
        )


        with(binding.contentMain) {
            loadingButton.setOnClickListener {
                if (url != "")
                    download()
                else
                    Toast.makeText(
                        this@MainActivity,
                        "please select the file to download",
                        Toast.LENGTH_SHORT
                    )
                        .show()
            }
            radioGroup.setOnCheckedChangeListener { _, checkedId ->

                when (checkedId) {
                    R.id.btn_radio_glide -> loadGlide()
                    R.id.btn_radio_loadApp -> loadLoadApp()
                    R.id.btn_radio_retrofit -> loadRetrofit()
                }
            }


        }


    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
        downloadContentObserver = null
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            id?.let {
                val downloadStatus = downloadManager.queryStatus(it)
                downloadStatus.takeIf { status -> status != DownloadStatus.UNKNOWN }?.run {
                    notificationManager.sendNotification(
//                    this@MainActivity.getText(R.string.notification_description).toString(),
                        fileName,
                        downloadStatus,
                        this@MainActivity
                    )

                }
            }

        }
    }


    private fun download() {
        try {
            val request =
                DownloadManager.Request(Uri.parse(url))
                    .setTitle(fileName)
                    .setDescription(getString(R.string.app_description))
                    .setRequiresCharging(false)
                    .setAllowedOverMetered(true)
                    .setAllowedOverRoaming(true)

            downloadManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
            downloadID =
                downloadManager.enqueue(request)// enqueue puts the download request in the queue.

            downloadManager.createAndRegisterDownloadContentObserver()

        } catch (e: Exception) {
            Toast.makeText(this, "Something Went Wrong", Toast.LENGTH_SHORT).show()
        }
    }

    private fun DownloadManager.queryStatus(id: Long): DownloadStatus {
        query(DownloadManager.Query().setFilterById(id)).use {
            with(it) {
                if (this != null && moveToFirst()) {
                    val columnIndexStatus = getColumnIndex(COLUMN_STATUS)

                    return when (getInt(columnIndexStatus)) {
                        DownloadManager.STATUS_SUCCESSFUL -> DownloadStatus.SUCCESSFUL
                        DownloadManager.STATUS_FAILED -> DownloadStatus.FAILED
                        else -> DownloadStatus.UNKNOWN
                    }
                }
                return DownloadStatus.UNKNOWN
            }
        }
    }


    private fun createChannel(channelId: String, channelName: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            )
                .apply {
                    setShowBadge(false)
                }

            notificationChannel.enableLights(true)
            notificationChannel.lightColor = Color.RED
            notificationChannel.enableVibration(true)
            notificationChannel.description =
                getString(R.string.notification_description)

            notificationManager = this.getSystemService(
                NotificationManager::class.java
            )
            notificationManager.createNotificationChannel(notificationChannel)

        }
    }

    private fun DownloadManager.createAndRegisterDownloadContentObserver() {
        object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                downloadContentObserver?.run { queryProgress() }
            }
        }.also {
            downloadContentObserver = it
            contentResolver.registerContentObserver(
                "content://downloads/my_downloads".toUri(),
                true,
                downloadContentObserver!!
            )
        }
    }

    private fun DownloadManager.queryProgress() {
        query(DownloadManager.Query().setFilterById(downloadID)).use {
            with(it) {
                if (this != null && moveToFirst()) {
                    val columnIndexStatus = getColumnIndex(COLUMN_STATUS)
                    when (getInt(columnIndexStatus)) {
                        DownloadManager.STATUS_FAILED -> {
                            binding.contentMain.loadingButton.changeButtonState(ButtonState.Completed)
                        }

                        DownloadManager.STATUS_RUNNING -> {
                            binding.contentMain.loadingButton.changeButtonState(ButtonState.Loading)
                        }
                        DownloadManager.STATUS_SUCCESSFUL -> {
                            binding.contentMain.loadingButton.changeButtonState(ButtonState.Completed)
                        }
                    }
                }
            }
        }
    }


    companion object {
        var url = ""
        var fileName = ""
    }

    private fun loadGlide() {
        url = "https://github.com/bumptech/glide"
        fileName = getString(R.string.radio_glide)
    }

    private fun loadLoadApp() {
        url = "https://github.com/udacity/nd940-c3-advanced-android-programming-project-starter"
        fileName = getString(R.string.radio_loadApp)
    }

    private fun loadRetrofit() {
        url = "https://github.com/square/retrofit"
        fileName = getString(R.string.radio_retrofit)
    }


}

enum class DownloadStatus(val statusText: String) {
    SUCCESSFUL("Successful"), FAILED("Failed"), UNKNOWN("Unknown")
}