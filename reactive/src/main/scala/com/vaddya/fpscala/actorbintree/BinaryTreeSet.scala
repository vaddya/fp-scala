/**
  * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
  */
package com.vaddya.fpscala.actorbintree

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

  import BinaryTreeSet._
  import BinaryTreeNode._

  var root: ActorRef = createRoot

  /** Used to stash incoming operations during garbage collection) */
  var pendingQueue: Seq[Operation] = Queue.empty[Operation]

  def createRoot: ActorRef = context.actorOf(BinaryTreeNode(0, initiallyRemoved = true))

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

object BinaryTreeNode {

  trait Position
  case object Left extends Position
  case object Right extends Position

  case class CopyTo(treeNode: ActorRef)

  /** Acknowledges that a copy has been completed. This message should be sent
    * from a node to its parent, when this node and all its children nodes have
    * finished being copied.
    */
  case object CopyFinished

  def apply(elem: Int, initiallyRemoved: Boolean): Props = Props(new BinaryTreeNode(elem, initiallyRemoved))
}

class BinaryTreeNode(var elem: Int, initiallyRemoved: Boolean) extends Actor {

  import BinaryTreeSet._
  import BinaryTreeNode._

  var subtrees: Map[Position, ActorRef] = Map()
  var removed: Boolean = initiallyRemoved

  def receive: Receive = normal

  /** Handles `Operation` messages and `CopyTo` requests. */
  def normal: Receive = {
    case op@Insert(req, id, e) =>
      def insert(position: Position): Unit = subtrees get position match {
        case Some(node) => node ! op
        case None =>
          subtrees = subtrees.updated(position, context.actorOf(BinaryTreeNode(e, initiallyRemoved = false)))
          req ! OperationFinished(id)
      }

      if (elem > e) insert(Left)
      else if (elem < e) insert(Right)
      else {
        removed = false
        req ! OperationFinished(id)
      }

    case op@Contains(req, id, e) =>
      def contains(pos: Position): Unit = subtrees get pos match {
        case Some(node) => node ! op
        case None => req ! ContainsResult(id, result = false)
      }

      if (elem > e) contains(Left)
      else if (elem < e) contains(Right)
      else req ! ContainsResult(id, result = !removed)

    case op@Remove(req, id, e) =>
      def remove(pos: Position): Unit = subtrees get pos match {
        case Some(node) => node ! op
        case None => req ! OperationFinished(id)
      }

      if (elem > e) remove(Left)
      else if (elem < e) remove(Right)
      else {
        removed = true
        req ! OperationFinished(id)
      }

    case ct@CopyTo(treeNode) =>
      if (removed && subtrees.isEmpty) context.parent ! CopyFinished
      if (!removed) treeNode ! Insert(self, -1, elem)
      subtrees foreach { case (_, node) => node ! ct }
      context become copying(subtrees.values.toSet, insertConfirmed = removed)
  }

  /** `expected` is the set of ActorRefs whose replies we are waiting for,
    * `insertConfirmed` tracks whether the copy of this node to the new tree has been confirmed.
    */
  def copying(expected: Set[ActorRef], insertConfirmed: Boolean): Receive = {
    case OperationFinished(_) =>
      if (expected.nonEmpty) context become copying(expected, insertConfirmed = true)
      else {
        self ! PoisonPill
        context.parent ! CopyFinished
      }
    case CopyFinished =>
      val left = expected - sender
      if (left.nonEmpty || !insertConfirmed) context become copying(left, insertConfirmed)
      else {
        self ! PoisonPill
        context.parent ! CopyFinished
      }
  }
}
