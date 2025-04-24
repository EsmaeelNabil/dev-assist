package dev.supersam.devassist

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

// get the application context using reflection

@SuppressLint("PrivateApi")
fun getApplicationContext(): Context? {
    return try {
        val activityThreadClass = Class.forName("android.app.ActivityThread")
        val currentActivityThreadMethod = activityThreadClass.getMethod("currentActivityThread")
        val currentActivityThread = currentActivityThreadMethod.invoke(null)
        val getApplicationMethod = activityThreadClass.getMethod("getApplication")
        getApplicationMethod.invoke(currentActivityThread) as Context
    } catch (e: Exception) {
        null
    }
}


fun Context.getSharedPreferences(): SharedPreferences {
    return this.getSharedPreferences("dev_assist", Context.MODE_PRIVATE)
}

interface Cache {
    fun getString(key: String, defaultValue: String): String
    fun putString(key: String, value: String)
}

object DevAssistCache : Cache {
    val sharedPreferences: SharedPreferences? get() = getApplicationContext()?.getSharedPreferences()

    fun getAll(): Map<String, *>? {
        return sharedPreferences?.all
    }

    override fun getString(key: String, defaultValue: String) =
        sharedPreferences?.getString(key, defaultValue)
            ?: defaultValue


    override fun putString(key: String, value: String) {
        sharedPreferences?.edit { putString(key, value) }
    }
}


