package com.evolution.blackjack.domain.data

final case class Card(cardSuit: CardSuit, cardValue: CardValue)

object Card {
  implicit def CardOrdering: Ordering[Card] = (x: Card, y: Card) => x.cardValue.cardId - y.cardValue.cardId
}
