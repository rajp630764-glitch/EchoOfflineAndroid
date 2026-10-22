package com.echooffline

import android.Manifest
import android.app.Activity
import android.content.ContentUris
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

data class AudioTrack(val id: Long, val title: String, val artist: String, val uri: Uri)

class MainActivity : Activity() {
    private val requestAudioPermission = 401
    private lateinit var statusText: TextView
    private lateinit var songsContainer: LinearLayout
    private var mediaPlayer: MediaPlayer? = null
    private var observer: ContentObserver? = null
    private val tracks = mutableListOf<AudioTrack>()
    private var currentlyPlaying: Long? = null

    private fun audioPermission(): String =
        if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
        else Manifest.permission.READ_EXTERNAL_STORAGE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
        if (checkSelfPermission(audioPermission()) == PackageManager.PERMISSION_GRANTED) {
            scanSongs()
        } else {
            statusText.text = "गाने स्कैन करने के लिए Music permission दें।"
            requestPermissions(arrayOf(audioPermission()), requestAudioPermission)
        }
    }

    private fun buildUi() {
        val outer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 32, 28, 20)
            setBackgroundColor(0xFF10131A.toInt())
        }
        outer.addView(TextView(this).apply {
            text = "Echo Offline"
            textSize = 28f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER_VERTICAL
        })
        outer.addView(TextView(this).apply {
            text = "आपके फ़ोन के गाने • ऑफलाइन म्यूज़िक"
            textSize = 14f
            setTextColor(0xFFB8C0D0.toInt())
            setPadding(0, 8, 0, 18)
        })
        statusText = TextView(this).apply {
            text = "Music library तैयार हो रही है…"
            textSize = 14f
            setTextColor(0xFFE3E8F2.toInt())
            setPadding(0, 0, 0, 12)
        }
        outer.addView(statusText)
        outer.addView(Button(this).apply {
            text = "गाने फिर से स्कैन करें"
            setOnClickListener { scanSongs() }
        })
        val scroll = ScrollView(this)
        songsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 12, 0, 12)
        }
        scroll.addView(songsContainer)
        outer.addView(scroll, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ))
        setContentView(outer)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == requestAudioPermission &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            scanSongs()
            Toast.makeText(this, "Music permission मिल गई", Toast.LENGTH_SHORT).show()
        } else if (requestCode == requestAudioPermission) {
            statusText.text = "Permission नहीं मिली। गाने दिखाने के लिए Settings में अनुमति दें।"
        }
    }

    private fun scanSongs() {
        if (checkSelfPermission(audioPermission()) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(audioPermission()), requestAudioPermission)
            return
        }
        val found = mutableListOf<AudioTrack>()
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.IS_MUSIC
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        try {
            contentResolver.query(
                collection, projection, selection, null,
                "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val name = cursor.getString(titleCol) ?: "Unknown title"
                    val artist = cursor.getString(artistCol) ?: "Unknown artist"
                    found.add(AudioTrack(
                        id, name, artist, ContentUris.withAppendedId(collection, id)
                    ))
                }
            }
            tracks.clear()
            tracks.addAll(found.distinctBy { it.id })
            renderTracks()
            statusText.text = if (tracks.isEmpty()) {
                "कोई गाना नहीं मिला। फ़ोन में ऑडियो डाउनलोड करके फिर स्कैन करें।"
            } else {
                "${tracks.size} गाने मिले • डुप्लिकेट ID हटाए गए"
            }
            registerMediaObserver()
        } catch (e: Exception) {
            statusText.text = "गाने स्कैन नहीं हो पाए: ${e.localizedMessage ?: "Unknown error"}"
        }
    }

    private fun renderTracks() {
        songsContainer.removeAllViews()
        if (tracks.isEmpty()) {
            songsContainer.addView(TextView(this).apply {
                text = "आपकी Music Library खाली है।"
                textSize = 16f
                setTextColor(0xFFE3E8F2.toInt())
                setPadding(0, 20, 0, 20)
            })
            return
        }
        tracks.forEach { track ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(12, 10, 8, 10)
                setBackgroundColor(0xFF1B202B.toInt())
            }
            val labels = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            labels.addView(TextView(this).apply {
                text = track.title
                textSize = 16f
                setTextColor(0xFFFFFFFF.toInt())
                maxLines = 2
            })
            labels.addView(TextView(this).apply {
                text = track.artist
                textSize = 12f
                setTextColor(0xFFB8C0D0.toInt())
                maxLines = 1
            })
            row.addView(labels, LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            ))
            row.addView(Button(this).apply {
                text = if (currentlyPlaying == track.id) "चल रहा" else "Play"
                setOnClickListener { playTrack(track) }
            })
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.bottomMargin = 8
            songsContainer.addView(row, params)
        }
    }

    private fun playTrack(track: AudioTrack) {
        try {
            val playbackIntent = Intent(this, PlaybackService::class.java).apply {
                putExtra(PlaybackService.EXTRA_URI, track.uri.toString())
                putExtra(PlaybackService.EXTRA_TITLE, track.title)
                putExtra(PlaybackService.EXTRA_ARTIST, track.artist)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(playbackIntent)
            } else {
                startService(playbackIntent)
            }
            currentlyPlaying = track.id
            statusText.text = "अब चल रहा है: ${track.title} • ऐप बंद करने पर भी संगीत जारी रह सकता है"
            renderTracks()
        } catch (e: Exception) {
            Toast.makeText(this, "यह गाना नहीं चल पाया।", Toast.LENGTH_SHORT).show()
        }
    }

    private fun registerMediaObserver() {
        if (observer != null) return
        observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                scanSongs()
            }
        }
        contentResolver.registerContentObserver(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true, observer!!
        )
    }

    override fun onDestroy() {
        observer?.let { contentResolver.unregisterContentObserver(it) }
        observer = null
        mediaPlayer?.release()
        mediaPlayer = null
        super.onDestroy()
    }
}
