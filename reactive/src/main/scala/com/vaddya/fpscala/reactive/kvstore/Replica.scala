package com.vaddya.fpscala.reactive.kvstore

import akka.actor.{Actor, ActorRef, PoisonPill, Props, ReceiveTimeout, Terminated}

import scala.concurrent.duration._

object Replica {
  import Persistence.Persist

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

  case class Handler(actor: ActorRef, persist: Persist,
                     persisted: Boolean = false, replicators: Set[ActorRef] = Set.empty, deadline: Long = 0L)

  def props(arbiter: ActorRef, persistenceProps: Props): Props = Props(new Replica(arbiter, persistenceProps))
}

class Replica(val arbiter: ActorRef, persistenceProps: Props) extends Actor {
  import Arbiter._
  import Persistence._
  import Replica._
  import Replicator._

  /** In-memory key-value storage */
  var kv = Map.empty[String, String]
  /** Persistent storage */
  var storage: ActorRef = createStorage()
  /** Map from secondary replicas to replicators */
  var secondaries = Map.empty[ActorRef, ActorRef]
  /** Current set of replicators */
  var replicators = Set.empty[ActorRef]
  /** Map from operation id to operation handler */
  var handlers = Map.empty[Long, Handler]
  /** Sequence counter to sync with replicator */
  var seqCounter = 0L

  override def preStart(): Unit = {
    arbiter ! Join
  }

  def receive: Receive = {
    case JoinedPrimary => context.become(leader)
    case JoinedSecondary => context.become(replica)
  }

  def leader: Receive = {
    case Replicas(replicas) => updateReplicas(replicas)
    case Get(key, id) => sender() ! GetResult(key, kv get key, id)
    case Insert(key, value, id) =>
      kv += key -> value
      handlers += id -> createLeaderHandler(key, Some(value), id)
      context.setReceiveTimeout(100.millis)
    case Remove(key, id) =>
      kv -= key
      handlers += id -> createLeaderHandler(key, None, id)
      context.setReceiveTimeout(100.millis)
    case Replicated(_, id) =>
      handlers = handlers.updatedWith(id) {
        case Some(handler) => control(id, handler.copy(replicators = handler.replicators - sender()))
        case None => None // timed out
      }
    case Persisted(_, id) =>
      handlers = handlers.updatedWith(id) {
        case Some(handler) => control(id, handler.copy(persisted = true))
        case None => None // timed out 
      }
    case ReceiveTimeout =>
      if (handlers.isEmpty) context.setReceiveTimeout(Duration.Undefined)
      else {
        handlers = filterTimedOut(handlers)
        persist(handlers.values)
      }
    case Terminated(dead) if storage eq dead =>
      storage = createStorage()
      persist(handlers.values)
  }

  def updateReplicas(replicas: Set[ActorRef]): Unit = {
    val oldReplicas = secondaries.keySet -- replicas - self
    val oldReplicators = oldReplicas.map(secondaries)
    val newReplicas = replicas -- secondaries.keySet - self
    val newReplicators = createReplicators(newReplicas)
    oldReplicators foreach (_ ! PoisonPill)
    secondaries = secondaries -- oldReplicas ++ newReplicators
    replicators = secondaries.values.toSet
    handlers = handlers.map(withoutReplicators(oldReplicators)).flatten.toMap
  }

  def withoutReplicators(replicators: Set[ActorRef])(tuple: (Long, Handler)): Option[(Long, Handler)] = tuple match {
    case (id, handler) => control(id, handler.copy(replicators = handler.replicators -- replicators)).map(id -> _)
  }

  def createLeaderHandler(key: String, valueOption: Option[String], id: Long): Handler = {
    val persist = Persist(key, valueOption, id)
    val replicate = Replicate(key, valueOption, id)
    val deadline = operationDeadline()
    storage ! persist
    replicators foreach (_ ! replicate)
    Handler(sender(), persist, persisted = false, replicators, deadline)
  }

  def control(id: Long, handler: Handler): Option[Handler] = handler match {
    case Handler(sender, _, persisted, replicators, deadline) =>
      if (persisted && replicators.isEmpty) {
        sender ! OperationAck(id)
        None
      } else if (System.currentTimeMillis > deadline) {
        sender ! OperationFailed(id)
        None
      } else Some(handler)
  }

  def filterTimedOut(value: Map[Long, Handler]): Map[Long, Handler] = {
    val now = System.currentTimeMillis
    val (valid, timed) = handlers partition { case (_, handler) => handler.deadline > now }
    timed foreach { case (id, handler) => handler.actor ! OperationFailed(id) }
    valid
  }

  def replica: Receive = {
    case Get(key, id) =>
      sender() ! GetResult(key, kv get key, id)
    case Snapshot(key, valueOption, seq) =>
      if (seqCounter > seq && !handlers.contains(seq)) sender() ! SnapshotAck(key, seq)
      else if (seqCounter == seq) {
        valueOption match {
          case Some(value) => kv += (key -> value)
          case None => kv -= key
        }
        handlers += seq -> createReplicaHandler(key, valueOption, seq)
        seqCounter += 1
        context.setReceiveTimeout(100.millis)
      }
    case Persisted(key, seq) =>
      handlers = handlers.updatedWith(seq) {
        case Some(handler) => handler.actor ! SnapshotAck(key, seq); None
        case None => None
      }
    case ReceiveTimeout =>
      if (handlers.isEmpty) context.setReceiveTimeout(Duration.Undefined)
      else persist(handlers.values)
    case Terminated(dead) if storage eq dead =>
      storage = createStorage()
      persist(handlers.values)
  }

  def createReplicaHandler(key: String, valueOption: Option[String], id: Long): Handler = {
    val persist = Persist(key, valueOption, id)
    storage ! persist
    Handler(sender(), persist)
  }

  def persist(handlers: Iterable[Handler]): Unit = handlers filterNot (_.persisted) foreach (storage ! _.persist)

  def createReplicators(replicas: Set[ActorRef]): Map[ActorRef, ActorRef] = {
    val replicators = replicas.map(replica => replica -> context.actorOf(Replicator.props(replica))).toMap
    for {
      (_, replicator) <- replicators
      (key, value) <- kv
    } replicator ! Replicate(key, Some(value), -1)
    replicators
  }

  def createStorage(): ActorRef = {
    storage = context.actorOf(persistenceProps)
    context.watch(storage)
  }

  def operationDeadline(): Long = System.currentTimeMillis + 1.second.toMillis
}
