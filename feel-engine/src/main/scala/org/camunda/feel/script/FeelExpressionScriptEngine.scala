package org.camunda.feel.script

import javax.script._
import org.camunda.feel.parser.FeelParser

class FeelExpressionScriptEngine(val factory: ScriptEngineFactory)
    extends FeelScriptEngine {

  val eval = (expression: String, context: Map[String, Any]) =>
    engine.evalExpression(expression, context)

  val parse = (expression: String) => FeelParser.parseExpression(expression)

}
