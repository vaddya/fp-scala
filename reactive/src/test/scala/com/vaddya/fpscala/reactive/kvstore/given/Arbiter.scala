/**
  * Copyright (C) 2015 Typesafe Inc. <http://www.typesafe.com>
  */
package com.vaddya.fpscala.reactive.kvstore.`given`

import akka.actor.{Actor, ActorRef, Props}

import scala.util.Random

class Arbiter(lossy: Boolean, audit: ActorRef) extends Actor {
  import com.vaddya.fpscala.reactive.kvstore.Arbiter._

  var leader: Option[ActorRef] = None
  var replicas = Set.empty[ActorRef]

  def receive: Receive = {
    case Join =>
      if (leader.isEmpty) {
        leader = Some(sender)
        replicas += sender
        sender ! JoinedPrimary
        audit ! JoinedPrimary
      } else {
        replicas += (if (lossy) context.actorOf(Props(classOf[LossyTransport], sender)) else sender)
        sender ! JoinedSecondary
        audit ! JoinedSecondary
      }
      leader foreach (_ ! Replicas(replicas))
  }
}

class LossyTransport(target: ActorRef) extends Actor {
  val rnd = new Random
  var dropped = 0
  def receive: Receive = {
    case msg =>
      if (dropped > 2 || rnd.nextFloat < 0.9) {
        dropped = 0
        target forward msg
      } else {
        dropped += 1
      }
  }
}
