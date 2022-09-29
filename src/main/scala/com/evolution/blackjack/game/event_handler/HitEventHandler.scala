package com.evolution.blackjack.game.event_handler

import akka.actor.typed.scaladsl.{ActorContext, TimerScheduler}
import akka.pattern.StatusReply
import akka.persistence.typed.scaladsl.Effect
import com.evolution.blackjack.game.BlackjackTableActor.{HitEvent, findNextTurn, findPlayerHand}
import com.evolution.blackjack.game.blackjack_actor_state.{BlackjackActorState => S}
import com.evolution.blackjack.game.command_handler.{BlackjackCommand => C}
import com.evolution.blackjack.game.data.{PlayerTurnData, PlayersTurnOption}
import com.evolution.blackjack.game.event_handler.{BlackjackEvent => E}

class HitEventHandler(ctx: ActorContext[C], timer: TimerScheduler[C]) extends BlackjackEventHandler[E.HitEvent, S] {

  def handle(event: E.HitEvent, currentState: S): S = {

    currentState match {
      case state: S.PlayersTurnState => {
        if (state.currentPlayerTurnData == PlayerTurnData(event.playerId, event.handId)) {

          val updatedPlayerHandsDataAndCardDeck = for {
            playerHandsData <- state.playersHandsDataMap.get(event.playerId).toRight(s"Failed to find active player with id ${event.playerId}")
            playerHand <- playerHandsData.hands.get(event.handId).toRight(s"Failed to find player ${event.playerId} hand ${event.handId}")
            cardResult <- state.cardDeck.takeFirstCards(1).toRight(s"Failed to find next card for double")
            updatedHand = playerHand.copy(cards = playerHand.cards ::: cardResult._1)
            updatedPlayerHandsData = playerHandsData.copy(hands = playerHandsData.hands + (event.playerId -> updatedHand))
          } yield (updatedPlayerHandsData, cardResult._2)

          updatedPlayerHandsDataAndCardDeck match {
            case Left(msg) => S.ErrorState(state.seats, msg)
            case Right(v) => {
              val updatedPlayersHands = state.playersHandsDataMap + (event.playerId -> v._1)
              state.copy(playersHandsDataMap = updatedPlayersHands, currentPlayerTurnData = state.currentPlayerTurnData)
            }
          }




//          findPlayerHand(event.playerId, event.handId, state.playersHandsDataMap) match {
//            case Some(data) => {
//              state.cardDeck.takeFirstCards(1) match {
//                case Some((card :: Nil, remainingDeck)) => {
//
//                  val nextPlayerTurnData = findNextTurn(state.currentPlayerTurnData.playerId, state.currentPlayerTurnData.handId,
//                    state.playersHandsDataMap, state.seats)
//
//                  nextPlayerTurnData match {
//                    case Some(v) => state.copy(currentPlayerTurnData = v, playersHandsDataMap = updatedPlayersHands, cardDeck = remainingDeck)
//                    case None => S.DealerTurnState(state.seats, updatedPlayersHands, state.dealerHand, remainingDeck)
//                  }
//
////                  if (data.playerTurnHistory.isEmpty || data.playerTurnHistory.forall(_ == PlayersTurnOption.Stay)) {
////                    Effect.persist(HitEvent(playerId, handId, card, remainingDeck))
////                      .thenRun(_ => replyTo ! StatusReply.ack())
////                  } else {
////                    Effect.none.thenRun(_ => replyTo ! StatusReply.error("Not allowed to hit"))
////                  }
//                }
//                case None => {
//                  Effect.none.thenRun(_ => replyTo ! StatusReply.error("Failed to get hit card from deck"))
//                }
//              }
//            }
//            case None => {
//              Effect.none
//                .thenRun(_ => replyTo ! StatusReply.error(s"Failed to find player $playerId hand $handId"))
//            }
//          }
//
//        } else S.ErrorState(state.seats, s"Player ${event.playerId} tries to make hit in other players turn")
      }
    }
  }
  }
}
