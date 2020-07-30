package com.example.testcontentresolver

import android.content.ContentResolver
import android.content.Context
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.GuardedBy
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.util.logging.Logger

private const val TAG = "NewImageContentObserver"

private val MEDIASTORE_PROJECTION = arrayOf(
        MediaStore.Images.ImageColumns.DATE_MODIFIED, // The time the file was last modified. Units are seconds
        MediaStore.MediaColumns.DATA // Path to the file on disk.
)

data class ImageMetadata(
        val timeTaken: Long,
        val filePath: String
)

class NewImageContentObserver(
        val context: Context
): ContentObserver(null) {

    @GuardedBy("itself")
    private val visitedUris = LinkedHashSet<Uri>()

    override fun onChange(selfChange: Boolean) {
        super.onChange(selfChange, null)
    }

    override fun onChange(selfChange: Boolean, uri: Uri?) {
        GlobalScope.async {
            if (!visitedUris.contains(uri) && uri != null) {
                visitedUris.add(uri)
                Log.v(TAG, "visitedUris: add $uri")

                context.contentResolver.query(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        MEDIASTORE_PROJECTION,
                        null,
                        null,
                        MediaStore.Images.ImageColumns.DATE_MODIFIED + " DESC LIMIT 1" // Only query the most recent image
                ).use { cursor ->
                    getImageData(cursor)?.let {
                        val currentTime = System.currentTimeMillis()
                        Log.v(TAG, """
                        |==============================
                        |New Image detected: 
                        |Metadata - $it
                        |Current time stamp from system - $currentTime
                        |Difference between system time and image metadata: ${(currentTime - it.timeTaken)/1000} second""".trimMargin("|"))
                    }
                }
            }
        }
    }

    fun getImageData(cursor: Cursor?): ImageMetadata? {
        if (cursor == null) {
            Log.v(TAG, "getImageDataList passed in a null cursor, return")
            return null
        }

        if (cursor.moveToFirst()) {
            val imagePath = cursor.getString(1)
            val imageLastModifiedTimeMillis = cursor.getLong(0) * 1000 // millis
            return ImageMetadata(imageLastModifiedTimeMillis, imagePath)
        }

        return null
    }
}

