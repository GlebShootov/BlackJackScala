package com.evolution.blackjack.game.event_handler

trait BlackjackEventHandler[E, S] {

  def handle(event: E, currentState: S): S
}
