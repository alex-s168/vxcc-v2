package vxcc.ir

import blitz.collections.contents
import vxcc.cg.*
import vxcc.utils.Either
import vxcc.utils.flatten
import vxcc.utils.warn

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

data class IrGlobalScope<E: CGEnv<E>>(
    val types: MutableMap<String, Owner.Flags> = mutableMapOf(),
    val functions: MutableList<String> = mutableListOf(),
    val abis: MutableMap<String, TypedABI> = mutableMapOf(),
)

data class IrLocalScope<E: CGEnv<E>>(
    val parent: IrGlobalScope<E>,
    val locals: MutableMap<String, Pair<Owner.Flags, Owner<E>>> = mutableMapOf(),
    val blocks: MutableList<String> = mutableListOf()
)

internal data class IrCall<E: CGEnv<E>>(
    val type: Owner.Flags?,
    val fn: String,
    val args: List<Either<Value<E>, String>>
)

private fun String.splitWithNesting(
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

private fun <E: CGEnv<E>> parseAndEmitCall(
    ctx: IrLocalScope<E>,
    typeResolver: (TypeId) -> Owner.Flags,
    callEmitter: (IrLocalScope<E>, E, IrCall<E>, dest: Owner<E>?) -> Unit,
    env: E,
    callIn: String,
    type: Owner.Flags?,
    dest: Owner<E>?,
) {
    require(callIn.first() == '[')
    require(callIn.last() == ']')

    val call = callIn.substring(1).dropLast(1).splitWithNesting(',', '[', ']')
    val fn = call[0]
    val args = call.drop(1).map { itIn ->
        val it = itIn.trim()
        if (it.startsWith("::"))
            Either.ofB(it.substring(2))
        else if (it.startsWith(':'))
            Either.ofB(".${it.substring(1)}")
        else
            Either.ofA<Value<E>, String>(parseAndEmitVal(ctx, typeResolver, callEmitter, env, it, null)!!)
    }
    val irCall = IrCall(type, fn, args)
    callEmitter(ctx, env, irCall, dest)
}

private fun <E: CGEnv<E>> parseAndEmitVal(
    ctx: IrLocalScope<E>,
    typeResolver: (TypeId) -> Owner.Flags,
    callEmitter: (IrLocalScope<E>, E, IrCall<E>, dest: Owner<E>?) -> Unit,
    env: E,
    v: String,
    dest: ((Owner.Flags) -> Owner<E>)?,
): Value<E>? {
    if (v.startsWith('%')) {
        val name = v.substring(1)
        val va = ctx.locals[name]!!
        val destDest = dest?.invoke(va.first)
        val vaSto = va.second.storage!!.flatten()
        return if (destDest == null) {
            vaSto
        } else {
            vaSto.emitMov(env, destDest.storage!!.flatten())
            null
        }
    }
    val (typeStr, rest) = v.split(' ', limit = 2).also {
        if (it.size != 2) throw Exception("Size needs to be specified!")
    }
    val type = typeResolver(typeStr)
    return if (rest.startsWith('[')) {
        val destDest = dest?.invoke(type)
        if (destDest == null) {
            val d = env.alloc(type)
            parseAndEmitCall(ctx, typeResolver, callEmitter, env, rest, type, d)
            d.canBeDepromoted = type.copy(use = CGEnv.Use.STORE)
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
        val destDest = dest?.invoke(type)
        if (destDest == null) {
            va
        } else {
            va.emitMov(env, destDest.storage!!.flatten())
            null
        }
    }
}

private fun <E: CGEnv<E>> parseAndEmit(
    lines: Iterable<String>,
    env: E,
    ctx: IrLocalScope<E>,
    verbose: Boolean,
    typeResolver: (TypeId) -> Owner.Flags,
) {
    for (lineIn in lines) {
        val line = lineIn.split('#', limit = 0)[0].trim()
        if (line.isEmpty())
            continue
        if (verbose)
            env.comment(line)

        if (line.startsWith("using abi")) {
            val abiStr = line.substringAfter("using abi ")
            val abi = if (abiStr == "none") null else (ctx.parent.abis[abiStr]
                ?: error("ABI $abiStr not defined! You can use `none` to set to no ABI"))
            ctx.locals.keys.asSequence()
                .contents
                .filter { it.startsWith("arg") }
                .forEach {
                    ctx.locals.remove(it)
                    env.deallocReg(it)
                }
            env.currentABI = abi
            abi?.typedArgRegs?.entries?.forEachIndexed { id, (reg, type) ->
                val alloc = env.forceAllocReg(type, reg)
                ctx.locals["arg$id"] = type to alloc
            }
            continue
        }

        when (line.first()) {
            ':' -> {
                val bn = ".${line.substring(1)}"
                env.switch(bn)
                ctx.blocks.add(bn)
            }
            '%' -> {
                val (name, rest) = line.substring(1).split(' ', limit = 2)
                if (name.contains('\'')) {
                    val (rname, _) = name.split('\'')
                    if (rname.startsWith("arg"))
                        error("You cannot reallocate function arguments!")
                }
                if (rest.startsWith('=')) {
                    parseAndEmitVal(ctx, typeResolver, ::callEmitter, env, rest.substring(2)) { type ->
                        if (name.contains('\'')) {
                            val (rname, rdest) = name.split('\'')
                            if (rname == "")
                                env.forceAllocReg(type, rdest).also { it.shouldBeDestroyed = true }
                            else
                                ctx.locals.computeIfAbsent(rname) { type to env.forceAllocReg(type, rdest) }.second
                        } else {
                            ctx.locals.computeIfAbsent(name) { type to env.alloc(type) }.second
                        }
                    }
                } else if (rest.startsWith("?")) {
                    val typeStr = rest.substring(2)
                    val type = typeResolver(typeStr)
                    if (name.contains('\'')) {
                        val (rname, rdest) = name.split('\'')
                        ctx.locals[rname] =  type to env.forceAllocReg(type, rdest)
                    } else {
                        ctx.locals[name] = type to env.alloc(type)
                    }
                } else if (rest.startsWith("<>")) {
                    val into = rest.substring(3)
                    val loc = ctx.locals[name]!!.second
                    env.forceIntoReg(loc, into)
                } else if (rest.startsWith('@')) {
                    if (rest.startsWith("@mem")) {
                        val (typeStr, where) = rest.substringAfter("@mem ").split(' ', limit = 2)
                        val type = typeResolver(typeStr)
                        val whereInt = where.toULongOrNull()
                        if (whereInt != null) {
                            ctx.locals[name] = type to Owner(
                                Either.ofB(env.addrToMemStorage(whereInt, type)),
                                type
                            )
                        } else if (where.startsWith('*')) {
                            val ext = env.alloc(env.optimal.ptr)
                            parseAndEmitVal(ctx, typeResolver, ::callEmitter, env, where.substring(1)) { _ -> ext }
                            ctx.locals[name] = type to Owner(
                                Either.ofB(env.addrToMemStorage(ext, type)),
                                type
                            )
                        } else {
                            val label = if (where.startsWith("::"))
                                where.substring(2)
                            else if (where.startsWith(':'))
                                where.substring(1)
                            else
                                throw Exception("invalid location")
                            ctx.locals[name] = type to Owner(
                                Either.ofB(env.addrOfAsMemStorage(label, type)),
                                type
                            )
                        }
                    } else {
                        throw Exception()
                    }
                }  else {
                    throw Exception("Unexpected symbol in line: $line")
                }
            }
            '[' -> {
                parseAndEmitCall(ctx, typeResolver, ::callEmitter, env, line, null, null)
            }
            '~' -> {
                val name = line.substring(3)
                if (name.startsWith("arg")) {
                    val select = name.substringAfter("arg")
                    val who = if (select == "*") {
                        ctx.locals.keys.asSequence()
                            .filter { it.startsWith("arg") }.toList()
                    } else if (select.startsWith(">")) {
                        val a = select.substring(1).toInt()
                        ctx.locals.keys.asSequence()
                            .filter { it.startsWith("arg") }
                            .filter { it.substringAfter("arg").toInt() > a }
                            .toList()
                    } else {
                        listOf(name)
                    }
                    who.forEach { env.dealloc(ctx.locals.remove(it)!!.second) }
                } else {
                    env.dealloc(ctx.locals.remove(name)!!.second)
                }
            }
            '!'-> {
                val asm = line.substring(2).split(' ')
                val cmd = asm[0]
                val args = asm.drop(1).map {
                    if (it.startsWith('%')) {
                        val (v, w) = it.substring(1).split('@', limit = 2)
                        Either.ofB<String, Pair<String, Owner<E>>>(w to ctx.locals[v]!!.second)
                    } else {
                        Either.ofA(it)
                    }
                }.toTypedArray()
                env.inlineAsm(cmd, *args)
            }
            else -> throw Exception("First symbol in line unexpected: $line")
        }
    }
}

fun <E: CGEnv<E>> ir(
    lines: Iterator<String>,
    env: E,
    ctx: IrGlobalScope<E> = IrGlobalScope(),
    verbose: Boolean = false,
) {
    while (lines.hasNext()) {
        var line = lines.next().split('#', limit = 2)[0].trim()
        if (line.isEmpty()) continue
        if (line.startsWith("abi")) {
            val (name, def) = line.substringAfter("abi ").split(" = ", limit = 2)
            require(name !in ctx.abis)
            val defFlags = def.split(' ').associate {
                val f = it.split(':')
                require(f.size == 2)
                f[0] to f[1]
            }
            val ins = defFlags["inr"] ?: error("`inr` (in registers) not defined in ABI!")
            val outs = defFlags["outr"] ?: error("`outr` (out registers) not defined in ABI!")
            val clobs = defFlags["clobr"] ?: error("`clobr` (clobbed registers) not defined in ABI!")
            fun f(fl: String) =
                fl  .split(',')
                    .associate {
                        it.split('@').let { (t, r) ->
                            r to (ctx.types[t] ?: error("type $t not found!"))
                        }
                    }
            val abi = TypedABI(f(ins), f(outs), f(clobs))
            ctx.abis[name] = abi
        } else if (line.startsWith("fn") || line.startsWith("export fn")) {
            val export = line[0] == 'e'
            if (verbose)
                env.comment(line)
            val fnName = line.substringAfter("fn ")
            env.switch(fnName)
            env.enterFrame()
            val fnLines = mutableListOf<String>()
            while (true) {
                line = lines.next().trim()
                if (line == "end")
                    break
                fnLines += line
            }
            ctx.functions.add(fnName)
            val localCtx = IrLocalScope(ctx)
            parseAndEmit(fnLines, env, localCtx, verbose) { ctx.types[it]!! }
            localCtx.locals.forEach { (name, _) ->
                warn("Local $name not deallocated! (Can lead to bad code)")
            }
            if (verbose)
                env.comment("end")
            env.leaveFrame()
            env.emitRet()
            if (export)
                env.export(fnName)
            env.endFnGen()
            env.currentABI = null
        } else if (line.startsWith("extern fn")) {
            val name = line.substringAfter("extern fn ")
            env.import(name)
        } else if (line.startsWith("data") || line.startsWith("export data")) {
            val export = line[0] == 'e'
            val rest = line.split(' ')
            val name = rest[1]
            if (verbose)
                env.comment("data $name")
            val data = rest.drop(2).map { it.toByte() }.toByteArray()
            env.staticLabeledData(name, data.size, data)
            if (export)
                env.export(name)
        } else if (line.startsWith("type")) {
            val (name, def) = line.substringAfter("type ").split(" = ", limit = 2)
            require(name !in ctx.types)
            val defFlags = def.split(' ').associate {
                val f = it.split(':')
                require(f.size == 2)
                f[0] to f[1]
            }
            val vType = defFlags[""]!!
            val type = when (vType) {
                "int" -> {
                    val vWidth = defFlags["w"]!!.toInt()
                    Owner.Flags(CGEnv.Use.SCALAR_AIRTHM, vWidth, null, Type.INT)
                }
                "flt" -> {
                    val vWidth = defFlags["w"]!!.toInt()
                    Owner.Flags(CGEnv.Use.SCALAR_AIRTHM, vWidth, null, Type.FLT)
                }
                else -> throw Exception("unknown type type")
            }
            ctx.types[name] = type
        } else {
            throw Exception("Unknown kind of declaration")
        }
    }
}