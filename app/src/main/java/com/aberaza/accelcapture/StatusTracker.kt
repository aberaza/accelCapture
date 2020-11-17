package com.aberaza.accelcapture

import android.content.Context
import android.content.SharedPreferences

enum class ServiceState {
    STARTED,
    STOPPED
}

private const val name = "SENSORS_READ_KEY"
private const val serviceState = "SENSORS_READ_STATE"
private const val userName = "SENSORS_READ_USER_NAME"
private const val activityName = "ACTIVITY_NAME"


private fun getPreferences(context: Context): SharedPreferences {
    return context.getSharedPreferences(name, 0)
}



fun setServiceState(context: Context, state: ServiceState) {
    val sharedPrefs = getPreferences(context)
    sharedPrefs.edit().let {
        it.putString(serviceState, state.name)
        it.apply()
    }
}

fun getServiceState(context: Context): ServiceState {
    val sharedPrefs = getPreferences(context)
    val value = sharedPrefs.getString(serviceState, ServiceState.STOPPED.name)?: "null"
    return ServiceState.valueOf(value)
}

fun setUserName(context: Context, name: String) {
    val sharedPrefs = getPreferences(context)
    sharedPrefs.edit().let {
        it.putString(userName, name)
        it.apply()
    }
}

fun getUserName(context: Context) : String {
    val sharedPrefs = getPreferences(context)
    return sharedPrefs.getString(userName, "name")?:"name"
}

fun setActivity(context:Context, activity: String) {
    val sharedPrefs = getPreferences(context)
    sharedPrefs.edit().let {
        it.putString(activityName, activity)
        it.apply()
    }
}

fun getActivity(context: Context) : String {
    val sharedPrefs = getPreferences(context)
    return sharedPrefs.getString(activityName, "unknownActivity")?:"unknown activity"
}