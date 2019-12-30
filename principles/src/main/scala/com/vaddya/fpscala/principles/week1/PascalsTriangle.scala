package com.vaddya.fpscala.principles.week1

/**
 * Do this exercise by implementing the pascal function in Main.scala, 
 * which takes a column c and a row r, counting from 0 and returns the 
 * number at that spot in the triangle. 
 */
object PascalsTriangle extends App {
  def pascal(c: Int, r: Int): Int = {
    if (c < 0 || c > r) 0
    else if (r == 0) 1
    else pascal(c - 1, r - 1) + pascal(c, r - 1)
  }

  assert(pascal(0, 2) == 1)
  assert(pascal(1, 2) == 2)
  assert(pascal(1, 3) == 3)

  def triangle(n: Int): Unit = {
    for (i <- 0 to n) {
      for (j <- 0 to i) {
        print(s"${pascal(j, i)} ")
      }
      println
    }
  }

  triangle(4)
}
