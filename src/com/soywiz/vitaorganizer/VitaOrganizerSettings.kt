package com.soywiz.vitaorganizer

import java.io.*
import java.util.*
import kotlin.reflect.KProperty

object VitaOrganizerSettings : AbstractSettings( File("vitaorganizer/settings.properties") ) {
	var vpkFolder: String by PropDelegate(".")
	var lastDeviceIp: String by PropDelegate("192.168.1.100")

	var lastDevicePort: String by PropDelegate("1337")
	var LANGUAGE: String by PropDelegate("auto")

	val isLanguageAutodetect: Boolean get() = (LANGUAGE == "auto")
	val LANGUAGE_LOCALE: Locale get() = if (isLanguageAutodetect) Locale.getDefault() else Locale(LANGUAGE)

	var renamerString: String by PropDelegate("%TITLE% [v%VER%][%ID%][%REG%][%VT%].vpk")
	var qcmaDirectory: String by PropDelegate("")
}