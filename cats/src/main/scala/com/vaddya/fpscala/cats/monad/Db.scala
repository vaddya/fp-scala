package com.vaddya.fpscala.cats.monad

final case class Db(usernames: Map[Int, String], passwords: Map[String, String])
