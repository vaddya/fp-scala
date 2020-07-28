package com.vaddya.fpscala.design.calculator

sealed abstract class Expr
final case class Literal(v: Double) extends Expr
final case class Ref(name: String) extends Expr
final case class Plus(a: Expr, b: Expr) extends Expr
final case class Minus(a: Expr, b: Expr) extends Expr
final case class Times(a: Expr, b: Expr) extends Expr
final case class Divide(a: Expr, b: Expr) extends Expr

object Calculator extends CalculatorInterface {
  def computeValues(namedExpressions: Map[String, Signal[Expr]]): Map[String, Signal[Double]] = {
    namedExpressions
      .view
      .mapValues(sig => Signal(eval(sig(), namedExpressions)))
      .toMap
  }

  def eval(expr: Expr, references: Map[String, Signal[Expr]]): Double = {
    def iter(expr: Expr,
             refs: Map[String, Signal[Expr]],
             used: Set[String]): Double = expr match {
      case Literal(v) => v
      case Plus(a, b) => iter(a, refs, used) + iter(b, refs, used)
      case Minus(a, b) => iter(a, refs, used) - iter(b, refs, used)
      case Times(a, b) => iter(a, refs, used) * iter(b, refs, used)
      case Divide(a, b) => iter(a, refs, used) / iter(b, refs, used)
      case Ref(name) =>
        if (used contains name) Double.NaN
        else iter(getReferenceExpr(name, refs), refs, used + name)
    }
  
    iter(expr, references, Set())
  }

  /** Get the Expr for a referenced variables.
   * If the variable is not known, returns a literal NaN.
   */
  private def getReferenceExpr(name: String, references: Map[String, Signal[Expr]]): Expr = {
    references.get(name).fold[Expr] {
      Literal(Double.NaN)
    } { exprSignal =>
      exprSignal()
    }
  }
}
