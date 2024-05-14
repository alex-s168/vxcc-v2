package vxcc.ir.v2

import blitz.parse.NumParse
import blitz.parse.comb.*

object Parse {
    val lower = "abcdefghijklmnopqrstuvwxyz"
    val letters = lower + lower.uppercase()
    val nums = "0123456789"
    val numLets = nums + letters

    val ident = parser {
        it.whitespaces()
            .asLongAs(*letters.toCharArray()) { it }
            ?.map(parser { it.asLongAs(*numLets.toCharArray()) { it } })
            ?.mapSecond { it.first + it.second }
            ?.mapFirst { it.whitespaces() }
    }

    val label = parser {
        it.require("::")
            ?.map(ident)
            ?.mapSecond { AstGlobalLabelRef(it) }
    } or parser {
        it.require(":")
            ?.map(ident)
            ?.mapSecond { AstLocalLabelRef(it) }
    }

    val Var = parser { it.require("%")?.map(ident)?.mapSecond { AstVarRef(it) } }

    lateinit var call: Parser<AstCall>

    val imm = parser { it.map(ident)?.map(NumParse.int)?.mapSecond { (t, v) -> AstImm(t, v) } }
    // lazy because call not yet initialized
    val value by lazy { (Var or imm or call or label).trim() }

    val callInner = parser {
        it.whitespaces()
            .require("[")
            ?.map(ident)
            ?.map(
                parser { it.require(",")?.array(",", value)?.require("]") } or
                parser { it.require("]")?.to(listOf()) }
            )
    }

    init {
        call = parser {
            it.map(ident)?.map(callInner)?.mapSecond { (t, fna) -> AstCall(t, fna.first, fna.second) }
        } or parser {
            it.map(callInner)?.mapSecond { (fn, a) -> AstCall(null, fn, a) }
        }
    }

    val varWithBounds = parser { it.map(Var)?.require("'")?.map(ident) } or parser { it.map(Var)?.mapSecond { it to null } }

    val assignStmt = parser {
        it.map(varWithBounds)
            ?.whitespaces()
            ?.require("=")
            ?.whitespaces()
            ?.map(value)
            ?.mapSecond { (va, v) -> AstAssign(va.first.to, va.second, v) }
    }

    val deleteStmt = parser {
        it.require("~")
            ?.whitespaces()
            ?.map(Var)
            ?.mapSecond { AstDelete(it.to) }
    }

    val usingStmt = parser {
        it.require("using ")
            ?.whitespaces()
            ?.map(ident)
            ?.map(ident)
            ?.mapSecond { (t, v) -> AstUse(t, v) }
    }

    val stmt = (deleteStmt or assignStmt or usingStmt).trim()

    interface Ast

    interface AstExpr: Ast

    data class AstImm(
        val type: String,
        val va: Long
    ): AstExpr

    data class AstVarRef(
        val to: String
    ): AstExpr

    data class AstLocalLabelRef(
        val to: String
    ): AstExpr

    data class AstGlobalLabelRef(
        val to: String
    ): AstExpr

    data class AstCall(
        val retType: String?,
        val fn: String,
        val args: List<Ast>
    ): AstExpr

    data class AstAssign(
        val to: String,
        /** when forced into specific reg for example */
        val toBound: String?,
        val value: AstExpr,
    ): Ast

    data class AstDelete(
        val va: String
    ): Ast

    data class AstUse(
        val type: String,
        val what: String
    ): Ast
}

fun main() {
    val src = "%1 = i32 [add,i32 1,%1]"
    println(Parse.stmt(Parsable(src)))
}