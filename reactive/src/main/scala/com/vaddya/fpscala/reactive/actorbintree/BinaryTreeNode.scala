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
    case contains@Contains(req, id, e) =>
      if (elem == e) {
        req ! ContainsResult(id, result = !removed)
      } else subtrees.get(if (elem > e) Left else Right) match {
        case Some(node) => node ! contains
        case None => req ! ContainsResult(id, result = false)
      }
    case insert@Insert(req, id, e) =>
      if (elem == e) {
        removed = false
        req ! OperationFinished(id)
      } else subtrees = subtrees.updatedWith(if (elem > e) Left else Right) {
        case some@Some(node) =>
          node ! insert
          some
        case None =>
          req ! OperationFinished(id)
          Some(context.actorOf(BinaryTreeNode.props(e, initiallyRemoved = false)))
      }
    case remove@Remove(req, id, e) =>
      if (elem == e) {
        removed = true
        req ! OperationFinished(id)
      } else subtrees.get(if (elem > e) Left else Right) match {
        case Some(node) => node ! remove
        case None => req ! OperationFinished(id)
      }
    case copyTo@CopyTo(treeNode) =>
      if (removed && subtrees.isEmpty) context.parent ! CopyFinished
      else {
        if (!removed) {
          treeNode ! Insert(self, -1, elem)
        }
        subtrees.values foreach (_ ! copyTo)
        context become copying(subtrees.values.toSet, insertConfirmed = removed)
      }
  }

  /** `expected` is the set of ActorRefs whose replies we are waiting for,
    * `insertConfirmed` tracks whether the copy of this node to the new tree has been confirmed.
    */
  def copying(expected: Set[ActorRef], insertConfirmed: Boolean): Receive = {
    case OperationFinished(_) =>
      if (expected.nonEmpty) {
        context become copying(expected, insertConfirmed = true)
      } else {
        self ! PoisonPill
        context.parent ! CopyFinished
      }
    case CopyFinished =>
      val left = expected - sender
      if (left.nonEmpty || !insertConfirmed) {
        context become copying(left, insertConfirmed)
      } else {
        self ! PoisonPill
        context.parent ! CopyFinished
      }
  }
}
