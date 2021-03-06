package com.soywiz.vitaorganizer

import com.soywiz.util.ubyte
import com.soywiz.vitaorganizer.tasks.SendMaiDumpToVitaTask
import it.sauronsoftware.ftp4j.*
import net.sf.sevenzipjbinding.*
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream
import java.io.*
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import java.util.*
import java.util.concurrent.BrokenBarrierException
import java.util.concurrent.CyclicBarrier
import java.util.zip.ZipFile
import javax.swing.JOptionPane

object PsvitaDevice {
	fun checkAddress(ip: String, port: Int = 1337): Boolean {
		try {
			val sock = Socket()
			sock.connect(InetSocketAddress(ip, port), 3000)
			sock.close()
			return true
		} catch (e: Throwable) {
			return false
		}
	}

	fun discoverIp(port: Int = 1337): List<String> {
		val ips = NetworkInterface.getNetworkInterfaces().toList().flatMap { it.inetAddresses.toList() }
		val ips2 = ips.map { it.hostAddress }.filter { it.startsWith("192.") }
		val ipEnd = Regex("\\.(\\d+)$")
		val availableIps = arrayListOf<String>()
		var rest = 256
		for (baseIp in ips2) {
			for (n in 0 until 256) {
				Thread {
					val ip = baseIp.replace(ipEnd, "\\.$n")
					if (checkAddress(ip, port)) {
						availableIps += ip
					}
					rest--
				}.start()
			}
		}
		while (rest > 0) {
			//println(rest)
			Thread.sleep(20L)
		}
		//println(availableIps)
		return availableIps
	}

	val ftp = FTPClient().apply {
		type = FTPClient.TYPE_BINARY
	}

	fun setFtpPromoteTimeouts() {
		//ftp.connector.setCloseTimeout(20)
		//ftp.connector.setReadTimeout(240) // PROM could take a lot of time!
		//ftp.connector.setConnectionTimeout(120)
	}

	fun resetFtpTimeouts() {
		ftp.connector.setCloseTimeout(20)
		ftp.connector.setReadTimeout(240) // PROM could take a lot of time!
		ftp.connector.setConnectionTimeout(120)

		//ftp.connector.setCloseTimeout(2)
		//ftp.connector.setReadTimeout(2)
		//ftp.connector.setConnectionTimeout(2)
	}

	init {
		resetFtpTimeouts()
	}

	//init {
	//ftp.sendCustomCommand()
	//}

	private fun connectedFtp(): FTPClient {
		val ip = VitaOrganizerSettings.lastDeviceIp
		val port = try {
			VitaOrganizerSettings.lastDevicePort.toInt()
		} catch (t: Throwable) {
			1337
		}



		retries@for (n in 0 until 5) {
			if (!ftp.isConnected()) {
				println("Connecting to ftp $ip:$port...")
				ftp.connect(ip, port)
				ftp.login("", "")
				if (ftp.isConnected()) {
					println("Connected")
				} else {
					println("Could not connect ($n)");
					break@retries
				}
			}
			try {
				ftp.noop()
				break@retries
			} catch (e: IOException) {
				ftp.disconnect(false)
			}
		}
		return ftp
	}

	fun disconnectFromFtp(): Boolean {
		if (ftp.isConnected())
			ftp.disconnect(false);

		return !ftp.isConnected();
	}

	fun getGameIds() = connectedFtp().list("/ux0:/app").filter { i -> i.type == it.sauronsoftware.ftp4j.FTPFile.TYPE_DIRECTORY }.map { File(it.name).name }

	fun getGameFolder(id: String) = "/ux0:/app/${File(id).name}"

	fun downloadSmallFile(path: String): ByteArray {
		try {
			if (connectedFtp().fileSize(path) == 0L) {
				return byteArrayOf()
			}
		} catch (e: Throwable) {
			return byteArrayOf()
		}

		val file = File.createTempFile("vita", "download")
		try {
			connectedFtp().download(path, file)
			return file.readBytes()
		} catch (e: FTPException) {
			e.printStackTrace()
		} catch (e: Throwable) {
			e.printStackTrace()
		} finally {
			//e.printStackTrace()
			file.delete()
		}
		return byteArrayOf()
	}

	fun getParamSfo(id: String): ByteArray = downloadSmallFile("${getGameFolder(id)}/sce_sys/param.sfo")
	fun getGameIcon(id: String): ByteArray {
		val result = downloadSmallFile("${getGameFolder(id)}/sce_sys/icon0.png")
		return result
	}

	fun downloadEbootBin(id: String): ByteArray = downloadSmallFile("${getGameFolder(id)}/eboot.bin")

	fun getParamSfoCached(id: String): ByteArray {
		val file = VitaOrganizerCache.entry(id).paramSfoFile
		if (!file.exists()) file.writeBytes(getParamSfo(id))
		return file.readBytes()
	}

	fun getGameIconCached(id: String): ByteArray {
		val file = VitaOrganizerCache.entry(id).icon0File
		if (!file.exists()) file.writeBytes(getGameIcon(id))
		return file.readBytes()
	}

	fun getFolderSize(path: String, folderSizeCache: HashMap<String, Long> = hashMapOf<String, Long>()): Long {
		return folderSizeCache.getOrPut(path) {
			var out = 0L
			val ftp = connectedFtp()
			try {
				for (file in ftp.list(path)) {
					//println("$path/${file.name}: ${file.size}")
					if (file.type == FTPFile.TYPE_DIRECTORY) {
						out += getFolderSize("$path/${file.name}", folderSizeCache)
					} else {
						out += file.size
					}
				}
			} catch (e: Throwable) {
				e.printStackTrace()
			}
			out
		}
	}

	fun getGameSize(id: String, folderSizeCache: HashMap<String, Long> = hashMapOf<String, Long>()): Long {
		return getFolderSize(getGameFolder(id), folderSizeCache)
	}

	class Status() {
		var startTime: Long = 0L
		var currentFile: Int = 0
		var totalFiles: Int = 0
		var currentSize: Long = 0L
		var totalSize: Long = 0L
		val elapsedTime: Int get() = (System.currentTimeMillis() - startTime).toInt()
		val speed: Double get() {
			return if (elapsedTime == 0) 0.0 else currentSize.toDouble() / (elapsedTime.toDouble() / 1000.0)
		}

		val currentSizeString: String get() = FileSize.toString(currentSize)
		val totalSizeString: String get() = FileSize.toString(totalSize)

		val speedString: String get() = FileSize.toString(speed.toLong()) + "/s"

		val fileRange: String get() = "$currentFile/$totalFiles"
		val sizeRange: String get() = "$currentSizeString/$totalSizeString"
	}

	val createDirectoryCache = hashSetOf<String>()

	fun createDirectories(_path: String, createDirectoryCache: HashSet<String> = PsvitaDevice.createDirectoryCache) {
		val path = _path.replace('\\', '/')
		val parent = File(path).parent
		if (parent != "" && parent != null) {
			createDirectories(parent, createDirectoryCache)
		}
		if (path !in createDirectoryCache) {
			println("Creating directory $path...")
			createDirectoryCache.add(path)
			try {
				connectedFtp().createDirectory(path)
			} catch (e: IOException) {
				throw e
			} catch (e: FTPException) {
				e.printStackTrace()
			} catch (e: Throwable) {
				e.printStackTrace()
			}
		}
	}

	fun uploadGame(id: String, zip: ZipFile, filter: (path: String) -> Boolean = { true }, updateStatus: (Status) -> Unit = { }) {
		val base = getGameFolder(id)

		val status = Status()

		val unfilteredEntries = zip.entries().toList()

		val filteredEntries = unfilteredEntries.filter { filter(it.name) }

		status.startTime = System.currentTimeMillis()

		status.currentFile = 0
		status.totalFiles = filteredEntries.size

		status.currentSize = 0L
		status.totalSize = filteredEntries.map { it.size }.sum()

		for (entry in filteredEntries) {
			val normalizedName = entry.name.replace('\\', '/')
			val vname = "$base/$normalizedName"
			val directory = File(vname).parent.replace('\\', '/')
			val startSize = status.currentSize
			if (!entry.isDirectory) {
				createDirectories(directory)

				print("Writting $vname...")
				try {
					connectedFtp().upload(vname, zip.getInputStream(entry), 0L, 0L, object : FTPDataTransferListener {
						override fun started() {
							print("started...")
						}

						override fun completed() {
							print("completed!")
							updateStatus(status)

							//untested
							//if(status.currentSize != status.totalSize) {
							//    println("$vname mismatch transfered size. $status.currentSize != $status.totalSize")
							//
						}

						override fun aborted() {
							print("aborted!")
						}

						override fun transferred(size: Int) {
							status.currentSize += size
							updateStatus(status)
						}

						override fun failed() {
							print("failed!")
						}
					})
				} catch (e: FTPException) {
					e.printStackTrace()
					throw FileNotFoundException("Can't upload file $vname")
				}
				println("")
			}
			status.currentSize = startSize + entry.size
			status.currentFile++
			updateStatus(status)
		}

		println("DONE. Now package should be promoted!")
	}

	fun uploadGameMaiDump(id: String, file: File, task: SendMaiDumpToVitaTask, updateStatus: (Status) -> Unit = { }) {
		val status = Status()
		val fileZeroSizes = ArrayList<String>()

		var randomAccessFile: RandomAccessFile? = null
		var inArchive: IInArchive? = null
		try {
			randomAccessFile = RandomAccessFile(file, "r")
			inArchive = SevenZip.openInArchive(null, // autodetect archive type
				RandomAccessFileInStream(randomAccessFile))

			var fullSize = 0L
			var fullCount = 0

			for (i in 0..(inArchive.numberOfItems - 1) ) {
				val size = inArchive.getStringProperty(i, PropID.SIZE).toLong()
				val dir = inArchive.getStringProperty(i, PropID.IS_FOLDER)
				fullSize += size

				if (dir.equals("-")) {	//dir "+" means directory, "-" -> file
					fullCount++
					if (size == 0L) {   //empty files aren't called in IArchiveExtractCallback
						val path = inArchive.getStringProperty(i, PropID.PATH)
						fileZeroSizes.add(path)
					}
				}
			}

			status.totalFiles = fullCount
			status.totalSize = fullSize
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

		status.currentFile = 0
		status.currentSize = 0L
		status.startTime = System.currentTimeMillis()

		try {
			for(filePath in fileZeroSizes) {
				try {
					//println("Upload empty file $filePath")
					val normalizedName = filePath.replace('\\', '/')
					val vname = "/ux0:/mai/$normalizedName"

					uploadFile(vname, ByteArray(0))
					status.currentFile++
					updateStatus(status)
				} catch (e: Exception) {
					e.printStackTrace()	//probably FTPException code=550, Could not create the directory.
				}
			}
			randomAccessFile = RandomAccessFile(file, "r")
			inArchive = SevenZip.openInArchive(null, RandomAccessFileInStream(randomAccessFile))
			inArchive!!.extract(null, false, MyExtractFTPCallback(inArchive, status, task, updateStatus))
		} finally {
			if (inArchive != null) {
				inArchive.close()
			}
			if (randomAccessFile != null) {
				randomAccessFile.close()
			}
		}

		println("DONE. MaiDump Transfer completed")
	}

	fun uploadFile(path: String, data: ByteArray, updateStatus: (Status) -> Unit = { }) {
		val status = Status()
		createDirectories(File(path).parent)
		status.startTime = System.currentTimeMillis()
		status.currentFile = 0
		status.totalFiles = 1
		status.totalSize = data.size.toLong()
		updateStatus(status)
		connectedFtp().upload(path, ByteArrayInputStream(data), 0L, 0L, object : FTPDataTransferListener {
			override fun started() {
			}

			override fun completed() {
			}

			override fun aborted() {
			}

			override fun transferred(size: Int) {
				status.currentSize += size
				updateStatus(status)
			}

			override fun failed() {
			}
		})
		status.currentFile++
		updateStatus(status)
	}

	fun removeFile(path: String) {
		try {
			connectedFtp().deleteFile(path)
		} catch (e: Throwable) {
			println("Can't delete $path")
			e.printStackTrace()
		}
	}

	fun promoteVpk(vpkPath: String, displayErrors: Boolean = true): Boolean {

		if (vpkPath.isNullOrEmpty()) {
			println("NULL or empty promoting vpk path specified!")
			return false
		}

		println("Promoting: 'PROM $vpkPath'")

		try {
			resetFtpTimeouts()
			val reply: FTPReply = connectedFtp().sendCustomCommand("PROM $vpkPath")

			if (reply.getCode() == 502) {
				println("PROM command is not supported by the server")
				if (displayErrors) error("The FTP server does not support promoting/installing VPK files, hence aborting!")
				return false
			} else if (reply.getCode() == 500) {
				println("ERROR PROMOTING $vpkPath")
				if (displayErrors) error("The FTP server could not promote/install the VPK file due to an install error, hence aborting!")
				return false
			} else if (reply.getCode() != 200) {
				println("Unknown error. Server response: $reply.toString()!")
				if (displayErrors) error("An unknown error occured. Details:\n$reply.toString()")
				return false
			}

			//vitashell replies with code 200 for PROMOTING OK, otherwise 500
			val isOK: Boolean = reply.getCode() == 200
			if (isOK)
				println("FTP server replied: OK PROMOTING")

			return isOK
		} catch(e: IllegalStateException) {
			println("Promoting, exception: Not connected to the server")
			if (displayErrors) error("It was repliied, that you are not connected to the server, hence aborting!")
		} catch(e: IOException) {
			println("Promoting, exception: I/O error")
			if (displayErrors) error("An I/O error occured while promoting/installing the VPK file, hence aborting!")
		} catch(e: it.sauronsoftware.ftp4j.FTPIllegalReplyException) {
			println("Promoting, exception: Server responded in a weird way")
			if (displayErrors) error("The server replied something unexpected, hence aborting!")
		}

		return false;
	}

	fun error(text: String) {
		JOptionPane.showMessageDialog(null, text, "Error", JOptionPane.ERROR_MESSAGE)
	}

	class MyExtractFTPCallback(private val inArchive: IInArchive, val status: Status, val task: SendMaiDumpToVitaTask, val updateStatus: (Status) -> Unit = { }) : IArchiveExtractCallback {
		var lastFileUploaded = ""
		var lastFileSize = 0L

		var runner :Runnable = object: Runnable{override fun run() {}	}
		var streamer = HackyStream()
		var thread = Thread()

		init {
		}

		@Throws(SevenZipException::class)
		override fun getStream(index: Int, extractAskMode: ExtractAskMode): ISequentialOutStream {

			//only gives files with size greater 0
			return ISequentialOutStream { data ->
				val filePath = inArchive.getStringProperty(index, PropID.PATH)
				try {
					val normalizedName = filePath.replace('\\', '/')
					val vname = "/ux0:/mai/$normalizedName"
					val directory = File(vname).parent.replace('\\', '/')
					var incrementFileStatus = 1

					val size = inArchive.getStringProperty(index, PropID.SIZE).toLong()
					if (size == 0L)
						println("Found size second 0 : " + filePath)

					val prop = inArchive.getStringProperty(index, PropID.IS_FOLDER)
					//println("Property :" + prop + " name :" + filePath)
					//non directories gives "-", always true in ISequentialOutStream
					if (prop.equals("-")) {

						//hopefully fixes resuming of uploads
						//7zip can gives chunks of big files, not java streaming friendly
						if (lastFileUploaded.equals(vname)) {
							//upload another chunk of the last file used
							incrementFileStatus = 0
							while(streamer.currentSize > 5E7) {
								Thread.sleep(1000)		//wait for 1 second if queue is over 50 MB
							}
							streamer.setNewData(data)
						} else {
							//new file to be uploaded, last file completed
							thread.join()	//wait for last one to complete
							println("new file to upload $vname")
							lastFileUploaded = vname
							lastFileSize = 0L
							streamer = HackyStream()
							streamer.setNewData(data)

							runner = object: Runnable {
								override fun run() {
									synchronized(PsvitaDevice.connectedFtp()) {
										PsvitaDevice.createDirectories(directory)
										PsvitaDevice.connectedFtp().upload(vname, streamer, 0L, 0L, object : FTPDataTransferListener {
											override fun started() {
												print("started...;")
											}

											override fun completed() {
												print("completed!")
												updateStatus(status)

												if(status.currentFile == status.totalFiles) {
													task.setSuccess()
												}
												//untested
												//if(status.currentSize != status.totalSize) {
												//    println("$vname mismatch transfered size. $status.currentSize != $status.totalSize")
												//
											}

											override fun aborted() {
												print("aborted!")
											}

											override fun transferred(size: Int) {
												status.currentSize += size
												updateStatus(status)
											}

											override fun failed() {
												print("failed!")
											}
										})
									}
								}
							}
							thread = Thread(runner)
							thread.start()

						}

						//println("Writing $vname...")

						lastFileSize += data.size.toLong()
					}
					status.currentFile += incrementFileStatus
					updateStatus(status)


				} catch (e: IOException) {
					e.printStackTrace()
				}
				data.size
			}
		}

		@Throws(SevenZipException::class)
		override fun prepareOperation(extractAskMode: ExtractAskMode) {
		}

		@Throws(SevenZipException::class)
		override fun setOperationResult(extractOperationResult: ExtractOperationResult) {
			if (extractOperationResult == ExtractOperationResult.OK) {
				streamer.setEnd()
			}
				//throw IllegalStateException("ExtractOperationResult should be OK was: $extractOperationResult")
		}

		@Throws(SevenZipException::class)
		override fun setCompleted(completeValue: Long) {
		}

		@Throws(SevenZipException::class)
		override fun setTotal(total: Long) {
		}
	}

	class HackyStream : InputStream(){

		val list = LinkedList<ByteArray>()
		var index: Int = 0
		var end = false
		val barrier = CyclicBarrier(1);		//not sure if it's working, but producer is faster than consumer
		var currentSize = 0L

		override fun read(): Int {
			while (true) {
				try {
					synchronized(list) {
						val data = list.first
						if (data != null) {
							if (index < data.size) {
								return data[index++].ubyte
							} else {
								list.removeFirst();
								currentSize -= data.size
								index = 0;
							}
						}
						barrier.reset();
					}
				} catch (e: NoSuchElementException) {	//gets called if list.first has no first
					if ( end ) {
						return -1;
					}
					barrier.await()
				}
			}
		}

		fun setNewData(data: ByteArray) {
			synchronized(list) {
				list.add(data)
				currentSize += data.size
				try {
					barrier.await()
				} catch (e: InterruptedException) {
					e.printStackTrace()
				} catch (e: BrokenBarrierException) {
					e.printStackTrace()
				}

			}
		}

		fun setEnd() {
			synchronized(list) {
				end = true
				try {
					barrier.await()
				} catch (e: InterruptedException) {
					e.printStackTrace()
				} catch (e: BrokenBarrierException) {
					e.printStackTrace()
				}
			}
		}
	}
}

