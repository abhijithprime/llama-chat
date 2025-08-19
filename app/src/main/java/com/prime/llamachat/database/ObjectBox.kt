package com.prime.llamachat.database

import android.content.Context
import android.util.Log
import com.prime.llamachat.BuildConfig
import com.prime.llamachat.database.entities.MyObjectBox
import io.objectbox.BoxStore
import io.objectbox.android.Admin


object ObjectBox {
    lateinit var store: BoxStore
        private set

    fun init(context: Context) {
        if (!::store.isInitialized) {
            store = MyObjectBox.builder()
                .androidContext(context.applicationContext)
                .build()
            Log.i("ObjectBox", "ObjectBox initialized ${store.isClosed}")
            if (BuildConfig.DEBUG) {
              try {
                    val started = Admin(store).start(context.applicationContext)
                    Log.i("ObjectBoxAdmin", "Started: $started")
                } catch (e: Exception) {
                    Log.e("ObjectBoxAdmin", "Failed to start Admin", e)
                }
            }
        }
    }
}