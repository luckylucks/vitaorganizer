package com.soywiz.vitaorganizer

import java.io.*
import java.util.*
import kotlin.reflect.KProperty

object VitaOrganizerSettings : AbstractSettings( File("vitaorganizer/settings.properties") ) {
	var vpkFolder: String by PropDelegate(".")
	var lastDeviceIp: String by PropDelegate("192.168.1.100")
	var renamerString: String by PropDelegate("%TITLE% [v%VER%][%ID%][%REG%][%VT%].C%COMP%.vpk")
}