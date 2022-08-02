package com.evolution.blackjack.game

import com.evolution.blackjack.domain.data.{Card, CardValue}

import scala.annotation.tailrec

object HandScoreCalculator {

  def countScore(cards: List[Card]): Int = {
    countScore(cards.map(toValues))
  }

  @tailrec
  private def countScore(cardValues: List[Int], initialScore: Int = 0): Int = {
    val score = cardValues.sum + initialScore
    if (score > 21 && cardValues.contains(11)) {
      countScore(cardValues diff List(11), 1)
    } else {
      score
    }
  }

  private def toValues(card: Card): Int = {
    card match {
      case Card(_, CardValue.`2`) => 2
      case Card(_, CardValue.`3`) => 3
      case Card(_, CardValue.`4`) => 4
      case Card(_, CardValue.`5`) => 5
      case Card(_, CardValue.`6`) => 6
      case Card(_, CardValue.`7`) => 7
      case Card(_, CardValue.`8`) => 8
      case Card(_, CardValue.`9`) => 9
      case Card(_, CardValue.`10`) => 10
      case Card(_, CardValue.Jack) => 10
      case Card(_, CardValue.Queen) => 10
      case Card(_, CardValue.King) => 10
      case Card(_, CardValue.Ace) => 11
    }
  }
}
