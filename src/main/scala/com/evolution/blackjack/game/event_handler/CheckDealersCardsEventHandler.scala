package com.evolution.blackjack.game.event_handler

import akka.actor.typed.scaladsl.{ActorContext, TimerScheduler}
import com.evolution.blackjack.game.HandScoreCalculator
import com.evolution.blackjack.game.blackjack_actor_state.{BlackjackActorState => S}
import com.evolution.blackjack.game.command_handler.{BlackjackCommand => C}
import com.evolution.blackjack.game.event_handler.{BlackjackEvent => E}

class CheckDealersCardsEventHandler(ctx: ActorContext[C], timer: TimerScheduler[C]) extends BlackjackEventHandler[E.CheckDealersCardsEvent.type, S] {

  def handle(event: E.CheckDealersCardsEvent.type, currentState: S): S = {
    currentState match {
      case state: S.DealerTurnState => {
        val score = HandScoreCalculator.countScore(state.dealerHand)
        if (score < 17) {
          ctx.self ! C.DealerHit(None)
        } else {
          ctx.self ! C.DetermineWinners(None)
        }
        state
      }
    }
  }
}
