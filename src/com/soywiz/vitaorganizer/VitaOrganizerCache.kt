package com.soywiz.vitaorganizer

import com.soywiz.util.get
import java.io.File
import java.io.IOException

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
                    return 0L       //String is probably empty -> not initialized
                }
            }
            set(value) { sizeString = value.toString() }
        var permissions: Boolean    //there should be a third value -> non initialized; now permissionsString.isEmpty is used
            get() {                 //not sure if not initialized is an acceptable state
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

        fun delete() {  //Normally if one delete fails, all should fail -> game is already deleted
            deleteFile(icon0File)
            deleteFile(paramSfoFile)
            deleteFile(cacheFolder["$gameId.settings"])
        }

        fun deleteFile(file: File) {
            try {
                if (file.exists()) file.delete()
            } catch (io: IOException) {
                System.err.println("Error deleting ${file.name}")
                io.printStackTrace()
            }
        }
    }

    fun entry(gameId: String) = Entry(gameId)

}