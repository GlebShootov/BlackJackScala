package com.evolution.blackjack.domain.data

sealed trait CardSuit

object CardSuit {
  case object Spade extends CardSuit
  case object Club extends CardSuit
  case object Heart extends CardSuit
  case object Diamond extends CardSuit

  def cardSuits: Set[CardSuit] = Set(Spade, Club, Heart, Diamond)
}


//object CardSuitType extends Enumeration {
//  type CardSuitType = Value
//  val Spade, Club, Heart, Diamond = Value
//  val cardSuits: Set[CardSuitType] = Set(Spade, Club, Heart, Diamond)
//}
