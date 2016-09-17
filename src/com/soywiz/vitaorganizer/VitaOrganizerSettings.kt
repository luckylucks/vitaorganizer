package com.soywiz.vitaorganizer

import java.io.*

object VitaOrganizerSettings : AbstractSettings( File("vitaorganizer/settings.properties") ) {
	var vpkFolder: String by PropDelegate(".")
	var lastDeviceIp: String by PropDelegate("192.168.1.100")
	var renamerString: String by PropDelegate("%TITLE% [v%VER%][%ID%][%REG%][%VT%].vpk")
}