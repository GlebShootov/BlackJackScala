package com.evolution.blackjack.game.event_handler

import akka.actor.typed.scaladsl.{ActorContext, TimerScheduler}
import com.evolution.blackjack.game.BlackjackTableActor.{AwaitingPlayersState, Seats}
import com.evolution.blackjack.game.blackjack_actor_state.{BlackjackActorState => S}
import com.evolution.blackjack.game.command_handler.{BlackjackCommand => C}
import com.evolution.blackjack.game.event_handler.{BlackjackEvent => E}

import scala.concurrent.duration.DurationInt

class JoinGameEventHandler(ctx: ActorContext[C], timer: TimerScheduler[C]) extends BlackjackEventHandler[E.JoinGameEvent, S] {

  def handle(event: E.JoinGameEvent, currentState: S): S = {
    currentState match {
      case state: S.AwaitingPlayersState => {
        val updatedSeats: Seats = AwaitingPlayersState.seats + (event.playerId -> Some(event.seatId))
        if (!timer.isTimerActive(C.StartGame)) {
          timer.startSingleTimer(C.StartGame(None), 10.seconds)
        }
        state.copy(seats = updatedSeats)
      }
      case state: S => {

      }
    }
  }
}
