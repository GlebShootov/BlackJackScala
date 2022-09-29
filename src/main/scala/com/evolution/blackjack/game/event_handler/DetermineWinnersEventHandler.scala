package com.evolution.blackjack.game.event_handler

import akka.actor.typed.scaladsl.{ActorContext, TimerScheduler}
import com.evolution.blackjack.game.BlackjackTableActor.{GameEndedState, StartGame, startGameDelayDuration}
import com.evolution.blackjack.game.HandScoreCalculator
import com.evolution.blackjack.game.blackjack_actor_state.{BlackjackActorState => S}
import com.evolution.blackjack.game.command_handler.{BlackjackCommand => C}
import com.evolution.blackjack.game.data.HandScoreData
import com.evolution.blackjack.game.event_handler.{BlackjackEvent => E}

class DetermineWinnersEventHandler(ctx: ActorContext[C], timer: TimerScheduler[C]) extends BlackjackEventHandler[E.DetermineWinnersEvent.type, S] {

  def handle(event: E.DetermineWinnersEvent.type, currentState: S): S = {
    currentState match {
      case state : S.DealerTurnState => {
        val dealerScore = HandScoreCalculator.countScore(state.dealerHand)
        val dealerHandScoreData = HandScoreData(dealerScore, state.dealerHand)

        val playersHandScoreData = state.playerHandsData.toList.flatMap { data =>
          data._2.hands.map { handData =>
            val score = HandScoreCalculator.countScore(handData._2.cards)
            (data._1, HandScoreData(score, handData._2.cards))
          }
        }

        val winners = playersHandScoreData.filter(_._2.compare(dealerHandScoreData) > 0)
        val drawPlayers = playersHandScoreData.filter(_._2.compare(dealerHandScoreData) == 0)

        ctx.log.debug(s"Winners: $winners")
        ctx.log.debug(s"Draw players: $drawPlayers")

        timer.startSingleTimer(C.StartGame(None), startGameDelayDuration)
        S.GameEndedState(seats = state.seats)
      }
    }
  }
}
