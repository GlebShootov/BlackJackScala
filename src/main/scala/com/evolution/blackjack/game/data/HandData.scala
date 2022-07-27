package com.evolution.blackjack.game.data

import com.evolution.blackjack.domain.data.Card

final case class HandData(handDataId: HandId, betAmount: BigDecimal, cards: List[Card] /* todo add player turns sequence */)
