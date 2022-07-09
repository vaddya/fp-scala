package com.vaddya.fpscala.cats.monad

import cats.Eval

object Evaluate:
  def foldRight[A, B](as: List[A], acc: B)(fn: (A, B) => B): B =
    as match
      case head :: tail =>
        fn(head, foldRight(tail, acc)(fn))
      case Nil =>
        acc

  def safeFoldRight[A, B](as: List[A], acc: B)(fn: (A, B) => B): B =
    foldRightEval(as, Eval.now(acc))((a, b) =>
      b.map(fn(a, _))
    ).value

  private def foldRightEval[A, B](as: List[A], acc: Eval[B])(fn: (A, Eval[B]) => Eval[B]): Eval[B] =
    as match
      case head :: tail =>
        Eval.defer(fn(head, foldRightEval(tail, acc)(fn)))
      case Nil =>
        acc


@main def runEvaluate(): Unit =
  import Evaluate.safeFoldRight

  println(safeFoldRight((1 to 100000).toList, 0L)(_ + _))
