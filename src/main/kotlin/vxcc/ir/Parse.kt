package vxcc.ir

import vxcc.cg.*

/*
Example code:

%0 = i8 imm 10
%1 = i8 imm 20
%2 = i8 (add %0 %1)
~ %0
~ %1
%2 <> eax
(ret)
 */

data class IrScope<E: Env<E>>(
    val locals: MutableMap<String, Pair<TypeId, Owner<E>>> = mutableMapOf()
)

data class IrCall<E: Env<E>>(
    val type: Owner.Flags?,
    val fn: String,
    val args: List<Value<E>>
)

private fun <E: Env<E>> parseAndEmitCall(
    ctx: IrScope<E>,
    typeResolver: (TypeId) -> Owner.Flags,
    callEmitter: (E, IrCall<E>, dest: Owner<E>?) -> Unit,
    env: E,
    callIn: String,
    type: Owner.Flags?,
    dest: Owner<E>?,
) {
    require(callIn.first() == '(')
    require(callIn.last() == ')')
    val call = callIn.substring(1).dropLast(1).split(' ')
    val fn = call[0]
    val args = call.drop(1).map {
        parseAndEmitVal(ctx, typeResolver, callEmitter, env, it, null)!!
    }
    val irCall = IrCall(type, fn, args)
    callEmitter(env, irCall, dest)
}

private fun <E: Env<E>> parseAndEmitVal(
    ctx: IrScope<E>,
    typeResolver: (TypeId) -> Owner.Flags,
    callEmitter: (E, IrCall<E>, dest: Owner<E>?) -> Unit,
    env: E,
    v: String,
    dest: Owner<E>?,
): Value<E>? {
    if (v.startsWith('%'))
        return ctx.locals[v]!!.second.storage!!.flatten()
    val (typeStr, rest) = v.split(' ', limit = 1)
    val type = typeResolver(typeStr)
    return if (rest.startsWith('(')) {
        if (dest == null) {
            val d = env.alloc(type)
            parseAndEmitCall(ctx, typeResolver, callEmitter, env, rest, type, d)
            d.canBeDepromoted = type.copy(use = Env.Use.STORE)
            d.storage!!.flatten()
        } else {
            parseAndEmitCall(ctx, typeResolver, callEmitter, env, rest, type, dest)
            null
        }
    } else {
        if (type.type.float && type.totalWidth == 64)
            env.immediate(rest.toDouble())
        else if (type.type.float && type.totalWidth == 32)
            env.immediate(rest.toFloat())
        else
            env.immediate(rest.toLong(), type.totalWidth)
    }
}

fun <E: Env<E>> parseAndEmit(
    lines: Iterable<String>,
    ctx: IrScope<E>,
    typeResolver: (TypeId) -> Owner.Flags,
    callEmitter: (E, IrCall<E>, dest: Owner<E>?) -> Unit,
    env: E
) {
    for (lineIn in lines) {
        val line = lineIn.split('#', limit = 0)[0].trim()
        when (line.firstOrNull() ?: continue) {
            '%' -> {
                val (name, rest) = line.substring(1).split(' ', limit = 1)
                if (rest.startsWith('=')) {
                    parseAndEmitVal(ctx, typeResolver, callEmitter, env, rest.substring(2), ctx.locals[name]!!.second)
                } else if (rest.startsWith("<>")) {
                    val into = rest.substring(3)
                    env.forceIntoReg(ctx.locals[name]!!.second, into)
                } else {
                    throw Exception("Unexpected symbol in line: $line")
                }
            }
            '(' -> {
                parseAndEmitCall(ctx, typeResolver, callEmitter, env, line.substring(1), null, null)
            }
            '~' -> {
                val name = line.substring(3)
                env.dealloc(ctx.locals[name]!!.second)
            }
            else -> throw Exception("First symbol in line unexpected: $line")
        }
    }
}