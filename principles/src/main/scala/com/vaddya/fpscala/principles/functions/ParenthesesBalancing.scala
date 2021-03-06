package com.vaddya.fpscala.principles.functions

import scala.annotation.tailrec

/**
 * Write a recursive function which verifies the balancing of parentheses 
 * in a string, which we represent as a List[Char] not a String.
 */
object ParenthesesBalancing extends App {
  def balance(chars: List[Char]): Boolean = {
    @tailrec
    def iter(chars: List[Char], stack: List[Char]): Boolean = chars match {
      case Nil => stack.isEmpty
      case '(' :: ctail => iter(ctail, '(' :: stack)
      case ')' :: ctail => stack match {
        case '(' :: stail => iter(ctail, stail)
        case _ => false
      }
      case _ => iter(chars.tail, stack)
    }

    iter(chars, Nil)
  }

  assert(balance("(if (zero? x) max (/ 1 x))".toList))
  assert(balance("I told him (that it’s not (yet) done). (But he wasn’t listening)".toList))
  assert(!balance(":-)".toList))
  assert(!balance("())(".toList))
  assert(!balance("()(".toList))
}
