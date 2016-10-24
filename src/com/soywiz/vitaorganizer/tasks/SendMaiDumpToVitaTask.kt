package com.soywiz.vitaorganizer.tasks

import com.soywiz.vitaorganizer.*
import javax.swing.SwingUtilities

class SendMaiDumpToVitaTask(vitaOrganizer: VitaOrganizer, val entry: CachedGameEntry) : VitaTask(vitaOrganizer) {
	fun performBase() {

		status(Texts.format("STEP_SENDING_GAME", "id" to entry.id))
		//val zip = ZipFile(entry.vpkFile)
		try {
			PsvitaDevice.uploadGameMaiDump(entry.id, entry.vpkLocalFile!!, this, { status ->
				//println("$status")
				status(Texts.format("STEP_SENDING_GAME_UPLOADING", "id" to entry.id, "fileRange" to status.fileRange, "sizeRange" to status.sizeRange, "speed" to status.speedString))
			})
			//statusLabel.text = "Processing game ${vitaGameCount + 1}/${vitaGameIds.size} ($gameId)..."
		} catch (e: Throwable) {
			error(e.toString())
		}
		status(Texts.format("SENT_GAME_DATA", "id" to entry.id))
	}

	override fun perform() {
		performBase()
		status(Texts.format("GAME_SENT_SUCCESSFULLY", "id" to entry.id))
	}

	fun setSuccess() {
		status(Texts.format("GAME_SENT_SUCCESSFULLY", "id" to entry.id))
	}

}