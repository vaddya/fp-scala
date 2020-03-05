/**
  * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
  */
package com.vaddya.fpscala.reactive.actorbintree

import akka.actor._

import scala.collection.immutable.Queue

object BinaryTreeSet {

  trait Operation {
    def requester: ActorRef
    def id: Int
    def elem: Int
  }

  /** Request with identifier `id` to insert an element `elem` into the tree.
    * The actor at reference `requester` should be notified when this operation
    * is completed.
    */
  case class Insert(requester: ActorRef, id: Int, elem: Int) extends Operation

  /** Request with identifier `id` to check whether an element `elem` is present
    * in the tree. The actor at reference `requester` should be notified when
    * this operation is completed.
    */
  case class Contains(requester: ActorRef, id: Int, elem: Int) extends Operation

  /** Request with identifier `id` to remove the element `elem` from the tree.
    * The actor at reference `requester` should be notified when this operation
    * is completed.
    */
  case class Remove(requester: ActorRef, id: Int, elem: Int) extends Operation

  /** Request to perform garbage collection */
  case object GC

  trait OperationReply {
    def id: Int
  }

  /** Holds the answer to the Contains request with identifier `id`.
    * `result` is true if and only if the element is present in the tree.
    */
  case class ContainsResult(id: Int, result: Boolean) extends OperationReply

  /** Message to signal successful completion of an insert or remove operation. */
  case class OperationFinished(id: Int) extends OperationReply
}

class BinaryTreeSet extends Actor {
  import BinaryTreeNode._
  import BinaryTreeSet._

  var root: ActorRef = createRoot

  /** Used to stash incoming operations during garbage collection) */
  var pendingQueue: Seq[Operation] = Queue.empty[Operation]

  def createRoot: ActorRef = context.actorOf(BinaryTreeNode.props(0, initiallyRemoved = true))

  def receive: Receive = normal

  /** Accepts `Operation` and `GC` messages. */
  def normal: Receive = {
    case op: Operation => root ! op
    case GC =>
      val newRoot = createRoot
      root ! CopyTo(newRoot)
      context become garbageCollecting(newRoot)
  }

  /** Handles messages while garbage collection is performed.
    * `newRoot` is the root of the new binary tree where we want to copy
    * all non-removed elements into.
    */
  def garbageCollecting(newRoot: ActorRef): Receive = {
    case op: Operation =>
      pendingQueue :+= op
    case GC => ()
    case CopyFinished =>
      root = newRoot
      pendingQueue foreach (root ! _)
      pendingQueue = Queue.empty[Operation]
      context become normal
  }
}
