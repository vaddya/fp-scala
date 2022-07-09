package com.vaddya.fpscala.cats.monad

import com.vaddya.fpscala.cats.functors.{Tree, Branch, Leaf}
import cats.Monad
import cats.implicits.*
import cats.syntax.applicative.*

object TreeMonad:
  given Monad[Tree] with
    override def pure[A](a: A): Tree[A] = Leaf(a)

    override def flatMap[A, B](value: Tree[A])(func: A => Tree[B]): Tree[B] = value match
      case Branch(left, right) =>
        Branch(flatMap(left)(func), flatMap(right)(func))
      case Leaf(value) =>
        func(value)

    override def tailRecM[A, B](a: A)(func: A => Tree[Either[A, B]]): Tree[B] =
      flatMap(func(a)) {
        case Left(value) =>
          tailRecM(value)(func)
        case Right(value) =>
          Leaf(value)
      }

def branch[A](left: Tree[A], right: Tree[A]): Tree[A] = Branch(left, right)
def leaf[A](value: A): Tree[A] = Leaf(value)

@main def runTreeMonad(): Unit =
  import TreeMonad.{*, given}

  branch(leaf(1), leaf(2))

  val result = for (
    a <- branch(leaf(1), leaf(2));
    b <- branch(leaf(a + 10), leaf(a + 20))
  ) yield (a, b)

  println(result)