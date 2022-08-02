package com.evolution.blackjack.game.data

import com.evolution.blackjack.domain.data.Card

final case class HandScoreData(cards: List[Card], handStrength: HandStrength) {
  def compare(another: HandScoreData): Int = {
    handStrength match {
      case HandStrength.Blackjack => {
        another.handStrength match {
          case HandStrength.Blackjack => 0
          case HandStrength.RegularStrength(_) => 1
        }
      }
      case HandStrength.RegularStrength(score) => {
        another.handStrength match {
          case HandStrength.Blackjack => -1
          case HandStrength.RegularStrength(anotherScore) => {
            if (score > 21 && anotherScore > 21) 0
            else if (score > 21) -1
            else if (anotherScore > 21) 1
            else score - anotherScore
          }
        }
      }
    }
  }
}

object HandScoreData {
  def apply(score: Int, cards: List[Card]): HandScoreData = {
    val handStrength = if (score == 21 && cards.size == 2) HandStrength.Blackjack
    else HandStrength.RegularStrength(score)

    HandScoreData(cards, handStrength)
  }
}
