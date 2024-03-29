/**
  * Copyright (C) 2013-2015 Typesafe Inc. <http://www.typesafe.com>
  */
package com.vaddya.fpscala.reactive.kvstore.`given`

import akka.actor.{Actor, Props}

import scala.util.Random

object Persistence {
  def props(flaky: Boolean): Props = Props(new Persistence(flaky))
}

class Persistence(flaky: Boolean) extends Actor {
  import com.vaddya.fpscala.reactive.kvstore.Persistence._

  private def newFailCount: Int = if (flaky) Random.nextInt(4) else 0
  var failSteps: Int = newFailCount

  def receive: Receive = {
    case Persist(key, value, id) =>
      if (failSteps == 0) {
        sender() ! Persisted(key, id)
        failSteps = newFailCount
      } else failSteps -= 1
  }
}
