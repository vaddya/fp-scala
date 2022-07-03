package com.vaddya.fpscala.cats.functors

import cats.Functor
import cats.syntax.functor.*

object Functors:
  def increase[F[_]](start: F[Int])(implicit functor: Functor[F]): F[Int] =
    start.map(_ + 1)

  given Functor[Tree] with
    override def map[A, B](tree: Tree[A])(f: A => B): Tree[B] =
      tree match
        case Branch(left, right) => Branch(left.map(f), right.map(f))
        case Leaf(value) => Leaf(f(value))

@main def runFunctors(): Unit =
  import Functors.{*, given}

  println(increase(List(1,2)))
  println(increase(Option(5)))

  val tree = Branch(
    Leaf(1),
    Branch(
      Leaf(2),
      Leaf(3)
    )
  )

  println(Functor[Tree].map(tree)(_ * 2))
