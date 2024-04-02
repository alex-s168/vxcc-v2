package vxcc.binfmt

import blitz.*
import blitz.collections.ByteVec
import blitz.term.Terminal
import vxcc.arch.AbstractTarget
import vxcc.arch.parseTargetStr

data class VxBin(
    val target: AbstractTarget,
    val flags: Flags,
    val sections: List<Section>,
    val origin: ULong?,
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
        val size: UInt,
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

    infix fun link(other: VxBin): VxBin {
        require(!other.target.compatible(target)) {
            "Cannot link with executable of incompatible target"
        }

        // btw: refs are across all sections
        TODO()
    }

    fun endLinking() {
        sections.forEach {
            it.unresolved.forEach { r ->
                Terminal.errln("Unresolved reference to \"${r.to}\" in section \"${it.name}\"!")
            }
        }

        if (sections.any { it.unresolved.isNotEmpty() })
            error("Unresolved symbols during linking!")
    }

    fun strip() =
        copy(sections = sections
            .filter { it.name.startsWith('?') }
            .map {
                Section(it.name, mapOf(), it.unresolved, it.bytes)
            }
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

            val orig = if (flags.pic) null else {
                val by = ByteArray(8)
                by.toULong(Endian.LITTLE)
            }

            val rawSections = array {
                val name = string()

                val off = ByteArray(4)
                buf.popBack(off)
                val offInt = off.toUInt(Endian.LITTLE)

                val size = ByteArray(4)
                buf.popBack(size)
                val sizeInt = off.toUInt(Endian.LITTLE)

                val symbols = array {
                    val symName = string()
                    val symOff = ByteArray(4)
                    buf.popBack(symOff)
                    val symOffInt = symOff.toUInt(Endian.LITTLE)
                    symName to Symbol(symOffInt)
                }.toMap()

                val unresolved = array {
                    val symName = string()
                    val symOff = ByteArray(4)
                    buf.popBack(symOff)
                    val symOffInt = symOff.toUInt(Endian.LITTLE)
                    val kind = buf.popBack().toUByte()
                    Reference(symOffInt, symName, kind)
                }

                RawSection(name, offInt, sizeInt, symbols, unresolved)
            }

            val sections = rawSections.map {
                Section(
                    it.name,
                    it.symbols,
                    it.unresolved,
                    buf.copyIntoArray(
                        ByteArray(it.size.toInt()),
                        startOff = it.offset.toInt()
                    )
                )
            }

            return VxBin(parseTargetStr(target), flags, sections, orig)
        }
    }
}