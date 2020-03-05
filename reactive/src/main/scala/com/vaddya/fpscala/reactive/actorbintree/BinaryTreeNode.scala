package com.vaddya.fpscala.reactive.actorbintree

import akka.actor._

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

  def props(elem: Int, initiallyRemoved: Boolean): Props = Props(new BinaryTreeNode(elem, initiallyRemoved))
}

class BinaryTreeNode(var elem: Int, initiallyRemoved: Boolean) extends Actor {
  import BinaryTreeNode._
  import BinaryTreeSet._

  var subtrees: Map[Position, ActorRef] = Map()
  var removed: Boolean = initiallyRemoved

  def receive: Receive = normal

  /** Handles `Operation` messages and `CopyTo` requests. */
  def normal: Receive = {
    case op@Insert(req, id, e) =>
      def insert(pos: Position): Unit = subtrees get pos match {
        case Some(node) => node ! op
        case None =>
          val node = context.actorOf(BinaryTreeNode.props(e, initiallyRemoved = false))
          subtrees = subtrees.updated(pos, node)
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
      else {
        if (!removed) treeNode ! Insert(self, -1, elem)
        subtrees.values foreach (_ ! ct)
        context become copying(subtrees.values.toSet, insertConfirmed = removed)
      }
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
