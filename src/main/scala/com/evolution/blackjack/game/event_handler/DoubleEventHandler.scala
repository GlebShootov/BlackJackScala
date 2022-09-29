package com.evolution.blackjack.game.event_handler

import akka.actor.typed.scaladsl.{ActorContext, TimerScheduler}
import com.evolution.blackjack.game.BlackjackTableActor.findNextTurn
import com.evolution.blackjack.game.blackjack_actor_state.{BlackjackActorState => S}
import com.evolution.blackjack.game.command_handler.{BlackjackCommand => C}
import com.evolution.blackjack.game.data.PlayerTurnData
import com.evolution.blackjack.game.event_handler.{BlackjackEvent => E}

class DoubleEventHandler(ctx: ActorContext[C], timer: TimerScheduler[C]) extends BlackjackEventHandler[E.DoubleEvent, S] {

  def handle(event: E.DoubleEvent, currentState: S): S = {
    currentState match {
      case state: S.PlayersTurnState => {
        if (PlayerTurnData(event.playerId, event.handId) == state.currentPlayerTurnData) {
          val updatedPlayerHandsDataAndCardDeck = for {
            playerHandsData <- state.playersHandsDataMap.get(event.playerId).toRight(s"Failed to find active player with id ${event.playerId}")
            playerHand <- playerHandsData.hands.get(event.handId).toRight(s"Failed to find player ${event.playerId} hand ${event.handId}")
            cardResult <- state.cardDeck.takeFirstCards(1).toRight(s"Failed to find next card for double")
            updatedHand = playerHand.copy(betAmount = playerHand.betAmount * 2, cards = playerHand.cards ::: cardResult._1)
            updatedPlayerHandsData = playerHandsData.copy(hands = playerHandsData.hands + (event.playerId -> updatedHand))
          } yield (updatedPlayerHandsData, cardResult._2)

          updatedPlayerHandsDataAndCardDeck match {
            case Left(msg) => S.ErrorState(state.seats, msg)
            case Right((playerHands, remainingDeck)) => {
              val updatedPlayersHands = state.playersHandsDataMap + (event.playerId -> playerHands)

              val nextPlayerTurnData = findNextTurn(state.currentPlayerTurnData.playerId, state.currentPlayerTurnData.handId,
                state.playersHandsDataMap, state.seats)

              nextPlayerTurnData match {
                case Some(v) => state.copy(currentPlayerTurnData = v, playersHandsDataMap = updatedPlayersHands, cardDeck = remainingDeck)
                case None => S.DealerTurnState(state.seats, updatedPlayersHands, state.dealerHand, remainingDeck)
              }
            }
          }
        } else S.ErrorState(state.seats, s"Player ${event.playerId} tries to make double in other players turn")
      }
    }
  }
}
