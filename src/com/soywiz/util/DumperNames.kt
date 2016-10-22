package com.soywiz.util

import java.util.*

enum class DumperNames(val shortName: String, val longName: String, val file: String, val size: Long, val sha1: String) {

    VTLEAK("VT_Leak", "Vitamin Leaked Version", DumperModules.VITAMIN.file, 110535L, "8cc41b5a4fd1397cbf3642641f6d654468f4baad".toUpperCase()),
    VT1("VT1.0", "Vitamin 1.0 or 1.1", DumperModules.VITAMIN.file, 107851L, "2b2faa4bf7897bd62ae1e139bec2211639bde975".toUpperCase()),
    VT2("VT2.0", "Vitamin 2.0", DumperModules.VITAMIN.file, 78682L, "4371d5574f15bbfae70bb9a7d98354e5022c7979".toUpperCase()),
    MAI233_0("Mai.v233.0", "Mai.v233.0", DumperModules.MAI.file, 86442L, "A155B682D3803FA77EB5624ADCCFB03A47FE53A0"),
	MAI233_1("Mai.v233.1", "Mai.v233.1", DumperModules.MAI.file, 87056L, "CF7BAB1B68353218BA0C8A6AF0A7A72C693C8313"),
	MAI233_2z2("Mai.v233.2z2", "Mai.v233.2z2", DumperModules.MAI.file, -1L, "283CF9D06654AD28AF9A04A7A35C754A0F6D8411"),
	MAI233_2z7("Mai.v233.2z7", "Mai.v233.2z7 or 2z8 or 2z9", DumperModules.MAI.file, -1L, "B80A485F00ABDCC79C0C2348C1D5CB1D26F7DCFF"),
	MAI233_2z10("Mai.v233.2z10", "Mai.v233.2z10", DumperModules.MAI.file, -1L, "9B1ED3A379306062F65FECDCA48A671971934513"),
    HOMEBREW("HB", "Normal homebrew", "", -1L, ""),
	UNKNOWNMAI("UNKNOWN_MAI", "Unknown Mai Dumper", DumperModules.MAI.file, -1L, ""),
	UNKNOWNVITAMIN("UNKNOWN_VITAMIN", "Unknown Vitamin Dumper", DumperModules.VITAMIN.file, -1L, ""),
    UNKNOWN("UNKNOWN", "Unknown Dumper Version", "", -1L, "");

    companion object {
        fun findDumperBySize(size: Long, dumperModule: DumperModules? = null) = values().firstOrNull { it.size == size } ?: DumperModules.findUnknownDumper(dumperModule?.unknown)
        fun findDumperByShortName(shortName: String) = values().firstOrNull { it.shortName.equals(shortName) } ?: UNKNOWN
		fun findDumperBySHA1(sha1: String, dumperModule: DumperModules? = null) = values().firstOrNull { it.sha1.equals(sha1) } ?: DumperModules.findUnknownDumper(dumperModule?.unknown)
		fun findDumperBySHA1(sha1: String, suprx: String) = findDumperBySHA1(sha1, DumperModules.findDumperModules(suprx))

		fun calculateSHA1(byteArray: ByteArray): String {
			val d = java.security.MessageDigest.getInstance("SHA-1");
			d.reset()
			d.update(byteArray)

			val formatter = Formatter()		//transform bytearray to hexstring
			for (b in d.digest()) {
				formatter.format("%02X", b)
			}
			return formatter.toString()
		}
    }
}

enum class DumperModules(val file: String, val unknown: String) {
    VITAMIN("sce_module/steroid.suprx", "UNKNOWN_VITAMIN"),
    MAI("mai_moe/mai.suprx", "UNKNOWN_MAI");

    companion object {  //circular references between enums are illegal -> using strings
        fun findUnknownDumper(shortName: String?) = when(shortName) {
            is String -> DumperNames.findDumperByShortName(shortName)
            else -> DumperNames.UNKNOWN
        }
		fun findDumperModules(suprx: String): DumperModules? = values().firstOrNull { it.file.equals(suprx) }
		fun findUnknownDumperNameByFile(suprx: String): DumperNames{
			val d = findDumperModules(suprx)?.unknown ?: DumperNames.UNKNOWN.shortName
			return DumperNames.findDumperByShortName(d)
		}
    }
}