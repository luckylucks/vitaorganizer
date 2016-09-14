package com.soywiz.vitaorganizer.tasks

import com.soywiz.util.open2
import com.soywiz.vitaorganizer.*
import com.soywiz.util.DumperNamesHelper
import com.soywiz.util.DumperModules
import com.soywiz.util.DumperNames
import com.soywiz.vitaorganizer.ext.getBytes
import java.io.File
import java.util.zip.ZipFile

class UpdateFileListTask : VitaTask() {
	override fun perform() {
		synchronized(VitaOrganizer.VPK_GAME_IDS) {
			VitaOrganizer.VPK_GAME_IDS.clear()
		}
		val vpkFiles = File(VitaOrganizerSettings.vpkFolder).listFiles().filter { it.name.toLowerCase().endsWith(".vpk") }
		status(Texts.format("STEP_ANALYZING_FILES", "folder" to VitaOrganizerSettings.vpkFolder))
		var count = 0
		for (vpkFile in File(VitaOrganizerSettings.vpkFolder).listFiles().filter { it.name.toLowerCase().endsWith(".vpk") }) {
			//println(vpkFile)
			status(Texts.format("STEP_ANALYZING_ITEM", "name" to vpkFile.name, "current" to count + 1, "total" to vpkFiles.size))
			try {
				ZipFile(vpkFile).use { zip ->
					val paramSfoData = zip.getBytes("sce_sys/param.sfo")

					val psf = PSF.read(paramSfoData.open2("r"))
					val gameId = psf["TITLE_ID"].toString()

					val entry = VitaOrganizerCache.entry(gameId)

					//try to find compressionlevel and vitaminversion or maiversion
					val paramsfo = zip.getEntry("sce_sys/param.sfo")
					val compressionLevel = if (paramsfo != null) paramsfo.method.toString() else ""

					var dumper = DumperNames.UNKNOWN
					for ( file in DumperModules.values() ) {
						val suprx = zip.getEntry(file.file)
						if (suprx != null) {
							dumper = DumperNamesHelper().findDumperBySize(suprx.size)
						}
					}

					println("For file ${vpkFile} Compressionslevel : ${compressionLevel} Dumperversion : ${dumper}")
					if (!entry.compressionFile.isNotEmpty()) {
						entry.compressionFile = compressionLevel.toString()
					}
					if (!entry.dumperVersionFile.isNotEmpty()) {
						entry.dumperVersionFile = dumper.shortName.toString()
					}

					if (!entry.icon0File.exists()) {
						entry.icon0File.writeBytes(zip.getInputStream(zip.getEntry("sce_sys/icon0.png")).readBytes())
					}
					if (!entry.paramSfoFile.exists()) {
						entry.paramSfoFile.writeBytes(paramSfoData)
					}
					if (!entry.sizeFile.isNotEmpty()) {
						val uncompressedSize = ZipFile(vpkFile).entries().toList().map { it.size }.sum()
						entry.sizeFile = uncompressedSize.toString()
					}
					if (!entry.permissionsFile.isNotEmpty()) {
						val ebootBinData = zip.getBytes("eboot.bin")
						entry.permissionsFile = EbootBin.hasExtendedPermissions(ebootBinData.open2("r")).toString()
					}
					entry.pathFile = vpkFile.absolutePath
					synchronized(VitaOrganizer.VPK_GAME_IDS) {
						VitaOrganizer.VPK_GAME_IDS += gameId
					}
					//getGameEntryById(gameId).inPC = true
				}
			} catch (e: Throwable) {
				println("Error processing ${vpkFile.name}")
				e.printStackTrace()
			}
		}
		status(Texts.format("STEP_DONE"))
		VitaOrganizer.updateEntries()
	}
}