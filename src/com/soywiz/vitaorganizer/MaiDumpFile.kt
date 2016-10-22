package com.soywiz.vitaorganizer

import com.soywiz.util.*
import net.sf.sevenzipjbinding.*
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream
import net.sf.sevenzipjbinding.simple.ISimpleInArchiveItem
import net.sf.sevenzipjbinding.util.ByteArrayStream
import java.awt.Color
import java.awt.Font
import java.awt.image.BufferedImage
import java.io.*
import java.util.*
import javax.imageio.ImageIO

class MaiDumpFile(val file: File) {

	fun cacheAndGetGameId(): String? {
		var retGameId:String? = null

		val entries = WantedArchiveEntries()
		var fullSize = 0L

		var randomAccessFile: RandomAccessFile? = null
		var inArchive: IInArchive? = null
		try {
			randomAccessFile = RandomAccessFile(file, "r")
			inArchive = SevenZip.openInArchive(null, // autodetect archive type
				RandomAccessFileInStream(randomAccessFile))

			val indices = ArrayList<Int>()

			for (i in 0..(inArchive.numberOfItems - 1) ) {
				val path = inArchive.getStringProperty(i, PropID.PATH)
				val size = inArchive.getStringProperty(i, PropID.SIZE).toLong()
				fullSize += size

				if ( retGameId == null) {
					retGameId = path.substring( 0..(path.indexOf('\\')) ).replace("\\", "")
					println("Checking game for cache " + file + " with: " + retGameId)

					//if empty first entry was probably the base directory of the game e.g. PCSE0001
					if (retGameId.isEmpty()) {
						retGameId = null	//reset value
					} else {
						if ( !checkGame(retGameId) ) return retGameId
						//else retGameId = ""		// -> retGameId == null fails next time
					}
				}

				when {
					path.endsWith("\\sce_sys\\param.sfo") -> indices.add(i)
					path.endsWith("\\sce_sys\\icon0.png") -> indices.add(i)
					path.endsWith("\\mai_moe\\mai.suprx") -> indices.add(i)
					path.endsWith("\\eboot.bin") -> indices.add(i)
				}
			}

			//using filtering instead of simpleInArchive access, but does not seem to fasten the process
			inArchive.extract(indices.toIntArray(), false, MyExtractCallback(inArchive, entries))

		} catch (e: Exception) {
			System.err.println("Error occurs: " + e)
		} finally {
			if (inArchive != null) {
				try {
					inArchive!!.close()
				} catch (e: SevenZipException) {
					System.err.println("Error closing archive: " + e.printStackTrace())
				}

			}
			if (randomAccessFile != null) {
				try {
					randomAccessFile.close()
				} catch (e: IOException) {
					System.err.println("Error closing file: " + e.printStackTrace())
				}

			}
		}

		val psf = PSF.read( MemoryStream2( entries.psfFile.byteArray!! ) )

		val gameId = psf["TITLE_ID"].toString()
		var readedGameId = gameId
		var special = ""

		//all updates, maybe all dlc? should have a PSF file, if not PSF.read (a few lines above) already crashed
		//no icons for dlc, updates could have an icon, but probably not
		if ( retGameId?.contains(readedGameId) ?: false ) {
			if ( !retGameId!!.equals(readedGameId) ) {
				//both ids are not equal but similar, e.g.
				//PCSE0001 and PCSE0001_addc for dlc
				if (retGameId!!.contains("_addc")) {
					readedGameId = gameId + CachedGameEntry.SpecialValue.DLC.prettyPrint
					special = CachedGameEntry.SpecialValue.DLC.short
					entries.iconFile.byteArray = MakeImages.makeTextImage("DLC", 2, 40, 30)
				}
				//updates??, not sure if they changed the format, or are from vitamin or something else
				if (retGameId!!.contains("_patch")) {
					readedGameId = gameId + CachedGameEntry.SpecialValue.UPDATE.prettyPrint
					special = CachedGameEntry.SpecialValue.UPDATE.short
					entries.iconFile.byteArray = MakeImages.makeTextImage("Update", 2, 40, 20)
					return null
				}
			}
		}

		val entry = VitaOrganizerCache.entry(readedGameId)

		var dumper = DumperNames.findDumperByShortName(if(psf["ATTRIBUTE"].toString() == "32768") "HB" else "UNKNOWN")
		if(dumper == DumperNames.UNKNOWN) {
			if ( (entries.suprxFile?.size ?: 0L) > 0L) {
				val sha1 = DumperNames.calculateSHA1( entries.suprxFile.byteArray!! )
				//println("found sha1: $sha1")
				dumper = DumperNames.findDumperBySHA1(sha1, entries.suprxFile.fullName)
			}
		}

		val matcher = Regex("[cC]\\d").find(file.name)		//find one! string from C0 till C9, can be ambiguous ex. part of name or id
		val compressionlevel = matcher?.value ?: "unknown"

		println("For file ${file} Dumperversion : ${dumper} Compressionlevel : ${compressionlevel}")
		if (entry.dumperVersion.isEmpty()) {
			entry.dumperVersion = dumper.shortName.toString()
		}
		if (entry.compression.isEmpty()) {
			entry.compression = compressionlevel
		}

		if (!entry.icon0File.exists() && entries.iconFile.byteArray != null) {
			entry.icon0File.writeBytes( entries.iconFile.byteArray!! )
		}
		if (!entry.paramSfoFile.exists()) {
			entry.paramSfoFile.writeBytes( entries.psfFile.byteArray!! )
		}
		if (entry.size <= 0L) {
			entry.size = fullSize
		}
		if (entry.permissionsString.isEmpty() && !special.equals(CachedGameEntry.SpecialValue.DLC.short)) {
			entry.permissions = EbootBin.hasExtendedPermissions(entries.ebootFile.byteArray!!.open2("r"))
		}
		if (entry.pathFile.isEmpty() || !entry.pathFile.equals(file.absolutePath)) {
			entry.pathFile = file.absolutePath
		}
		entry.maydump = true
		entry.special = special

		return readedGameId
	}

	//Check if game was completely scanned, extracted partial from archive can be extremely time consuming
	private fun checkGame(gameId: String?): Boolean {
		if (gameId == null) return false
		val entry = VitaOrganizerCache.entry(gameId)

		return entry.dumperVersion.isEmpty() || !entry.icon0File.exists() || !entry.paramSfoFile.exists() || entry.pathFile.isEmpty() || entry.permissionsString.isEmpty()
	}

}

class WantedArchiveEntries {
	val psfFile = ArchiveEntry()
	val suprxFile = ArchiveEntry()
	val iconFile = ArchiveEntry()
	val ebootFile = ArchiveEntry()

	override fun toString(): String {
		return "WantedArchiveEntries:\n" +
			"psfFile : $psfFile\n" +
			"suprxFile : $suprxFile\n" +
			"iconFile : $iconFile\n" +
			"ebootFile : $ebootFile"
	}
}

class ArchiveEntry {
	var fullName = ""
	var size = -1L
	var byteArray : ByteArray? = null

	override fun toString(): String {
		return "Path: '$fullName' Size: $size"
	}

	fun readAll(item: ISimpleInArchiveItem) {
		fullName = item.path
		size = item.size

		val bas = ByteArrayStream(item.size.toInt())
		item.extractSlow( bas )
		byteArray = bas.bytes
		bas.close()
	}

	fun readAll(path: String, size: Long, data: ByteArray) {
		if (fullName.isEmpty()) {
			//first call
			this.fullName = path
			this.size = size
			this.byteArray = data
		} else {
			//another datachunk for the file
			val oldSize = this.byteArray?.size ?: 0		//should not be empty
			val chunkSize = data.size
			this.byteArray = Arrays.copyOf(this.byteArray, oldSize + chunkSize)
			System.arraycopy(data, 0, this.byteArray, oldSize, chunkSize)
		}
	}
}

object MakeImages {
	fun makeTextImage(text: String, x: Int, y: Int, fontSize: Int) : ByteArray{
		val buffer = BufferedImage(64, 64, BufferedImage.TYPE_4BYTE_ABGR)
		val g2d = buffer.createGraphics()

		g2d.color = Color.BLACK
		g2d.font = Font(Font.SERIF, Font.BOLD, fontSize)
		g2d.drawString(text, x, y)

		g2d.dispose()
		val baos = ByteArrayOutputStream()
		ImageIO.write(buffer, "png", baos)
		return baos.toByteArray()
	}
}

private class MyExtractCallback(private val inArchive: IInArchive, val output: WantedArchiveEntries) : IArchiveExtractCallback {

	@Throws(SevenZipException::class)
	override fun getStream(index: Int, extractAskMode: ExtractAskMode): ISequentialOutStream {
		return ISequentialOutStream { data ->
			try {
				val path = inArchive.getStringProperty(index, PropID.PATH)
				val size = inArchive.getStringProperty(index, PropID.SIZE).toLong()
				val partialSize = data.size
				//println("Maidump file : $path full Size : $size partial Size : $partialSize")
				when {
					path.endsWith("\\sce_sys\\param.sfo") -> output.psfFile.readAll(path, size, data)
					path.endsWith("\\sce_sys\\icon0.png") -> output.iconFile.readAll(path, size, data)
					path.endsWith("\\mai_moe\\mai.suprx") -> output.suprxFile.readAll(path, size, data)
					path.endsWith("\\eboot.bin") -> output.ebootFile.readAll(path, size, data)
				}
			} catch (e: IOException) {
				e.printStackTrace()
			} finally {
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
