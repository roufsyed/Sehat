package com.rouf.saht.common.helper

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.paperdb.Paper
import java.io.*
import java.lang.reflect.Type

object PaperDBHelper {

    private const val TAG = "PaperDBHelper"
    private const val BACKUP_FOLDER = "sehat_backups"
    private const val BACKUP_FILE_NAME = "sehat_backup.json"

    fun exportData(context: Context): Boolean {
        if (!hasStoragePermissions(context)) {
            Log.e(TAG, "Storage permission not granted")
            return false
        }

        val paperDir = File(context.filesDir, "io.paperdb")
        if (!paperDir.exists() || paperDir.list().isNullOrEmpty()) {
            Log.e(TAG, "PaperDB is empty or missing")
            return false
        }

        val backupMap = mutableMapOf<String, Any?>()
        paperDir.list()?.forEach { key ->
            backupMap[key] = Paper.book().read<Any>(key, null)
        }

        val json = Gson().toJson(backupMap)
        val backupFile = getBackupFile(context)

        return try {
            FileWriter(backupFile).use { it.write(json) }
            Log.d(TAG, "Exported to: ${backupFile.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed writing backup", e)
            false
        }
    }

    fun importData(context: Context): Boolean {
        if (!hasStoragePermissions(context)) {
            Log.e(TAG, "Storage permission not granted")
            return false
        }

        val backupFile = getBackupFile(context)
        if (!backupFile.exists()) {
            Log.e(TAG, "Backup file not found at ${backupFile.absolutePath}")
            return false
        }

        return try {
            val type: Type = object : TypeToken<Map<String, Any>>() {}.type
            val reader = FileReader(backupFile)
            val data: Map<String, Any> = Gson().fromJson(reader, type)
            reader.close()

            data.forEach { (key, value) ->
                Paper.book().write(key, value)
            }

            Log.d(TAG, "Import successful")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed importing backup", e)
            false
        }
    }

    fun hasStoragePermissions(context: Context): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> Environment.isExternalStorageManager()
            else -> {
                val writeGranted = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED

                val readGranted = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED

                writeGranted && readGranted
            }
        }
    }

    fun requestAllFilesPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
            !Environment.isExternalStorageManager()
        ) {
            val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    private fun getBackupFile(context: Context): File {
        val primaryDir = File(Environment.getExternalStorageDirectory(), BACKUP_FOLDER)
        if (primaryDir.exists() || primaryDir.mkdirs()) {
            return File(primaryDir, BACKUP_FILE_NAME)
        }

        val fallbackDir = File(context.getExternalFilesDir(null), BACKUP_FOLDER)
        if (!fallbackDir.exists()) fallbackDir.mkdirs()
        return File(fallbackDir, BACKUP_FILE_NAME)
    }
}
