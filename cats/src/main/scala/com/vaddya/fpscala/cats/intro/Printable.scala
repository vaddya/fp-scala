package com.vaddya.fpscala.cats.intro

trait Printable[A]:
  def format(value: A): String

object Printable:
  // Scala 2
  implicit val stringPrintable: Printable[String] = (value: String) => value

  // Scala 3
  given intPrintable: Printable[Int] with
    override def format(value: Int): String = value.toString

object PrintableInstances:
  // Scala 2
  def format[A](value: A)(implicit printable: Printable[A]): String =
    printable.format(value)

  // Scala 3
  def print[A](value: A)(using printable: Printable[A]): Unit =
    println(format(value))

object PrintableSyntax:
  // Scala 2
  implicit class PrintableOps[A](value: A):
    def format(implicit printable: Printable[A]): String =
      printable.format(value)

  // Scala 3
  extension[A] (value: A)
    def print(using printable: Printable[A]): Unit =
      println(summon[Printable[A]].format(value))

@main def runPrintable(): Unit =
  import PrintableInstances.print as printing
  import PrintableSyntax.*

  given Printable[Cat] with
    override def format(value: Cat): String =
      val name = PrintableInstances.format(value.name)
      val age = PrintableInstances.format(value.age)
      val color = PrintableInstances.format(value.color)

      s"$name is a $age year-old $color cat."

  val cat = Cat("Busya", 6, "Black")
  printing(cat)
  cat.print
