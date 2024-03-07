package vxcc

enum class Type(
    val vector: Boolean,
    val int: Boolean = false,
    val float: Boolean = false,
    val signed: Boolean = false,
) {
    INT(vector = false, int = true, signed = true),
    UINT(vector = false, int = true, signed = false),
    FLT(vector = false, float = true),

    VxINT(vector = true, int = true, signed = true),
    VxUINT(vector = true, int = true, signed = false),
    VxFLT(vector = true, float = true),
}