package vxcc.front.kt

import kotlinx.ast.common.AstSource
import kotlinx.ast.grammar.kotlin.common.summary
import kotlinx.ast.grammar.kotlin.target.antlr.kotlin.KotlinGrammarAntlrKotlinParser

fun main() {
    val source = AstSource.String("test", "fun main(): Int = 1")
    val kotlinFile = KotlinGrammarAntlrKotlinParser.parseKotlinFile(source)
    kotlinFile.summary(attachRawAst = false)
        .onSuccess { astList ->
            astList.forEach { println(it) }
        }.onFailure { errors ->
            errors.forEach(::println)
        }
}