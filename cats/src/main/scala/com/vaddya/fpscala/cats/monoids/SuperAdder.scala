package com.vaddya.fpscala.cats.monoids

import cats.Monoid
import cats.instances.int.*
import cats.syntax.monoid.*

object SuperAdder:
  def add[A](items: List[A])(using monoid: Monoid[A]): A =
    items.foldLeft(monoid.empty)(_ |+| _)

  given Monoid[Order] with
    override def combine(x: Order, y: Order): Order =
      Order(x.totalCost + y.totalCost, x.quantity + y.quantity)

    override def empty: Order = Order(0, 0)

@main def runSuperAdder(): Unit =
  import SuperAdder.{*, given}

  println(add(List(1, 2, 3)))
  println(add(List(Some(1), Some(2), None, Some(3))))
  println(add(List(Order(1, 2), Order(3, 4))))
