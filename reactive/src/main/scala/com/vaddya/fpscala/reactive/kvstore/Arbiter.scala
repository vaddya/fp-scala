package com.vaddya.fpscala.reactive.kvstore

import akka.actor.{Actor, ActorRef}

object Arbiter {
  case object Join

  case object JoinedPrimary
  case object JoinedSecondary

  /** This message contains all replicas currently known to the arbiter, including the primary. */
  case class Replicas(replicas: Set[ActorRef])
}

class Arbiter extends Actor {
  import Arbiter._

  var leader: Option[ActorRef] = None
  var replicas = Set.empty[ActorRef]

  def receive: Receive = {
    case Join =>
      if (leader.isEmpty) {
        leader = Some(sender())
        replicas += sender()
        sender() ! JoinedPrimary
      } else {
        replicas += sender()
        sender() ! JoinedSecondary
      }
      leader foreach (_ ! Replicas(replicas))
  }
}
