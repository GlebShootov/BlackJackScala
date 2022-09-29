package com.evolution.blackjack.game.event_handler

import akka.actor.typed.scaladsl.{ActorContext, TimerScheduler}
import com.evolution.blackjack.game.blackjack_actor_state.{BlackjackActorState => S}
import com.evolution.blackjack.game.command_handler.{BlackjackCommand => C}
import com.evolution.blackjack.game.event_handler.{BlackjackEvent => E}

class StartGameEventHandler(ctx: ActorContext[C], timer: TimerScheduler[C]) extends BlackjackEventHandler[E.StartGameEvent.type, S] {

  def handle(event: E.StartGameEvent.type, currentState: S): S = {
    if (currentState.seats.values.flatten.isEmpty) S.AwaitingPlayersState
    else S.PlaceBetsState(currentState.seats, Map.empty)
  }
}
