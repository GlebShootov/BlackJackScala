package com.evolution.blackjack.game.data

sealed trait HandStrength {
  def score: Int
}

object HandStrength {

  object Blackjack extends HandStrength {
    val score: Int = 21
  }

  final case class RegularStrength(score: Int) extends HandStrength
}
