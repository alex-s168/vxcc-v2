package vxcc.binfmt

import blitz.BitField
import blitz.Endian
import blitz.collections.ByteVec
import blitz.toUInt
import blitz.toUShort

data class VxBin(
    val target: String,
    val flags: Flags,
    val sections: List<Section>,
) {
    class Flags: BitField() {
        val linkingFinished by bit(0)
        val pic by bit(1)
    }

    class Section(
        val name: String,
        val symbols: Map<String, Symbol>,
        val unresolved: List<Reference>,
        val bytes: ByteArray
    )

    private data class RawSection(
        val name: String,
        val offset: UInt,
        val symbols: Map<String, Symbol>,
        val unresolved: List<Reference>,
    )

    data class Symbol(
        val pos: UInt,
    )

    data class Reference(
        val pos: UInt,
        val to: String,
        val kind: UByte,
    )

    fun write(buf: ByteVec) {
        TODO()
    }

    companion object {
        fun read(buf: ByteVec): VxBin {
            buf.flip()

            val magic = ByteArray(5)
            buf.popBack(magic)
            require(magic.toString(Charsets.US_ASCII) == "VXBIN") {
                "Not a VXCC binary file!"
            }

            val version = ByteArray(2)
            buf.popBack(version)
            val versionInt = version.toUShort(Endian.LITTLE)
            require(versionInt.toUInt() == 0u) {
                "Unsupported file format version $versionInt!"
            }

            fun string(): String {
                val len = ByteArray(2)
                buf.popBack(len)
                val lenInt = len.toUShort(Endian.LITTLE)
                val str = ByteArray(lenInt.toInt())
                buf.popBack(str)
                return str.toString(Charsets.US_ASCII)
            }

            fun <R> array(elem: () -> R): List<R> {
                val len = ByteArray(2)
                buf.popBack(len)
                val lenInt = len.toUShort(Endian.LITTLE)
                val list = mutableListOf<R>()
                repeat(lenInt.toInt()) {
                    list.add(elem())
                }
                return list
            }

            val target = string()

            val flags = Flags()
            flags.decode(buf.popBack())

            val rawSections = array {
                val name = string()
                val off = byteArrayOf(4)
                buf.popBack(off)
                val offInt = off.toUInt(Endian.LITTLE)
                val symbols = array {
                    val symName = string()
                    val symOff = byteArrayOf(4)
                    buf.popBack(symOff)
                    val symOffInt = symOff.toUInt(Endian.LITTLE)
                    symName to Symbol(symOffInt)
                }.toMap()
                val unresolved = array {
                    val symName = string()
                    val symOff = byteArrayOf(4)
                    buf.popBack(symOff)
                    val symOffInt = symOff.toUInt(Endian.LITTLE)
                    val kind = buf.popBack().toUByte()
                    Reference(symOffInt, symName, kind)
                }
                RawSection(name, offInt, symbols, unresolved)
            }

            val sections = rawSections.map {
                // TODO: don't copy everything
                Section(
                    it.name,
                    it.symbols,
                    it.unresolved,
                    buf.copyIntoArray(
                        ByteArray(buf.size),
                        startOff = it.offset.toInt()
                    )
                )
            }

            return VxBin(target, flags, sections)
        }
    }
}