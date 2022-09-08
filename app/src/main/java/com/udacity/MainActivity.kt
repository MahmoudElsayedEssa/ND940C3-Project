package com.udacity

import android.app.DownloadManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import com.udacity.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {

    private var downloadID: Long = 0

    private lateinit var notificationManager: NotificationManager
    private lateinit var pendingIntent: PendingIntent
    private lateinit var action: NotificationCompat.Action

    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))

        binding.contentMain.button.setOnClickListener {
            if (URL != "")
                download()
            else
                Toast.makeText(this, "please select the file to download", Toast.LENGTH_SHORT)
                    .show()
        }

        binding.contentMain.radioGroup.setOnCheckedChangeListener { _, checkedId ->

            when (checkedId) {
                R.id.btn_radio_glide -> {
                    URL = URL_GLIDE
                    fileName = getString(R.string.radio_glide)
                }
                R.id.btn_radio_loadApp -> {
                    URL = URL_LOAD_APP
                    fileName = getString(R.string.radio_loadApp)
                }
                R.id.btn_radio_retrofit -> {
                    URL = URL_RETROFIT
                    fileName = getString(R.string.radio_retrofit)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
        }
    }


    private fun download() {
        try {
            val request =
                DownloadManager.Request(Uri.parse(URL))
                    .setTitle(fileName)
                    .setDescription(getString(R.string.app_description))
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setRequiresCharging(false)
                    .setAllowedOverMetered(true)
                    .setAllowedOverRoaming(true)

            val downloadManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
            downloadID =
                downloadManager.enqueue(request)// enqueue puts the download request in the queue.
            Toast.makeText(this, "file downloaded", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Something Went Wrong", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private var URL = ""
        private var fileName = ""

        private const val URL_GLIDE =
            "https://github.com/bumptech/glide"
        private const val URL_LOAD_APP =
            "https://github.com/udacity/nd940-c3-advanced-android-programming-project-starter"
        private const val URL_RETROFIT =
            "https://github.com/square/retrofit"

        private const val CHANNEL_ID = "channelId"
    }

}
