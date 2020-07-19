package com.vaddya.fpscala.principles.funset

import scala.annotation.tailrec

object FunctionsOnSets extends App {
  type FunSet = Int => Boolean

  def contains(s: FunSet, elem: Int): Boolean = s(elem)

  /**
   * Define a function which creates a singleton set from one integer value: 
   * the set represents the set of the one given element. 
   */
  def singletonSet(elem: Int): FunSet = x => x == elem

  /**
   * Define the functions union,intersect, and diff, which takes two sets, 
   * and return, respectively, their union, intersection and differences. 
   * diff(s, t) returns a set which contains all the elements of the set s that are not in the set t. 
   */
  def union(s: FunSet, t: FunSet): FunSet = x => contains(s, x) || contains(t, x)

  /**
   * Returns the intersection of the two given sets,
   * the set of all elements that are both in `s` and `t`.
   */
  def intersect(s: FunSet, t: FunSet): FunSet = x => contains(s, x) && contains(t, x)

  /**
   * Returns the difference of the two given sets,
   * the set of all elements of `s` that are not in `t`.
   */
  def diff(s: FunSet, t: FunSet): FunSet = x => contains(s, x) && !contains(t, x)

  /**
   * Returns the subset of `s` for which `p` holds.
   */
  def filter(s: FunSet, p: Int => Boolean): FunSet = x => contains(s, x) && p(x)

  /**
   * The first function tests whether a given predicate is true for all elements 
   * of the set. Here, we consider that an integer x has the property 
   * -1000 <= x <= 1000 in order to limit the search space.
   */
  def forall(s: FunSet, p: Int => Boolean): Boolean = {
    val bound = 1000

    @tailrec
    def iter(a: Int): Boolean = {
      if (a >= bound) true
      else if (contains(s, a) && !p(a)) false
      else iter(a + 1)
    }

    iter(-bound)
  }

  /**
   * Using forall, implement a function exists which tests whether a set contains 
   * at least one element for which the given predicate is true. Note that the functions 
   * forall and exists behave like the universal and existential quantifiers of first-order logic.
   */
  def exists(s: FunSet, p: Int => Boolean): Boolean = !forall(s, x => !p(x))

  /**
   * Finally, using forall or exists, write a function map which transforms a given set 
   * into another one by applying to each of its elements the given function.
   */
  def map(s: FunSet, f: Int => Int): FunSet = x => exists(s, y => f(y) == x)

  val set: FunSet = x => x > 100
  assert(forall(set, x => x > 0))
  assert(!forall(set, x => x < 200))
  assert(exists(set, x => x == 200))
}
