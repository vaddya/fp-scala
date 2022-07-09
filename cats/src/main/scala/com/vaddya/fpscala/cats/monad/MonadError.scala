package com.vaddya.fpscala.cats.monad

import cats.MonadError
import cats.instances.try_.*
import cats.syntax.applicative.*
import cats.syntax.applicativeError.*
import cats.syntax.monad.*

import scala.util.Try

object MonadError:
  def validateAdult[F[_]](age: Int)(using MonadError[F, Throwable]): F[Int] =
    if (age >= 18) age.pure[F]
    else new IllegalArgumentException(" Age must be greater than or equal to 18").raiseError[F, Int]

@main def runMonadError(): Unit =
  import MonadError.*

  println(validateAdult[Try](18))
  println(validateAdult[Try](10))

  type ExceptionOr[A] = Either[Throwable, A]
  println(validateAdult[ExceptionOr](-1))
