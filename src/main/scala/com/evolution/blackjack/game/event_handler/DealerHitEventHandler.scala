package com.evolution.blackjack.game.event_handler

import akka.actor.typed.scaladsl.{ActorContext, TimerScheduler}
import com.evolution.blackjack.game.BlackjackTableActor.ErrorState
import com.evolution.blackjack.game.HandScoreCalculator
import com.evolution.blackjack.game.blackjack_actor_state.{BlackjackActorState => S}
import com.evolution.blackjack.game.command_handler.{BlackjackCommand => C}
import com.evolution.blackjack.game.event_handler.{BlackjackEvent => E}

class DealerHitEventHandler(ctx: ActorContext[C], timer: TimerScheduler[C]) extends BlackjackEventHandler[E.DealerHitEvent.type, S] {

  def handle(event: E.DealerHitEvent.type, currentState: S): S = {
    currentState match {
      case state : S.DealerTurnState => {
        state.cardDeck.takeFirstCards(1) match {
          case Some((card, remainingDeck)) => {
            val updatedDealerCards = state.dealerHand ::: card
            val score = HandScoreCalculator.countScore(updatedDealerCards)
            if (score < 17) {
              ctx.self ! C.DealerHit(None)
            } else {
              ctx.self ! C.DetermineWinners(None)
            }
            state.copy(dealerHand = updatedDealerCards, cardDeck = remainingDeck)
          }
          case None => S.ErrorState(state.seats, "Failed to get next card from deck on dealer hit")
        }
      }
    }
  }
}
