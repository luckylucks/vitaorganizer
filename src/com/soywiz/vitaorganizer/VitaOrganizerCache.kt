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
        val icon0File = cacheFolder["$gameId.icon0.png"]
        val paramSfoFile = cacheFolder["$gameId.param.sfo"]
        /*
        val pathFile = cacheFolder["$gameId.path"]
        val sizeFile = cacheFolder["$gameId.size"]
        val permissionsFile = cacheFolder["$gameId.extperm"]
        val dumperVersionFile = cacheFolder["$gameId.dumperversion"]
        val compressionFile = cacheFolder["$gameId.compression"]
        */
        var pathFile: String by PropDelegate("")
        var sizeFile: String by PropDelegate("")
        var permissionsFile: String by PropDelegate("")
        var dumperVersionFile: String by PropDelegate("")
        var compressionFile: String by PropDelegate("")

        fun delete() {
            icon0File.delete()
            paramSfoFile.delete()
            /*
            pathFile.delete()
            sizeFile.delete()
            permissionsFile.delete()
            dumperVersionFile.delete()
            compressionFile.delete()
            */
            cacheFolder["$gameId.settings"].delete()
        }
    }

    fun entry(gameId: String) = Entry(gameId)

    /*
    fun setIcon0File(titleId: String, data: ByteArray) {
        getIcon0File(titleId).writeBytes(data)
    }

    fun setParamSfoFile(titleId: String, data: ByteArray) {
        getParamSfoFile(titleId).writeBytes(data)
    }

    fun setVpkPath(titleId: String, path: String) {
        getVpkPathFile(titleId).writeBytes(path.toByteArray(Charsets.UTF_8))
    }

    fun getIcon0File(titleId: String): File {
        cacheFolder.mkdirs()
        return cacheFolder["$titleId.icon0.png"]
    }

    fun getParamSfoFile(titleId: String): File {
        cacheFolder.mkdirs()
        return cacheFolder["$titleId.param.sfo"]
    }

    fun getVpkPathFile(titleId: String): File {
        cacheFolder.mkdirs()
        return cacheFolder["$titleId.path"]
    }

    fun getVpkPath(titleId: String): String? {
        return try {
            getVpkPathFile(titleId).readText(Charsets.UTF_8)
        } catch (e: Throwable) {
            null
        }
    }
    */
}