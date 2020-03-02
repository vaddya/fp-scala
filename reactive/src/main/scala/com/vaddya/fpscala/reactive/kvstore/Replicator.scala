package com.vaddya.fpscala.reactive.kvstore

import akka.actor.{Actor, ActorRef, Props, ReceiveTimeout}
import jdk.nashorn.internal.runtime.Undefined

import scala.concurrent.duration._

object Replicator {
  case class Replicate(key: String, valueOption: Option[String], id: Long)
  case class Replicated(key: String, id: Long)

  case class Snapshot(key: String, valueOption: Option[String], seq: Long)
  case class SnapshotAck(key: String, seq: Long)
  
  case class Pending(id: Long, sender: ActorRef, snapshot: Snapshot)

  def props(replica: ActorRef): Props = Props(new Replicator(replica))
}

class Replicator(val replica: ActorRef) extends Actor {
  import Replicator._
  import context.dispatcher

  // map from sequence number to pair of sender and request
  var acks = Map.empty[Long, (ActorRef, Long, Snapshot)]
  // a sequence of not-yet-sent snapshots
//  var pending = Vector.empty[Snapshot]

  var seqCounter = 0L
  def nextSeq(): Long = {
    val ret = seqCounter
    seqCounter += 1
    ret
  }

  def receive: Receive = {
    case Replicate(key, valueOption, id) =>
      val seq = nextSeq()
      val snapshot = Snapshot(key, valueOption, seq)
      acks += seq -> (sender, id, snapshot)
      context.setReceiveTimeout(100 millis)
    case ReceiveTimeout =>
      if (acks.isEmpty) context.setReceiveTimeout(Duration.Undefined)
      else acks.values foreach { case (_, _, snapshot) => replica ! snapshot }
    case SnapshotAck(key, seq) =>
      acks get seq match {
        case Some((actor, id, _)) => 
          acks -= seq
          actor ! Replicated(key, id)
        case None =>
      }
  }
}
