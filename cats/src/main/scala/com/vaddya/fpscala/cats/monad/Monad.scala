package com.vaddya.fpscala.cats.monad

import cats.Id
import cats.implicits.*
import cats.syntax.applicative.*

trait Monad[F[_]]:
  def pure[A](a: A): F[A]
  def flatMap[A, B](value: F[A])(func: A => F[B]): F[B]
  def map[A, B](value: F[A])(func: A => B): F[B] =
    flatMap(value)((a: A) => pure(func(a)))

object MonadInstances:
  given Monad[Id] with
    override def pure[A](a: A): Id[A] = a
    override def flatMap[A, B](value: Id[A])(func: A => Id[B]): Id[B] = func(value)

