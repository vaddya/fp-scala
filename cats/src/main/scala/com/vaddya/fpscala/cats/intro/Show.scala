package com.vaddya.fpscala.cats.intro

import cats.Show
import cats.implicits.toShow
import cats.instances.all.*

object Show:
  implicit val catShow: Show[Cat] = (cat: Cat) =>
    val name = cat.name.show
    val age = cat.age.show
    val color = cat.color.show

    s"$name is a $age year-old $color cat."

@main def runShow(): Unit =
  import Show.*

  val cat = Cat("Busya", 6, "Black")
  println(cat.show)
