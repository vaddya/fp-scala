package com.vaddya.fpscala.cats.monad

import cats.data.State
import cats.syntax.applicative.*

import scala.util.Try

type CalcState[A] = State[List[Int], A]

object CalcState:
  def evalOne(sym: String): CalcState[Int] = sym match
    case "+" => operator(_ + _)
    case "-" => operator(_ - _)
    case "*" => operator(_ * _)
    case "/" => operator(_ / _)
    case num => operand(num.toInt)

  def evalAll(input: List[String]): CalcState[Int] =
    input.foldLeft(0.pure[CalcState]) { (state, str) =>
      state.flatMap(_ => evalOne(str))
    }

  def evalInput(input: String): Int =
    evalAll(input.split(" ").toList).runA(Nil).value

  private def operator(func: (Int, Int) => Int): CalcState[Int] =
    State(state => state match
      case b :: a :: tail =>
        val ans = func(a, b)
        (ans :: tail, ans)
      case _ =>
        sys.error("Fail!"))

  private def operand(num: Int): CalcState[Int] =
    State(stack => (num :: stack, num))

@main def runCalcState(): Unit =
  import CalcState.*

  val program = for {
    _ <- evalOne("1")
    _ <- evalOne("2")
    ans <- evalOne("-")
  } yield ans

  println(program.runA(Nil).value)

  println(evalAll(List("1", "2", "-")).runA(Nil).value)

  println(evalInput("1 2 + 3 * 9 /"))
