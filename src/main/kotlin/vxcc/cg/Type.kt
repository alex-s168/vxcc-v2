package vxcc.cg

enum class Type(
    val vector: Boolean,
    val int: Boolean = false,
    val float: Boolean = false,
) {
    INT(vector = false, int = true),
    FLT(vector = false, float = true),

    VxINT(vector = true, int = true),
    VxFLT(vector = true, float = true),
}