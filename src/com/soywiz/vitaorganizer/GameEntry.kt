package com.soywiz.vitaorganizer

import com.soywiz.util.DumperNames
import com.soywiz.util.stream
import java.io.File

class GameEntry(val gameId: String) {
	val entry = VitaOrganizerCache.entry(gameId)
	val psf by lazy {
		try {
			PSF.read(entry.paramSfoFile.readBytes().stream)
		} catch (e: Throwable) {
			mapOf<String, Any>()
		}
	}
	val hasExtendedPermissions by lazy { entry.permissions }
	val id by lazy { psf["TITLE_ID"].toString() }
	val title by lazy { psf["TITLE"].toString() }
	val dumperVersion by lazy { DumperNames.findDumperByShortName( entry.dumperVersion).longName }
	val dumperVersionShort by lazy { DumperNames.findDumperByShortName( entry.dumperVersion).shortName }
	val compressionLevel by lazy { entry.compression }
	var inVita = false
	var inPC = false
	val vpkLocalPath: String? get() = entry.pathFile
	val vpkLocalFile: File? get() = if (vpkLocalPath != null) File(vpkLocalPath!!) else null
	val size: Long by lazy { entry.size }

	fun region() : Region {
		if (id.contains("PCSB") || id.contains("PCSB"))
			return Region.EUR
		if (id.contains("PCSE") || id.contains("PCSA"))
			return Region.USA
		if (id.contains("PCSG") || id.contains("PCSC"))
			return Region.JPN
		return Region.UNKNOWN
	}

	enum class Region(val short: String, val long: String) {
		EUR("EUR", "Europe"),
		JPN("JPN", "Japan"),
		USA("USA", "North America"),
		UNKNOWN("UNKNOWN", "Unknown")
	}

	override fun toString(): String = id
}
