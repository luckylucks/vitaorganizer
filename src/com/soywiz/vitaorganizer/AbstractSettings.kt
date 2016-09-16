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
            return this@AbstractSettings.obj.readProperty(property.name, default)
        }

        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {
            this@AbstractSettings.obj.writeProperty(property.name, value)
        }
    }

    inner class PropDelegateLong(val default: Long) {
        operator fun getValue(thisRef: Any?, property: KProperty<*>): Long {
            return this@AbstractSettings.obj.readProperty(property.name, default.toString()).toLong()
        }

        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Long) {
            this@AbstractSettings.obj.writeProperty(property.name, value.toString())
        }
    }

    fun readProperty(name: String, default: String) : String{
        return ensureProperties().getProperty(name, default)
    }

    fun writeProperty(name: String, value: String) {
        ensureProperties().setProperty(name, value)
        obj.writeProperties()
        //println("Write into Property Name: ${name} Value: ${value}")
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
