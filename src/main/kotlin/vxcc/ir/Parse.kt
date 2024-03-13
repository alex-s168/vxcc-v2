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

data class IrGlobalScope<E: Env<E>>(
    val types: MutableMap<String, Owner.Flags> = mutableMapOf(),
    val functions: MutableList<String> = mutableListOf(),
)

data class IrLocalScope<E: Env<E>>(
    val locals: MutableMap<String, Pair<TypeId, Owner<E>>> = mutableMapOf(),
    val blocks: MutableList<String> = mutableListOf()
)

internal data class IrCall<E: Env<E>>(
    val type: Owner.Flags?,
    val fn: String,
    val args: List<Value<E>>
)

private fun <E: Env<E>> parseAndEmitCall(
    ctx: IrLocalScope<E>,
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
    ctx: IrLocalScope<E>,
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

private fun <E: Env<E>> parseAndEmit(
    lines: Iterable<String>,
    env: E,
    ctx: IrLocalScope<E>,
    typeResolver: (TypeId) -> Owner.Flags,
) {
    for (lineIn in lines) {
        val line = lineIn.split('#', limit = 0)[0].trim()
        when (line.firstOrNull() ?: continue) {
            ':' -> {
                val bn = line.substring(1)
                env.switch(bn)
                ctx.blocks.add(bn)
            }
            '%' -> {
                val (name, rest) = line.substring(1).split(' ', limit = 2)
                if (rest.startsWith('=')) {
                    parseAndEmitVal(ctx, typeResolver, ::callEmitter, env, rest.substring(2)) { typeStr, type ->
                        if (name.contains('\'')) {
                            val (rname, rdest) = name.split('\'')
                            ctx.locals.computeIfAbsent(rname) { typeStr to env.forceAllocReg(type, rdest) }.second
                        } else {
                            ctx.locals.computeIfAbsent(name) { typeStr to env.alloc(type) }.second
                        }
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
                env.dealloc(ctx.locals.remove(name)!!.second)
            }
            else -> throw Exception("First symbol in line unexpected: $line")
        }
    }
}

fun <E: Env<E>> ir(
    lines: Iterator<String>,
    env: E,
    ctx: IrGlobalScope<E> = IrGlobalScope(),
) {
    while (lines.hasNext()) {
        var line = lines.next().trim()
        if (line.isEmpty()) continue
        if (line.startsWith("fn")) {
            val fnName = line.substring(3)
            env.switch(fnName)
            val fnLines = mutableListOf<String>()
            while (true) {
                line = lines.next().trim()
                if (line == "end")
                    break
                fnLines += line
            }
            ctx.functions.add(fnName)
            parseAndEmit(fnLines, env, IrLocalScope()) { ctx.types[it]!! }
        } else if (line.startsWith("type")) {
            val (name, def) = line.substringAfter("type ").split(" = ", limit = 2)
            assert(name !in ctx.types)
            val defFlags = def.split(' ').associate {
                val f = it.split(':')
                require(f.size == 2)
                f[0] to f[1]
            }
            val vType = defFlags[""]!!
            val type = when (vType) {
                "int" -> {
                    val vWidth = defFlags["w"]!!.toInt()
                    Owner.Flags(Env.Use.SCALAR_AIRTHM, vWidth, null, Type.INT)
                }
                "flt" -> {
                    val vWidth = defFlags["w"]!!.toInt()
                    Owner.Flags(Env.Use.SCALAR_AIRTHM, vWidth, null, Type.FLT)
                }
                else -> throw Exception("unknown type type")
            }
            ctx.types[name] = type
        } else {
            throw Exception("Unknown kind of declaration")
        }
    }
}