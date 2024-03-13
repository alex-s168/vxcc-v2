package vxcc.ir

import vxcc.cg.Env
import vxcc.cg.Owner
import vxcc.cg.flatten

fun <E: Env<E>> callEmitter(env: E, call: IrCall<E>, dest: Owner<E>?) {
    when (call.fn) {
        "add" -> call.args[0].emitAdd(env, call.args[1], dest!!.storage!!.flatten())
        "mul" -> call.args[0].emitMul(env, call.args[1], dest!!.storage!!.flatten())
        "imul" -> call.args[0].emitSignedMul(env, call.args[1], dest!!.storage!!.flatten())
        "imax" -> call.args[0].emitSignedMax(env, call.args[1], dest!!.storage!!.flatten())
        else -> throw Exception("No builtin function ${call.fn}!")
    }
}