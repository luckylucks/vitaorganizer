package com.soywiz.vitaorganizer.tasks

import com.soywiz.vitaorganizer.*
import java.io.File

class UpdateFileListTask(vitaOrganizer: VitaOrganizer) : VitaTask(vitaOrganizer) {
	override fun perform() {
		synchronized(vitaOrganizer.VPK_GAME_IDS) {
			vitaOrganizer.VPK_GAME_IDS.clear()
		}
		status(Texts.format("STEP_ANALYZING_FILES", "folder" to VitaOrganizerSettings.vpkFolder))

		val MAX_SUBDIRECTORY_LEVELS = 2

		fun listVpkFiles(folder: File, level: Int = 0): List<File> {
			val out = arrayListOf<File>()
			if (level > MAX_SUBDIRECTORY_LEVELS) return out
			for (child in folder.listFiles()) {
				if (child.isDirectory) {
					out += listVpkFiles(child, level = level + 1)
				} else {
					if (child.extension.toLowerCase() == "vpk") out += child
				}
			}
			return out
		}

		val vpkFiles = listVpkFiles(File(VitaOrganizerSettings.vpkFolder))

		for (vpkFile in vpkFiles) {
			val ff = VpkFile(vpkFile)
			val gameId = ff.cacheAndGetGameId()
			if (gameId != null) {
				synchronized(vitaOrganizer.VPK_GAME_IDS) {
					vitaOrganizer.VPK_GAME_IDS += gameId
				}
			}
		}

		fun listMaiDumpFiles(folder: File, level: Int = 0): List<File> {
				val out = arrayListOf<File>()
				if (level > MAX_SUBDIRECTORY_LEVELS) return out
				for (child in folder.listFiles()) {
					if (child.isDirectory) {
						out += listVpkFiles(child, level = level + 1)
					} else {
						val acceptableTypes = arrayOf("zip", "7z", "rar")
						if (child.extension.toLowerCase() in acceptableTypes ) out += child
					}
				}
				return out
			}

		val maiDumpFiles = listMaiDumpFiles(File(VitaOrganizerSettings.vpkFolder))
		for (maiDumpFile in maiDumpFiles) {
			val ff = MaiDumpFile(maiDumpFile)
			println("Maidumping : " + maiDumpFile.path)
			try {
				status(Texts.format("UPDATEFILELISTTASK", "current" to maiDumpFile))
				val gameId = ff.cacheAndGetGameId()
				System.gc()			//extracting archives can exhaust memory (less probable)
									//or filechunks vary extremely for different types of archives
				if (gameId != null) {
					synchronized(vitaOrganizer.VPK_GAME_IDS) {
						vitaOrganizer.VPK_GAME_IDS += gameId
					}
				}
			} catch (e: Exception) {
				println("Error processing in UpdateFileListTask Maidump: " + maiDumpFile)
				e.printStackTrace()
			}
			//Thread.sleep(200L)
		}
		status(Texts.format("STEP_DONE"))
		vitaOrganizer.updateEntries()
	}
}