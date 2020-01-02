package com.vaddya.fpscala.design.week3

import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen.{const, frequency, oneOf}
import org.scalacheck.Prop.{forAll, propBoolean}
import org.scalacheck._

import scala.annotation.tailrec

abstract class QuickCheckHeap extends Properties("Heap") with IntHeap {

  lazy val genHeap: Gen[H] = oneOf(
    const(empty),
    for {
      a: A <- arbitrary[A]
      h: H <- frequency((1, empty), (1, genHeap))
    } yield insert(a, h)
  )
  implicit lazy val arbHeap: Arbitrary[H] = Arbitrary(genHeap)

  property("insertion of heap min to heap should not change its min") = forAll { h: H =>
    val m = if (isEmpty(h)) 0 else findMin(h)
    findMin(insert(m, h)) == m
  }

  property("min of two elements inserted into empty heap should be min of new heap") = forAll { (a: Int, b: Int) =>
    val h = insert(a, insert(b, empty))
    findMin(h) == (a min b)
  }

  property("after insertion and deletion from empty heap it should be empty") = forAll { h: H =>
    isEmpty(h) ==> {
      isEmpty(deleteMin(insert(0, h)))
    }
  }

  property("sequence of min should be sorted in ascending order") = forAll { h: H =>
    @tailrec
    def ascending(prev: A, heap: H): Boolean = {
      if (isEmpty(h)) true
      else {
        val m = findMin(h)
        prev <= m && ascending(m, deleteMin(h))
      }
    }

    ascending(Int.MinValue, h)
  }

  property("min of two melded heaps should be min of their min") = forAll { (h1: H, h2: H) =>
    (nonEmpty(h1) && nonEmpty(h2)) ==> {
      val m = findMin(h1) min findMin(h2)
      findMin(meld(h1, h2)) == m
    }
  }
}
