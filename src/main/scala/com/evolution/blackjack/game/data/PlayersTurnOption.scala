package com.evolution.blackjack.game.data

sealed trait PlayersTurnOption

object PlayersTurnOption {
  final object Hit extends PlayersTurnOption
  final object Double extends PlayersTurnOption
  final object Insurance extends PlayersTurnOption
  final object Surrender extends PlayersTurnOption
  final object Stay extends PlayersTurnOption
  final object EvenMoney extends PlayersTurnOption
}
