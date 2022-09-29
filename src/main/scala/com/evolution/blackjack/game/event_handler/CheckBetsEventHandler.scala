package com.evolution.blackjack.game.event_handler

import akka.actor.typed.scaladsl.{ActorContext, TimerScheduler}
import com.evolution.blackjack.domain.data.{Balance, PlayerId}
import com.evolution.blackjack.game.blackjack_actor_state.{BlackjackActorState => S}
import com.evolution.blackjack.game.command_handler.{BlackjackCommand => C}
import com.evolution.blackjack.game.event_handler.{BlackjackEvent => E}

class CheckBetsEventHandler(ctx: ActorContext[C], timer: TimerScheduler[C]) extends BlackjackEventHandler[E.CheckBetsEvent, S] {

  def handle(event: E.CheckBetsEvent, currentState: S): S = {
    currentState match {
      case S.CheckBetsState(seats, playersBets) => {
        val validBets = getValidBetsMap(playersBets, event.balances)
        val invalidBets = playersBets.removedAll(validBets.keys)
        // TODO: handle invalid bets
        S.DealCardsState(seats, validBets)
      }
    }
  }

  private def getValidBetsMap(playersBets: Map[PlayerId, BigDecimal], balances: Map[PlayerId, Balance]): Map[PlayerId, BigDecimal] = {
    playersBets.filter { betEntry =>
      balances.get(betEntry._1) match {
        case Some(balance) => balance.amount >= betEntry._2
        case None => false
      }
    }
  }
}
