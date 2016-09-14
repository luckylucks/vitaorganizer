package com.soywiz.vitaorganizer

import java.io.*
import java.util.*
import kotlin.reflect.KProperty

abstract class AbstractSettings(var file: File) {
    val CHARSET = Charsets.UTF_8

    protected var initialized = false
    protected val properties = Properties()
    val obj = this

    inner class PropDelegate(val default: String) {
        operator fun getValue(thisRef: Any?, property: KProperty<*>): String {
            return this@AbstractSettings.obj.ensureProperties().getProperty(property.name, default)
        }

        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {
            this@AbstractSettings.obj.ensureProperties().setProperty(property.name, value)
            this@AbstractSettings.obj.writeProperties()
        }
    }

    protected fun ensureProperties(): Properties {
        if (!initialized) readProperties()
        return properties
    }

    protected fun readProperties() {
        if (file.exists()) properties.load(InputStreamReader(ByteArrayInputStream(file.readBytes()), CHARSET))
        initialized = true
    }

    protected fun writeProperties() {
        file.parentFile.mkdirs()
        FileOutputStream(file).use { os ->
            OutputStreamWriter(os, CHARSET).use { writer ->
                properties.store(writer, "")
            }
        }
    }
}
