package com.evolution.blackjack.domain.data

import scala.util.Random

final case class CardDeck(cards: List[Card])

object CardDeck {
  def getCardDeck(decksQuantity: Int = 1): CardDeck = {
    if (decksQuantity <= 0) {
      CardDeck(List())
    } else {
      val cardSuits = CardSuit.cardSuits.toList
      val cardValues = CardValue.cardValues.toList

      val indexSeq = for {
        cardSuitId <- cardSuits.indices
        cardValueId <- cardValues.indices
        cardSuit = cardSuits(cardSuitId)
        cardValue = cardValues(cardValueId)
      } yield Card(cardSuit, cardValue)
      CardDeck(List.fill(decksQuantity)(indexSeq.toList).flatten)
    }
  }

  def getShuffledDeck(decksQuantity: Int = 1): CardDeck = {
    val cardDeck = getCardDeck(decksQuantity)
    CardDeck(Random.shuffle(cardDeck.cards))
  }
}