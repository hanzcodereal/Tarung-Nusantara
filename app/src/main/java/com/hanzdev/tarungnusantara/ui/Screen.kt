package com.hanzdev.tarungnusantara.ui

import android.content.Context
import android.view.View
import com.hanzdev.tarungnusantara.game.SessionConfig

interface Nav {
    val context: Context
    fun push(screen: Screen)
    fun pop()
    fun replace(screen: Screen)
    fun popTo(count: Int)
    fun startGame(cfg: SessionConfig)
    fun toast(msg: String)
    fun openUrl(url: String)
    fun sendEmail(address: String)
    fun requestBluetooth(onResult: (Boolean) -> Unit)
    fun requestEnableBluetooth(onResult: (Boolean) -> Unit)
    fun requestDiscoverable()
    fun music(file: String)
}

abstract class Screen {
    lateinit var nav: Nav
    abstract fun build(ctx: Context): View
    open fun onShow() {}
    open fun onHide() {}
    open fun onBack(): Boolean = false
    open val music: String = "m_menu.ogg"
    open val musicLoop: Boolean = true
}
