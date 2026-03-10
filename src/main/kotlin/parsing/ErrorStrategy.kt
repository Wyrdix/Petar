package fr.univ_lille.iut_info.parsing

import org.antlr.v4.runtime.BailErrorStrategy
import org.antlr.v4.runtime.Parser
import org.antlr.v4.runtime.RecognitionException

class ErrorStrategy: BailErrorStrategy() {

    override fun recover(recognizer: Parser?, e: RecognitionException?) {
        println(e?.localizedMessage)
        throw RuntimeException(e)
    }

    override fun reportError(recognizer: Parser?, e: RecognitionException?) {
        println(e?.localizedMessage)
        throw RuntimeException(e)
    }
}