package com.vaddya.fpscala.cats.functors

import com.vaddya.fpscala.cats.functors.Invariant.Codec

object Invariant:
  trait Codec[A]:
    self =>
    def encode(value: A): String
    def decode(value: String): A
    def imap[B](dec: A => B, enc: B => A): Codec[B] =
      new Codec[B]:
        override def encode(value: B): String = self.encode(enc(value))
        override def decode(value: String): B = dec(self.decode(value))

  def encode[A](value: A)(implicit c: Codec[A]): String = c.encode(value)
  def decode[A](value: String)(implicit c: Codec[A]): A = c.decode(value)

  given stringCodec: Codec[String] with
    def encode(value: String): String = value
    def decode(value: String): String = value

  given intCodec: Codec[Int] = stringCodec.imap(_.toInt, _.toString)

  given boxCodec[A](using c: Codec[A]): Codec[Box[A]] with
    override def encode(box: Box[A]): String = c.encode(box.value)
    override def decode(value: String): Box[A] = Box(c.decode(value))

@main def runInvariant(): Unit =
  import Invariant.{*, given}

  implicit val codec: Codec[Double] = stringCodec.imap(_.toDouble, _.toString)

  println(encode(123.3))

  println(decode[Box[Int]]("123"))
  println(encode(Box(123)))
