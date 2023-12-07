package com.vikas.bledevicestate

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    val PERMISSION_PREFERENCES = "permissionSharedPrefs"
    private val sharedPreferences: SharedPreferences
    private var editor: SharedPreferences.Editor? = null

    init {
        sharedPreferences =
            context.getSharedPreferences(PERMISSION_PREFERENCES, Context.MODE_PRIVATE)
    }

    fun firstTimeAskingPermission(permission: String?, isFirstTime: Boolean) {
        doEdit()
        editor!!.putBoolean(permission, isFirstTime)
        doCommit()
    }

    fun isFirstTimeAskingPermission(permission: String?): Boolean {
        return sharedPreferences.getBoolean(permission, true)
    }

    private fun doEdit() {
        if (editor == null) {
            editor = sharedPreferences.edit()
        }
    }

    private fun doCommit() {
        if (editor != null) {
            editor!!.commit()
            editor = null
        }
    }
}