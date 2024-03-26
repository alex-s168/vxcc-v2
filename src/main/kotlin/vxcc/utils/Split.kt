package vxcc.utils

fun String.splitWithNesting(
    delim: Char,
    nestUp: Char,
    nestDown: Char,
    dest: MutableList<String> = mutableListOf()
): MutableList<String> {
    val last = StringBuilder()
    val iter = iterator()
    var nesting = 0
    while (iter.hasNext()) {
        val c = iter.next()
        if (nesting == 0 && c == delim) {
            dest.add(last.toString())
            last.clear()
        } else if (c == nestUp) {
            nesting ++
            last.append(c)
        } else if (c == nestDown) {
            if (nesting == 0)
                throw Exception("Unmatched $nestDown")
            nesting --
            last.append(c)
        } else {
            last.append(c)
        }
    }
    dest.add(last.toString())
    if (nesting != 0)
        throw Exception("Unmatched $nestUp")
    return dest
}