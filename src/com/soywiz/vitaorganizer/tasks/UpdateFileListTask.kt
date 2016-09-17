package com.soywiz.vitaorganizer.tasks

import com.soywiz.util.open2
import com.soywiz.vitaorganizer.*
import com.soywiz.util.DumperModules
import com.soywiz.util.DumperNames
import com.soywiz.vitaorganizer.ext.getBytes
import java.io.File
import java.util.regex.Pattern
import java.util.zip.ZipFile

class UpdateFileListTask : VitaTask() {
	override fun perform() {
		synchronized(VitaOrganizer.VPK_GAME_IDS) {
			VitaOrganizer.VPK_GAME_IDS.clear()
		}
		val vpkFiles = File(VitaOrganizerSettings.vpkFolder).listFiles().filter { it.name.toLowerCase().endsWith(".vpk") }
		status(Texts.format("STEP_ANALYZING_FILES", "folder" to VitaOrganizerSettings.vpkFolder))
		for ((index, vpkFile) in File(VitaOrganizerSettings.vpkFolder).listFiles().filter { it.name.toLowerCase().endsWith(".vpk") }.withIndex()) {
			//println(vpkFile)
			status(Texts.format("STEP_ANALYZING_ITEM", "name" to vpkFile.name, "current" to index + 1, "total" to vpkFiles.size))
			try {
				ZipFile(vpkFile).use { zip ->
					val paramSfoData = zip.getBytes("sce_sys/param.sfo")

					val psf = PSF.read(paramSfoData.open2("r"))
					val gameId = psf["TITLE_ID"].toString()

					val entry = VitaOrganizerCache.entry(gameId)

					//try to find vitaminversion or maiversion
					var dumper = DumperNames.UNKNOWN
					for ( file in DumperModules.values() ) {
						val suprx = zip.getEntry(file.file)
						if (suprx != null) {
							dumper = DumperNames.findDumperBySize(suprx.size, file)
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

					synchronized(VitaOrganizer.VPK_GAME_IDS) {
						VitaOrganizer.VPK_GAME_IDS += gameId
					}
					//getGameEntryById(gameId).inPC = true
				}
			} catch (e: Throwable) {
				println("Error processing ${vpkFile.name}")
				e.printStackTrace()
			}
			//Thread.sleep(200L)
		}
		status(Texts.format("STEP_DONE"))
		VitaOrganizer.updateEntries()
	}
}