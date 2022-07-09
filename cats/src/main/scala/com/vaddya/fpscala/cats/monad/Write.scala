package com.vaddya.fpscala.cats.monad

import cats.Monad
import cats.data.Writer
import cats.instances.vector.*
import cats.syntax.applicative.*
import cats.syntax.writer.*

type Logged[A] = Writer[Vector[String], A]

object Write:
  def slowly[A](body: => A): A =
    try body finally Thread.sleep(200)

  def factorial(n: Int): Int =
    val ans = slowly(if (n == 0) 1 else n * factorial(n - 1))
    println(s"fact $n $ans")
    ans

  def factorialWriter(n: Int): Logged[Int] =
    (if n == 0 then 1.pure[Logged]
    else slowly(factorialWriter(n - 1).map(_ * n)))
      .flatMap { ans =>
        Vector(s"fact $n $ans").tell
          .map(_ => ans)
      }

@main def runWriter(): Unit =
  import Write.*

  println(factorialWriter(5).run)
