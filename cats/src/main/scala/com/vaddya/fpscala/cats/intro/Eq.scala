package com.vaddya.fpscala.cats.intro

import cats.Eq
import cats.syntax.eq.*

object Eq:
  implicit val catEq: Eq[Cat] = (cat1, cat2) =>
    cat1 == cat2

@main def runEq(): Unit =
  import Eq.*

  val cat1 = Option(Cat("Busya", 6, "Black"))
  val cat2 = Option.empty[Cat]

  println(cat1 === cat1)
  println(cat1 =!= cat2)
