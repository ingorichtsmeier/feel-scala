package org.camunda.feel.interpreter

import org.camunda.feel.parser._
import org.joda.time.LocalDate

/**
 * @author Philipp Ossler
 */
class FeelInterpreter {

  def test(expression: Exp)(implicit context: Context): Val = {

    // transform simple literal to simple positive unary test
    val exp = expression match {
      case x @ ConstNumber(_) => Equal(x)
      case x @ ConstBool(_) => Equal(x)
      case e => e
    }

    value(exp)
  }

  def value(expression: Exp)(implicit context: Context): Val = expression match {
    // simple literal
    case ConstNumber(x) => ValNumber(x)
    case ConstBool(b) => ValBoolean(b)
    case ConstDate(d) => ValDate(d)
    // simple unary test
    case LessThan(x) => unaryOp(value(x), _ < _, ValBoolean)
    case LessOrEqual(x) => unaryOp(value(x), _ <= _, ValBoolean)
    case GreaterThan(x) => unaryOp(value(x), _ > _, ValBoolean)
    case GreaterOrEqual(x) => unaryOp(value(x), _ >= _, ValBoolean)
    case interval @ Interval(start, end) => unaryOpDual(value(start.value), value(end.value), isInInterval(interval), ValBoolean)
    // can not evaluate expression
    case exp => ValError(s"unsupported expression '$exp'")
  }

  private def unaryOp(x: Val, c: (Compareable[_], Compareable[_]) => Boolean, f: Boolean => Val)(implicit context: Context): Val =
    withVal(input, _ match {
      case ValNumber(i) => withNumber(x, x => f(c(i, x)))
      case ValDate(i) => withDate(x, x => f(c(i, x)))
      case _ => ValError(s"expected Number or Date but found '$input'")
    })

  private def unaryOpDual(x: Val, y: Val, c: (Compareable[_], Compareable[_], Compareable[_]) => Boolean, f: Boolean => Val)(implicit context: Context): Val =
    withVal(input, _ match {
      case ValNumber(i) => withNumbers(x, y, (x, y) => f(c(i, x, y)))
      case ValDate(i) => withDates(x, y, (x, y) => f(c(i, x, y)))
      case _ => ValError(s"expected Number or Date but found '$input'")
    })

  private def withInput[R, T](f: Val => Val)(implicit context: Context): Val =
    withVal(input, _ match {
      case ValError(e) => ValError(s"expected Number but found '$input'")
      case _ => f(input)
    })

  private def withNumbers(x: Val, y: Val, f: (Double, Double) => Val): Val =
    withNumber(x, x => {
      withNumber(y, y => {
        f(x, y)
      })
    })

  private def withNumber(x: Val, f: Double => Val): Val = x match {
    case ValNumber(x) => f(x)
    case _ => ValError(s"expected Number but found '$x'")
  }

  private def withDates(x: Val, y: Val, f: (LocalDate, LocalDate) => Val): Val =
    withDate(x, x => {
      withDate(y, y => {
        f(x, y)
      })
    })

  private def withDate(x: Val, f: LocalDate => Val): Val = x match {
    case ValDate(x) => f(x)
    case _ => ValError(s"expected Date but found '$x'")
  }

  private def withVal(x: Val, f: Val => Val): Val = x match {
    case ValError(e) => ValError(s"expected value but found '$e'")
    case _ => f(x)
  }

  private def isInInterval(interval: Interval): (Compareable[_], Compareable[_], Compareable[_]) => Boolean =
    (i, x, y) => {
      val inStart: Boolean = interval.start match {
        case OpenIntervalBoundary(_) => i > x
        case ClosedIntervalBoundary(_) => i >= x
      }
      val inEnd = interval.end match {
        case OpenIntervalBoundary(_) => i < y
        case ClosedIntervalBoundary(_) => i <= y
      }
      inStart && inEnd
    }

  private def input(implicit context: Context): Val = context.input
}

case class Context(in: Any) {

  def input: Val = in match {
    case (x: Int) => ValNumber(x)
    case (b: Boolean) => ValBoolean(b)
    case (d: LocalDate) => ValDate(d)
    case _ => ValError(s"unsupported input '$in'")
  }
}