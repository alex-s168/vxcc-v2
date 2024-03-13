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
    dest: ((TypeId, Owner.Flags) -> Owner<E>)?,
): Value<E>? {
    if (v.startsWith('%'))
        return ctx.locals[v.substring(1)]!!.second.storage!!.flatten()
    val (typeStr, rest) = v.split(' ', limit = 2)
    val type = typeResolver(typeStr)
    return if (rest.startsWith('(')) {
        val destDest = dest?.invoke(typeStr, type)
        if (destDest == null) {
            val d = env.alloc(type)
            parseAndEmitCall(ctx, typeResolver, callEmitter, env, rest, type, d)
            d.canBeDepromoted = type.copy(use = Env.Use.STORE)
            d.storage!!.flatten()
        } else {
            parseAndEmitCall(ctx, typeResolver, callEmitter, env, rest, type, destDest)
            null
        }
    } else {
        val va = if (type.type.float && type.totalWidth == 64)
            env.immediate(rest.toDouble())
        else if (type.type.float && type.totalWidth == 32)
            env.immediate(rest.toFloat())
        else
            env.immediate(rest.toLong(), type.totalWidth)
        val destDest = dest?.invoke(typeStr, type)
        if (destDest == null) {
            va
        } else {
            va.emitMov(env, destDest.storage!!.flatten())
            null
        }
    }
}

fun <E: Env<E>> parseAndEmit(
    lines: Iterable<String>,
    env: E,
    ctx: IrScope<E> = IrScope(),
    typeResolver: (TypeId) -> Owner.Flags,
) {
    for (lineIn in lines) {
        val line = lineIn.split('#', limit = 0)[0].trim()
        when (line.firstOrNull() ?: continue) {
            '%' -> {
                val (name, rest) = line.substring(1).split(' ', limit = 2)
                if (rest.startsWith('=')) {
                    parseAndEmitVal(ctx, typeResolver, ::callEmitter, env, rest.substring(2)) { typeStr, type ->
                        ctx.locals.computeIfAbsent(name) { typeStr to env.alloc(type) }.second
                    }
                } else if (rest.startsWith("<>")) {
                    val into = rest.substring(3)
                    env.forceIntoReg(ctx.locals[name]!!.second, into)
                } else {
                    throw Exception("Unexpected symbol in line: $line")
                }
            }
            '(' -> {
                parseAndEmitCall(ctx, typeResolver, ::callEmitter, env, line.substring(1), null, null)
            }
            '~' -> {
                val name = line.substring(3)
                env.dealloc(ctx.locals[name]!!.second)
            }
            else -> throw Exception("First symbol in line unexpected: $line")
        }
    }
}