package com.vaddya.fpscala.principles.functions

/**
 * Write a recursive function that counts how many different ways 
 * you can make change for an amount, given a list of coin denominations. 
 */
object CountingChange extends App {
  def countChange(money: Int, coins: List[Int]): Int = {
    if (money == 0) 1
    else if (money < 0) 0
    else coins match {
      case head :: tail => countChange(money - head, coins) + countChange(money, tail)
      case _ => 0
    }
  }

  assert(countChange(4, List(1, 2)) == 3)
}
