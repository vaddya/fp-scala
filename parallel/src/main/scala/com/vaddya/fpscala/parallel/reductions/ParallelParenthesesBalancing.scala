package com.vaddya.fpscala.parallel.reductions

import scala.annotation._
import org.scalameter._

object ParallelParenthesesBalancingRunner {

  @volatile var seqResult = false

  @volatile var parResult = false

  val standardConfig = config(
    Key.exec.minWarmupRuns -> 40,
    Key.exec.maxWarmupRuns -> 80,
    Key.exec.benchRuns -> 120,
    Key.verbose -> true
  ) withWarmer(new Warmer.Default)

  def main(args: Array[String]): Unit = {
    val length = 100000000
    val chars = new Array[Char](length)
    val threshold = 10000
    val seqtime = standardConfig measure {
      seqResult = ParallelParenthesesBalancing.balance(chars)
    }
    println(s"sequential result = $seqResult")
    println(s"sequential balancing time: $seqtime")

    val fjtime = standardConfig measure {
      parResult = ParallelParenthesesBalancing.parBalance(chars, threshold)
    }
    println(s"parallel result = $parResult")
    println(s"parallel balancing time: $fjtime")
    println(s"speedup: ${seqtime.value / fjtime.value}")
  }
}

object ParallelParenthesesBalancing extends ParallelParenthesesBalancingInterface {

  /** Returns `true` iff the parentheses in the input `chars` are balanced.
   */
  def balance(chars: Array[Char]): Boolean = {
    var counter = 0
    chars.foldLeft(true) { case (isBalanced, c) =>
      isBalanced && {
        if (c == '(') {
          counter += 1
          true
        } else if (c == ')') {
          counter -= 1
          counter >= 0
        } else counter >= 0
      }
    } && counter == 0
  }

  /** Returns `true` iff the parentheses in the input `chars` are balanced.
   */
  def parBalance(chars: Array[Char], threshold: Int): Boolean = {

    @tailrec
    def traverse(idx: Int, until: Int, open: Int, min: Int): (Int, Int) = {
      if (idx >= until) (open, min)
      else chars(idx) match {
        case '(' => traverse(idx + 1, until, open + 1, min)
        case ')' => traverse(idx + 1, until, open - 1, min min (open - 1))
        case _   => traverse(idx + 1, until, open, min)
      }
    }

    def reduce(from: Int, until: Int): (Int, Int) = {
      if (until - from < threshold) traverse(from, until, 0, 0)
      else {
        val mid = until + (from - until) / 2
        val ((o1, m1), (o2, m2)) = parallel(
          reduce(from, mid),
          reduce(mid, until)
        )
        (o1 + o2, m1 min (o1 + m2))
      }
    }

    reduce(0, chars.length) == (0, 0)
  }

  // For those who want more:
  // Prove that your reduction operator is associative!

}
