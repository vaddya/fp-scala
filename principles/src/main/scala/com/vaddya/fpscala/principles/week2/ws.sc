def countChange(money: Int, coins: List[Int]): Int = {
  if (money == 0) 1
  else if (money < 0) 0
  else coins match {
    case head :: tail => countChange(money - head, coins) + countChange(money, tail)
    case _ => 0
  }
}

countChange(3, List(1, 2))