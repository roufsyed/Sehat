//package com.rouf.saht.common.helper
//
//import android.content.Context
//import android.content.Intent
//import android.content.IntentSender
//import android.net.Uri
//import androidx.activity.ComponentActivity
//import io.paperdb.Paper
//import org.json.JSONArray
//import org.json.JSONObject
//
//object PaperBackupManager {
//    private const val MIME_TYPE = "application/json"
//
//    fun createBackupLauncher(activity: ComponentActivity, onBackupReady: (IntentSender) -> Unit) {
//        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
//            addCategory(Intent.CATEGORY_OPENABLE)
//            type = MIME_TYPE
//            putExtra(Intent.EXTRA_TITLE, "paperdb_backup.json")
//        }
//        val intentSender = Intent.createChooser(intent, "Choose backup location").intentSender
//        onBackupReady(intentSender)
//    }
//
//    fun openBackupLauncher(activity: ComponentActivity, onRestoreReady: (IntentSender) -> Unit) {
//        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
//            addCategory(Intent.CATEGORY_OPENABLE)
//            type = MIME_TYPE
//        }
//        val intentSender = Intent.createChooser(intent, "Select backup file").intentSender
//        onRestoreReady(intentSender)
//    }
//
//    fun doBackup(context: Context, uri: Uri) {
//        val json = JSONObject()
//        json.put("heart_rate_monitor_data",
//            JSONArray(Paper.book().read<List<Any>>("heart_rate_monitor_data", emptyList()))
//        )
//        json.put("pedometer_data_list", JSONArray(Paper.book().read<List<Any>>("pedometer_data_list", emptyList())))
//        json.put("personal_information", Paper.book().read<Any>("personal_information"))
//        json.put("pedometer_data", Paper.book().read<Any>("pedometer_data"))
//        json.put("pedometer_settings", Paper.book().read<Any>("pedometer_settings"))
//        json.put("heart_rate_monitor_settings", Paper.book().read<Any>("heart_rate_monitor_settings"))
//
//        context.contentResolver.openOutputStream(uri)?.use { output ->
//            output.write(json.toString().toByteArray())
//        }
//    }
//
//    fun doRestore(context: Context, uri: Uri) {
//        val jsonString = context.contentResolver.openInputStream(uri)?.use { input ->
//            input.bufferedReader().readText()
//        } ?: return
//
//        val json = JSONObject(jsonString)
//        Paper.book().write("heart_rate_monitor_data", json.optJSONArray("heart_rate_monitor_data")?.let { toList(it) })
//        Paper.book().write("pedometer_data_list", json.optJSONArray("pedometer_data_list")?.let { toList(it) })
//        Paper.book().write("personal_information", json.opt("personal_information"))
//        Paper.book().write("pedometer_data", json.opt("pedometer_data"))
//        Paper.book().write("pedometer_settings", json.opt("pedometer_settings"))
//        Paper.book().write("heart_rate_monitor_settings", json.opt("heart_rate_monitor_settings"))
//    }
//
//    private fun toList(jsonArray: JSONArray): List<Any?> {
//        val list = mutableListOf<Any?>()
//        for (i in 0 until jsonArray.length()) {
//            list.add(jsonArray.get(i))
//        }
//        return list
//    }
//}
