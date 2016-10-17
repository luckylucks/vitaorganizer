package com.soywiz.vitaorganizer

import com.soywiz.util.stream
import com.soywiz.util.DumperNames
import java.io.File

class CachedGameEntry(val gameId: String) {
	val entry = VitaOrganizerCache.entry(gameId)
	val psf by lazy {
		try {
			PSF.read(entry.paramSfoFile.readBytes().stream)
		} catch (e: Throwable) {
			mapOf<String, Any>()
		}
	}
	val hasExtendedPermissions by lazy {
		try {
			entry.permissions
		} catch (e: Throwable) {
			true
		}
	}
	val maidump by lazy { entry.maydump }
	val special by lazy { entry.special }
	enum class SpecialValue(val short: String, val prettyPrint: String){
		DLC("DLC", " - DLC"),
		UPDATE("UPDATE", " - Update")
	}

	val attribute by lazy { psf["ATTRIBUTE"].toString() }
	val id by lazy { psf["TITLE_ID"].toString() }
	val title by lazy { psf["TITLE"].toString() }
	val dumperVersion by lazy { 
        var text = "UNKNOWN";
		if(attribute == "32768")
			text = "HB"
    	else if(entry.dumperVersion.isNotEmpty())
			text = entry.dumperVersion

        DumperNames.findDumperByShortName(text).longName
    }
	val dumperVersionShort by lazy { DumperNames.findDumperByShortName( entry.dumperVersion).shortName }
	fun region() : GameEntry.Region {
		if (id.contains("PCSB") || id.contains("PCSF"))
			return GameEntry.Region.EUR
		if (id.contains("PCSE") || id.contains("PCSA"))
			return GameEntry.Region.USA
		if (id.contains("PCSG") || id.contains("PCSC"))
			return GameEntry.Region.JPN
		return GameEntry.Region.UNKNOWN
	}

	val compressionLevel by lazy { 
        if(entry.compression.isNotEmpty())  {
			//see https://pkware.cachefly.net/webdocs/casestudies/APPNOTE.TXT
			//4.4.5
			val method = entry.compression
			if(method == "0")
				"not compressed"
			else if(method == "1")
				"shrunk"
			else if(method == "2")
				"compresion factor 1"
			else if(method == "3")
				"compresion factor 2"
			else if(method == "4")
				"compresion factor 3"
			else if(method == "5")
				"compresion factor 4"
			else if(method == "6")
				"imploded"
			else if(method == "7")
				"reversed"
			else if(method == "8")
				"deflate"
			else if(method == "9")
				"shrunk64"
			else
				method
		}
        else 
			"could not read from param.sfo" 
    }
	var inVita = false
	var inPC = false
	val vpkLocalPath: String? get() = entry.pathFile
	val vpkLocalFile: File? get() = if (vpkLocalPath != null) File(vpkLocalPath!!) else null
	val vpkLocalVpkFile: VpkFile? get() = if (vpkLocalPath != null) VpkFile(File(vpkLocalPath!!)) else null
	val size: Long by lazy {
		try {
			entry.size
		} catch (e: Throwable) {
			0L
		}
	}

	override fun toString(): String = id
}

enum class Region(val short: String, val long: String) {
	EUR("EUR", "Europe"),
	JPN("JPN", "Japan"),
	USA("USA", "North America"),
	UNKNOWN("UNKNOWN", "Unknown")
}
