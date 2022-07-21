package com.evolution.blackjack.game.data

import com.evolution.blackjack.domain.data.Card

final case class HandData(betAmount: BigDecimal, cards: List[Card])
