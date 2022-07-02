package com.vaddya.fpscala.cats.monoids

import cats.Monoid
import cats.instances.string.*
import cats.syntax.monoid.*

trait MySemigroup[A] {
  def combine(x: A, y: A): A
}

trait MyMonoid[A] extends MySemigroup[A] {
  def empty: A
}

object MyMonoid {
  def apply[A](using monoid: MyMonoid[A]): MyMonoid[A] = monoid

  given booleanAndMonoid: MyMonoid[Boolean] with
    override def combine(x: Boolean, y: Boolean): Boolean = x && y

    override def empty: Boolean = true

  given booleanOrMonoid: MyMonoid[Boolean] with
    override def combine(x: Boolean, y: Boolean): Boolean = x || y

    override def empty: Boolean = false

  given booleanEitherMonoid: MyMonoid[Boolean] with
    override def combine(x: Boolean, y: Boolean): Boolean = (x && !y) || (!x && y)

    override def empty: Boolean = false

  given booleanXorMonoid: MyMonoid[Boolean] with
    override def combine(x: Boolean, y: Boolean): Boolean = (!x || y) && (x || !y)

    override def empty: Boolean = true

  given setUnionMonoid[A]: MyMonoid[Set[A]] with
    override def combine(x: Set[A], y: Set[A]): Set[A] = x | y

    override def empty: Set[A] = Set.empty

  given setIntersectMonoid[A]: MySemigroup[Set[A]] with
    override def combine(x: Set[A], y: Set[A]): Set[A] = x & y
}

@main def runMonoid(): Unit =
  import MyMonoid.given

  println(MyMonoid[Set[Int]].combine(Set(1, 2), Set(2, 3)))

  println("Hello" |+| "there" |+| Monoid[String].empty)
