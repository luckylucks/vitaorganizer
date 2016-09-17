package com.soywiz.util

enum class DumperNames(val shortName: String, val longName: String, val file: String, val size: Long) {

    VTLEAK("VT_Leak", "Vitamin Leaked Version", DumperModules.VITAMIN.file, 110535L),
    VT1("VT1.0", "Vitamin 1.0 or 1.1", DumperModules.VITAMIN.file, 107851L),
    VT2("VT2.0", "Vitamin 2.0", DumperModules.VITAMIN.file, 78682L),
    MAI233("Mai.v233.0", "Mai.v233.0", DumperModules.MAI.file, 86442L),
    UNKNOWNMAI("UNKNOWN_MAI", "Unknown Mai Dumper", DumperModules.MAI.file, -1L),
    UNKNOWNVITAMIN("UNKNOWN_VITAMIN", "Unknown Vitamin Dumper", DumperModules.VITAMIN.file, -1L),
    UNKNOWN("UNKNOWN", "Unknown Dumper Version", "", -1L);

    companion object {
        fun findDumperBySize(size: Long, dumperModule: DumperModules? = null) = values().firstOrNull { it.size == size } ?: DumperModules.findUnknownDumper(dumperModule?.unknown)
        fun findDumperByShortName(shortName: String) = values().firstOrNull { it.shortName.equals(shortName) } ?: UNKNOWN
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
    }
}
