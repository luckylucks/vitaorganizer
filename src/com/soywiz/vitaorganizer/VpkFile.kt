package com.soywiz.vitaorganizer

import com.soywiz.util.DumperModules
import com.soywiz.util.DumperNames
import com.soywiz.util.open2
import com.soywiz.vitaorganizer.ext.getBytes
import java.io.File
import java.util.zip.ZipFile

class VpkFile(val vpkFile: File) {
	//val entry: GameEntry by lazy { VpkFile() }

	val paramSfoData: ByteArray by lazy {
		try {
			ZipFile(vpkFile).use { zip ->
				zip.getBytes("sce_sys/param.sfo")
			}
		} catch (e: Throwable) {
			byteArrayOf()
		}
	}

	val psf by lazy {
		try {
			ZipFile(vpkFile).use { zip ->
				PSF.read(paramSfoData.open2("r"))
			}
		} catch (e: Throwable) {
			hashMapOf<String, Any>()
		}
	}

	val id by lazy { psf["TITLE_ID"].toString() }
	val title by lazy { psf["TITLE"].toString() }
	val hasExtendedPermissions: Boolean by lazy {
		try {
			ZipFile(vpkFile).use { zip ->
				val ebootBinData = zip.getBytes("eboot.bin")
				EbootBin.hasExtendedPermissions(ebootBinData.open2("r"))
			}
		} catch (e: Throwable) {
			true
		}
	}

	fun cacheAndGetGameId(): String? {
		var retGameId:String? = null
		try {
			ZipFile(vpkFile).use { zip ->
				val psf = psf
				val zipEntries = zip.entries()
				val gameId = psf["TITLE_ID"].toString()
				retGameId = gameId

				val entry = VitaOrganizerCache.entry(gameId)

				//try to find compressionlevel and vitaminversion or maiversion
				val paramsfo = zip.getEntry("sce_sys/param.sfo")
				val compressionLevel = if (paramsfo != null) paramsfo.method.toString() else ""

				var dumper = DumperNames.findDumperByShortName(if(psf["ATTRIBUTE"].toString() == "32768") "HB" else "UNKNOWN")
				if(dumper == DumperNames.UNKNOWN) {
					for (file in DumperModules.values()) {
						val suprx = zip.getEntry(file.file)
						if (suprx != null) {
							dumper = DumperNames.findDumperBySize(suprx.size)
						}
					}
				}

				val matcher = Regex("[cC]\\d").find(vpkFile.name)		//find one! string from C0 till C9, can be ambiguous ex. part of name or id
				val compressionlevel = matcher?.value ?: "unknown"

				println("For file ${vpkFile} Dumperversion : ${dumper} Compressionlevel : ${compressionlevel}")
				if (entry.dumperVersion.isEmpty()) {
					entry.dumperVersion = dumper.shortName.toString()
				}
				if (entry.compression.isEmpty()) {
					entry.compression = compressionlevel
				}

				if (!entry.icon0File.exists()) {
					entry.icon0File.writeBytes(zip.getInputStream(zip.getEntry("sce_sys/icon0.png")).readBytes())
				}
				if (!entry.paramSfoFile.exists()) {
					entry.paramSfoFile.writeBytes(paramSfoData)
				}
				if (entry.size <= 0L) {
					val uncompressedSize = ZipFile(vpkFile).entries().toList().map { it.size }.sum()
					entry.size = uncompressedSize
				}
				if (entry.permissionsString.isEmpty()) {
					val ebootBinData = zip.getBytes("eboot.bin")
					entry.permissions = EbootBin.hasExtendedPermissions(ebootBinData.open2("r"))
				}
				if (entry.pathFile.isEmpty() || !entry.pathFile.equals(vpkFile.absolutePath)) {
					entry.pathFile = vpkFile.absolutePath
				}
			}
		} catch (e: Throwable) {
			println("Error processing ${vpkFile.name}")
			e.printStackTrace()
		}
		return retGameId
	}


}