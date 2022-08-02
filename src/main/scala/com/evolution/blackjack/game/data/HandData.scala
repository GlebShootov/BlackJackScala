package com.evolution.blackjack.game.data

import com.evolution.blackjack.domain.data.Card

final case class HandData(handDataId: HandId, betAmount: BigDecimal, cards: List[Card], playerTurnHistory: List[PlayersTurnOption])

object HandData {
  def apply(handDataId: HandId, betAmount: BigDecimal, cards: List[Card]): HandData =
    HandData(handDataId, betAmount, cards, List.empty)
}
