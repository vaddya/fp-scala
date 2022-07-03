package com.vaddya.fpscala.cats.functors

import com.vaddya.fpscala.cats.intro.Printable
import com.vaddya.fpscala.cats.intro.Printable.{*, given}
import com.vaddya.fpscala.cats.intro.PrintableInstances.*

object Contravariant:
  extension[A] (p: Printable[A])
    def contramap[B](func: B => A): Printable[B] =
      (value: B) => p.format(func.apply(value))

  given[A] (using p: Printable[A]): Printable[Box[A]] with
    override def format(box: Box[A]): String = p.format(box.value)

  implicit def boxPrintable[A](implicit p: Printable[A]): Printable[Box[A]] =
    p.contramap[Box[A]](_.value)


@main def runContravariant(): Unit =
  import Contravariant.*

  println(format(100))

  val printable = stringPrintable.contramap((_: Int).toHexString)
  println(format(100)(printable))

  println(Box(123))
