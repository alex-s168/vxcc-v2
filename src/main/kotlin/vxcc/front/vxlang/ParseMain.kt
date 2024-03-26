package vxcc.front.vxlang

import vxcc.utils.splitWithNesting

// comments start and end with #
fun String.removeCommentsAndNL(): String {
    val res = StringBuilder()
    var parsingComment = false
    for (c in this) {
        if (c == '#')
            parsingComment = !parsingComment
        else if (!parsingComment)
            if (c != '\n')
                res.append(c)
    }
    return res.toString()
}

fun String.statements(): List<String> =
    splitWithNesting(';', '{', '}').map { it.trim() }

enum class Modifier {
    PRIVATE,
    PUBLIC,
}

fun String.parseModifier(): Pair<String, Modifier> {
    if (startsWith("private"))
        return substringAfter("private ") to Modifier.PRIVATE
    if (startsWith("public"))
        return substringAfter("public ") to Modifier.PUBLIC
    return this to Modifier.PUBLIC
}

fun String.convName() =
    replace("::", "__")

fun String.convType(): String {
    val spl = split('<')
    val base = spl[0]
    return if (spl.size == 1)
        base.convName()
    else {
        val template = spl[1].substringBefore('>')
        if (base == "ptr")
            "ptr"
        else
            TODO()
    }
}

// possible statements at highest leve:
//  struct Name { fields }
//  type Name value
//  fn function(args) { code }
//  extern fn function(args)
//  type name
//  type name = value
//  _ir code

fun emit(str: String) =
    println(str)

/** for struct decl and fn decl */
fun String.parseDeclList(): List<Pair</** type */ String, String>> =
    split(',').map {
        val xy = it.split(' ').map { arg -> arg.trim() }
        xy[0].convType() to xy[1].convName()
    }

/** Creates VIR ABI based on function arguments and types */
fun List<Pair</** type */ String, String>>.emitAbi(): String {
    // TODO !!!!!!!!!!!
    return "vabi"
}

fun String.emitBody() {
    // TODO!!!!!!!!!!!!!!!!!!!!!!
}

fun Pair<String, Modifier>.emitDecl() {
    val (type, rest) = first.split(' ', limit = 2)
    when (type) {
        "struct" -> TODO()
        "type" -> TODO()
        "fn" -> {
            var (name, args) = rest.split('(')
            name = name.trim().convName()
            args = args.substringBefore(')').trim()
            val body = rest.substringAfter('{').substringBeforeLast('}')
            val argList = args.parseDeclList()
            val abi = argList.emitAbi()
            emit("${if (second == Modifier.PUBLIC) "export " else ""}fn $name")
            emit("  using abi $abi")
            argList.forEachIndexed { i, (_, name) ->
                emit("  %arg$i >< $name")
            }
            body.emitBody()
            emit("end")
        }
        "extern" -> {
            val (fn, decl) = rest.split(' ', limit = 2)
            require(fn == "fn") {
                "only extern fn is valid!"
            }
            var (name, args) = decl.split('(')
            name = name.trim().convName()
            args = args.substringBeforeLast(')').trim()
            val argList = args.parseDeclList()
            // we'll just ignore it for now
            emit("extern fn $name")
        }
        "_ir" -> emit(rest)
        else -> TODO("globals are not yet supported!")
    }
}