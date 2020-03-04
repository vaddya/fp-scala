package com.vaddya.fpscala.reactive.kvstore

import akka.actor.{Actor, ActorRef, Props, ReceiveTimeout}
import com.vaddya.fpscala.reactive.kvstore.Replicator._

import scala.concurrent.duration._

object Replicator {
  case class Replicate(key: String, valueOption: Option[String], id: Long)
  case class Replicated(key: String, id: Long)

  case class Snapshot(key: String, valueOption: Option[String], seq: Long)
  case class SnapshotAck(key: String, seq: Long)

  case class Pending(id: Long, actor: ActorRef, snapshot: Snapshot)

  def props(replica: ActorRef): Props = Props(new Replicator(replica))
}

class Replicator(val replica: ActorRef) extends Actor {
  /** Map from sequence number to request id, sender and snapshot */
  var pending = Map.empty[Long, Pending]
  var seqCounter = 0L

  def receive: Receive = {
    case Replicate(key, valueOption, id) =>
      val seq = nextSeq()
      val snapshot = Snapshot(key, valueOption, seq)
      replica ! snapshot
      pending += seq -> Pending(id, sender, snapshot)
      context.setReceiveTimeout(100 millis)
    case ReceiveTimeout =>
      if (pending.isEmpty) context.setReceiveTimeout(Duration.Undefined)
      else pending.values foreach (pending => replica ! pending.snapshot)
    case SnapshotAck(key, seq) =>
      pending = pending.updatedWith(seq) {
        case Some(Pending(id, actor, _)) =>
          actor ! Replicated(key, id)
          None
        case None => None
      }
  }

  def nextSeq(): Long = {
    val seq = seqCounter
    seqCounter += 1
    seq
  }
}
