package com.vaddya.fpscala.reactive.kvstore

import akka.actor.{Actor, ActorRef, OneForOneStrategy, PoisonPill, Props, ReceiveTimeout, SupervisorStrategy, Terminated}
import Arbiter._
import akka.pattern.{ask, pipe}

import scala.concurrent.duration._
import akka.util.Timeout

object Replica {
  sealed trait Operation {
    def key: String
    def id: Long
  }
  case class Insert(key: String, value: String, id: Long) extends Operation
  case class Remove(key: String, id: Long) extends Operation
  case class Get(key: String, id: Long) extends Operation

  sealed trait OperationReply
  case class OperationAck(id: Long) extends OperationReply
  case class OperationFailed(id: Long) extends OperationReply
  case class GetResult(key: String, valueOption: Option[String], id: Long) extends OperationReply

  def props(arbiter: ActorRef, persistenceProps: Props): Props = Props(new Replica(arbiter, persistenceProps))
}

class Replica(val arbiter: ActorRef, persistenceProps: Props) extends Actor {
  import Replica._
  import Replicator._
  import Persistence._
  import context.dispatcher

  arbiter ! Join


  
  var kv = Map.empty[String, String]
  // a map from secondary replicas to replicators
  var secondaries = Map.empty[ActorRef, ActorRef]
  // the current set of replicators
  var replicators = Set.empty[ActorRef]
  var persisting = Map.empty[Long, (ActorRef, Persist)]
  var seqCount = 0L
  var storage: ActorRef = createStorage()

  def receive: Receive = {
    case JoinedPrimary => context become leader
    case JoinedSecondary => context become replica
  }

  def leader: Receive = {
    case Replicas(replicas) =>
      val oldReplicas = secondaries.keySet -- replicas - self
      secondaries --= oldReplicas
      oldReplicas foreach (_ ! PoisonPill)
      val newReplicas = replicas -- secondaries.keySet - self
      secondaries ++= createReplicators(newReplicas)
      replicators = secondaries.values.toSet
    case Insert(key, value, id) =>
      kv += key -> value
      replicators foreach (_ ! Replicate(key, Some(value), id))
      sender ! OperationAck(id)
    case Remove(key, id) =>
      kv -= key
      replicators foreach (_ ! Replicate(key, None, id))
      sender ! OperationAck(id)
    case Get(key, id) =>
      sender ! GetResult(key, kv get key, id)
    case Replicated(key, id) => ???
    case Persisted(key, id) => ???
    case Terminated(actor) if actor == storage =>
      storage = createStorage()
  }

  def createReplicators(replicas: Set[ActorRef]): Map[ActorRef, ActorRef] = {
    val replicators = replicas.map(replica => replica -> context.actorOf(Replicator.props(replica)))
    for {
      (_, replicator) <- replicators
      (key, value) <- kv
    } replicator ! Replicate(key, Some(value), -1)
    replicators.toMap
  }

  def replica: Receive = {
    case Get(key, id) =>
      sender ! GetResult(key, kv get key, id)
    case Snapshot(key, valueOption, seq) =>
      if (seqCount > seq) sender ! SnapshotAck(key, seq)
      else if (seqCount == seq) {
        valueOption match {
          case Some(value) => kv += ((key, value))
          case None => kv -= key
        }
        seqCount += 1
        val persist = Persist(key, valueOption, seq)
        storage ! persist
        persisting += seq -> (sender, persist)
        context.setReceiveTimeout(100 millis)
      }
    case Persisted(key, seq) =>
      persisting get seq match {
        case Some((actor, _)) =>
          actor ! SnapshotAck(key, seq)
          persisting -= seq
        case None =>
      }
    case ReceiveTimeout =>
      if (persisting.isEmpty) context.setReceiveTimeout(Duration.Undefined)
      else persisting.values foreach { case (_, persist) => storage ! persist }
    case Terminated(actor) if actor == storage => 
      storage = createStorage()
  }
  
  def createStorage(): ActorRef = {
    storage = context.actorOf(persistenceProps)
    context.watch(storage)
    storage
  }
}
