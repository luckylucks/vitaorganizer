package com.soywiz.vitaorganizer

import com.soywiz.util.*
import com.soywiz.vitaorganizer.ext.getBytes
import net.sf.sevenzipjbinding.IInArchive
import net.sf.sevenzipjbinding.SevenZip
import net.sf.sevenzipjbinding.SevenZipException
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream
import net.sf.sevenzipjbinding.simple.ISimpleInArchiveItem
import net.sf.sevenzipjbinding.util.ByteArrayStream
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.util.zip.ZipFile

class MaiDumpFile(val file: File) {

	fun cacheAndGetGameId(): String? {
		var retGameId:String? = null

		val psfFile = ArchiveEntry()
		val suprxFile = ArchiveEntry()
		val iconFile = ArchiveEntry()
		val ebootFile = ArchiveEntry()
		var fullSize = 0L

		var randomAccessFile: RandomAccessFile? = null
		var inArchive: IInArchive? = null
		try {
			randomAccessFile = RandomAccessFile(file, "r")
			inArchive = SevenZip.openInArchive(null, // autodetect archive type
				RandomAccessFileInStream(randomAccessFile))

			// Getting simple interface of the archive inArchive
			val simpleInArchive = inArchive!!.getSimpleInterface()

			//println("using file {$file}")
			//println("   Size   | Compr.Sz. | Filename")
			//println("----------+-----------+---------")

			for (item in simpleInArchive.archiveItems) {
				//println(String.format("%9s | %9s | %s", item.size, item.packedSize, item.path) )

				val path = item.path
				fullSize += item.size
				item.isFolder
				//scanning can be time consuming, looking for id and missing elements indexOfFirst { it.equals('\\') }
				if ( retGameId == null) {
					retGameId = when {
						item.isFolder -> path
						else -> path.substring( 0..(path.indexOf('\\')) )
					}
					println("Checking game for cache " + file + " with: " + retGameId)
					if ( !checkGame(retGameId) ) return retGameId
				}

				when {
					path.endsWith("\\sce_sys\\param.sfo") -> psfFile.readAll(item)
					path.endsWith("\\sce_sys\\icon0.png") -> iconFile.readAll(item)
					path.endsWith("\\mai_moe\\mai.surpx") -> suprxFile.readAll(item)
					path.endsWith("\\eboot.bin") -> ebootFile.readAll(item)
				}
			}
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

		val psf = PSF.read( MemoryStream2( psfFile.byteArray!! ) )

		val gameId = psf["TITLE_ID"].toString()
		retGameId = gameId

		val entry = VitaOrganizerCache.entry(gameId)

		var dumper = DumperNames.findDumperByShortName(if(psf["ATTRIBUTE"].toString() == "32768") "HB" else "UNKNOWN")
		if(dumper == DumperNames.UNKNOWN) {
			for (file in DumperModules.values()) {
				if (suprxFile.size != -1L) {
					dumper = DumperNames.findDumperBySize(suprxFile.size)
				}
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

		if (!entry.icon0File.exists()) {
			entry.icon0File.writeBytes( iconFile.byteArray!! )
		}
		if (!entry.paramSfoFile.exists()) {
			entry.paramSfoFile.writeBytes( psfFile.byteArray!! )
		}
		if (entry.size <= 0L) {
			entry.size = fullSize
		}
		if (entry.permissionsString.isEmpty()) {
			entry.permissions = EbootBin.hasExtendedPermissions(ebootFile.byteArray!!.open2("r"))
		}
		if (entry.pathFile.isEmpty() || !entry.pathFile.equals(file.absolutePath)) {
			entry.pathFile = file.absolutePath
		}
		entry.maydump = true

		return retGameId
	}

	//Check if game was completely scanned, extracted partial from archive can be extremely time consuming
	private fun checkGame(gameId: String?): Boolean {
		if (gameId == null) return false
		val entry = VitaOrganizerCache.entry(gameId)

		return entry.dumperVersion.isEmpty() || !entry.icon0File.exists() || !entry.paramSfoFile.exists() || entry.pathFile.isEmpty() || entry.permissionsString.isEmpty()
	}

}

class ArchiveEntry {
	public var fullName = ""
	public var size = -1L
	public var byteArray : ByteArray? = null

	fun readAll(item: ISimpleInArchiveItem) {
		fullName = item.path
		size = item.size

		val bas = ByteArrayStream(item.size.toInt())
		item.extractSlow( bas )
		byteArray = bas.bytes
		bas.close()
	}
}