package com.soywiz.vitaorganizer

import com.soywiz.util.get
import java.io.File

object VitaOrganizerCache {
    val directory = "vitaorganizer/cache"
    val cacheFolder = File(directory)

    init {
        cacheFolder.mkdirs()
    }

    class Entry(val gameId: String) : AbstractSettings( cacheFolder["$gameId.settings"] ) {
        //data stored into files
        val icon0File = cacheFolder["$gameId.icon0.png"]
        val paramSfoFile = cacheFolder["$gameId.param.sfo"]

        //data stored into game specific property file
        var pathFile: String by PropDelegate("")
        var size: Long
            get() {
                try {
                    return sizeString.toLong()
                } catch (e: Throwable) {
                    return 0L
                }
            }
            set(value) { sizeString = value.toString() }
        var permissions: Boolean
            get() {
                try {
                    return permissionsString.toBoolean()
                } catch (e: Throwable) {
                    return true
                }
            }
            set(value) {
                permissionsString = value.toString()
            }

        var dumperVersion: String by PropDelegate("")
        var compression: String by PropDelegate("")

        //internal String representations for non string variables
        var sizeString: String by PropDelegate("")
        var permissionsString: String by PropDelegate("")

        fun delete() {
            icon0File.delete()
            paramSfoFile.delete()
            cacheFolder["$gameId.settings"].delete()
        }
    }

    fun entry(gameId: String) = Entry(gameId)

}