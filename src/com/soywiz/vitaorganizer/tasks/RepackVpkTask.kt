package com.soywiz.vitaorganizer.tasks

import com.soywiz.vitaorganizer.FileSize
import com.soywiz.vitaorganizer.CachedGameEntry
import com.soywiz.vitaorganizer.Texts
import com.soywiz.vitaorganizer.VitaOrganizer
import java.io.File
import java.io.FileOutputStream
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

class RepackVpkTask(vitaOrganizer: VitaOrganizer, val entry: CachedGameEntry, val compression: Int = Deflater.BEST_COMPRESSION, val setSecure: Boolean? = null) : VitaTask(vitaOrganizer) {
	override fun perform() {
		status(Texts.format("STEP_REPACKING_VPK"))
		val file = entry.vpkLocalFile!!
		val tempFile = File("${file.absolutePath}.temp")
		val tempFile2 = File("${file.absolutePath}.temp2")

		tempFile.delete()
		tempFile2.delete()

		val temp = ByteArray(10 * 1024 * 1024)

		ZipFile(file).use { zip ->
			FileOutputStream(tempFile).use { out ->
				ZipOutputStream(out).use { zout ->
					zout.setLevel(compression)
					val entries = zip.entries().toList().distinctBy { it.name }
					var currentSize = 0L
					val totalSize = entries.map { it.size }.sum()

					for ((index, e) in entries.withIndex()) {
						fun updateStatus() {
							status(Texts.format(
								"STEP_REPACKING_ENTRY",
								"current" to (index + 1), "total" to entries.size,
								"currentSize" to FileSize.toString(currentSize), "totalSize" to FileSize.toString(totalSize)
							))
							progress(index, entries.size)
						}

						updateStatus()
						zout.putNextEntry(ZipEntry(e.name))
						if (e.name == "eboot.bin" && setSecure != null) {
							val full = zip.getInputStream(e).readBytes()
							if (setSecure) {
								full[0x80] = (full[0x80].toInt() and 1.inv()).toByte() // Remove bit 0
								full[0x80] = (full[0x80].toInt() or 2).toByte() // Set bit 1?
							} else {
								full[0x80] = (full[0x80].toInt() or 1).toByte() // Set bit 0
							}
							zout.write(full)
							currentSize += full.size
						} else {
							zip.getInputStream(e).use {
								var localSize = 0L
								while (it.available() > 0) {
									val bytes = it.read(temp)
									if (bytes <= 0) break
									zout.write(temp, 0, bytes)
									currentSize += bytes
									localSize += bytes
									if (localSize >= 1 * 1024 * 1024) {
										localSize -= 1 * 1024 * 1024
										updateStatus()
									}
								}
							}
							updateStatus()
						}

						zout.closeEntry()
					}
				}
			}
		}
		status(Texts.format("STEP_DONE"))

		//Thread.sleep(300L)

		println("Renames + deletes:")
		println(file.renameTo(tempFile2))
		println(tempFile.renameTo(file))
		println(tempFile2.delete())
		println(entry.entry.delete()) // flush this info!

		vitaOrganizer.updateFileList()
	}
}