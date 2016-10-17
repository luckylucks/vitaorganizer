package com.soywiz.vitaorganizer.tasks

import com.soywiz.vitaorganizer.CachedGameEntry
import com.soywiz.vitaorganizer.Texts
import com.soywiz.vitaorganizer.VitaOrganizer
import com.soywiz.vitaorganizer.VitaOrganizerSettings
import net.sf.sevenzipjbinding.*
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile
import javax.swing.JOptionPane

class UnpackMaiDumpQCMA(vitaOrganizer: VitaOrganizer, val entry: CachedGameEntry) : VitaTask(vitaOrganizer) {
	override fun perform() {
		val file = entry.vpkLocalFile!!
		val directory = VitaOrganizerSettings.qcmaDirectory
		val option = JOptionPane.showConfirmDialog(vitaOrganizer,
			"Current directory for QCMA: " + directory
			+ "\nThe game will be extracted into the directory."
			+ "\nAfter extracting it can be transferred with QCMA."
			+ "\nManual delete after use."
			+ "\nExtract: " + file.toString() + "?",
			"Warning using QCMA with MaiDump",
			JOptionPane.WARNING_MESSAGE,
			JOptionPane.YES_NO_OPTION);

		if (option == JOptionPane.OK_OPTION) {
			status(Texts.format("UNPACK_QCMA_STATUS_GAME", "game" to entry.vpkLocalPath))
			SevenZipJBindingExtractor().extract(file.toString(), directory)
			status(Texts.format("STEP_DONE"))
		}
	}
}

//copied from https://sourceforge.net/p/sevenzipjbind/discussion/757964/thread/b64a36fb/#9b6d
class SevenZipJBindingExtractor {

	@Throws(SevenZipException::class, IOException::class)
	fun extract(file: String, extractPath: String) {
		var inArchive: IInArchive? = null
		var randomAccessFile: RandomAccessFile? = null
		try {
			randomAccessFile = RandomAccessFile(File(file), "r")
			inArchive = SevenZip.openInArchive(null, RandomAccessFileInStream(randomAccessFile))
			inArchive!!.extract(null, false, MyExtractCallback(inArchive, extractPath))
		} finally {
			if (inArchive != null) {
				inArchive.close()
			}
			if (randomAccessFile != null) {
				randomAccessFile.close()
			}
		}
	}

	private class MyExtractCallback(private val inArchive: IInArchive, extractPath: String) : IArchiveExtractCallback {
		private val extractPath: String

		init {
			var extractPath = extractPath
			if (!extractPath.endsWith("\\") || !extractPath.endsWith("/")) {
				extractPath = extractPath + "\\"
			}
			this.extractPath = extractPath
		}

		@Throws(SevenZipException::class)
		override fun getStream(index: Int, extractAskMode: ExtractAskMode): ISequentialOutStream {
			return ISequentialOutStream { data ->
				val filePath = inArchive.getStringProperty(index, PropID.PATH)
				var fos: FileOutputStream? = null
				try {
					val path = File(extractPath + filePath)

					if (!path.parentFile.exists()) {
						path.parentFile.mkdirs()
					}

					if (!path.exists()) {
						path.createNewFile()
					}
					fos = FileOutputStream(path, true)
					fos.write(data)
				} catch (e: IOException) {
					e.printStackTrace()
				} finally {
					try {
						if (fos != null) {
							fos.flush()
							fos.close()
						}
					} catch (e: IOException) {
						e.printStackTrace()
					}

				}
				data.size
			}
		}

		@Throws(SevenZipException::class)
		override fun prepareOperation(extractAskMode: ExtractAskMode) {
		}

		@Throws(SevenZipException::class)
		override fun setOperationResult(extractOperationResult: ExtractOperationResult) {
		}

		@Throws(SevenZipException::class)
		override fun setCompleted(completeValue: Long) {
		}

		@Throws(SevenZipException::class)
		override fun setTotal(total: Long) {
		}
	}
}