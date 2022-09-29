package com.evolution.blackjack.game.event_handler

import akka.actor.typed.scaladsl.{ActorContext, TimerScheduler}
import com.evolution.blackjack.domain.data.{CardDeck, PlayerId}
import com.evolution.blackjack.game.BlackjackTableActor.findFirstPlayerTurn
import com.evolution.blackjack.game.blackjack_actor_state.{BlackjackActorState => S}
import com.evolution.blackjack.game.command_handler.{BlackjackCommand => C}
import com.evolution.blackjack.game.data.{HandData, HandId, PlayerHandsData}
import com.evolution.blackjack.game.event_handler.{BlackjackEvent => E}

import java.util.UUID
import scala.annotation.tailrec
import scala.collection.immutable.ListMap

class DealCardsEventHandler(ctx: ActorContext[C], timer: TimerScheduler[C]) extends BlackjackEventHandler[E.DealCardsEvent.type, S] {

  def handle(event: E.DealCardsEvent.type, currentState: S): S = {
    currentState match {
      case state: S.DealCardsState => {
        val deck = CardDeck.getShuffledDeck(8) // TODO:  wrap to F[] // extract to settings
        val activePlayers = state.playersBets.keySet.toList
        val result = for {
          result <- dealCards(activePlayers, Map.empty, state.playersBets, deck)
          firstPlayerTurn <- findFirstPlayerTurn(result._1, state.seats).toRight("Failed to find first player turn")
          result2 <- result._2.takeFirstCards(2).toRight(s"Not enough cards in deck to deal dealer")
        } yield S.PlayersTurnState(state.seats, firstPlayerTurn, result._1, result2._1, result2._2)

        result match {
          case Left(msg) => S.ErrorState(currentState.seats, msg)
          case Right(nextState) => nextState
        }
      }
    }
  }

  @tailrec
  private def dealCards(activePlayers: List[PlayerId],
                playerHands: Map[PlayerId, PlayerHandsData],
                playersBets: Map[PlayerId, BigDecimal],
                deck: CardDeck): Either[String, (Map[PlayerId, PlayerHandsData], CardDeck)] = {
    activePlayers match {
      case Nil => Right((playerHands, deck))
      case playerId :: remainingActivePlayers => {
        deck.takeFirstCards(2) match {
          case None => Left(s"Not enough cards in deck to deal player $playerId")
          case Some(result) => {
            val playerHandsData = playersBets.get(playerId).toRight(s"Failed to find bet amount for player $playerId")
              .map { amount =>
                val handId = HandId(UUID.randomUUID().toString)
                val handData = HandData(handId, amount, result._1)
                PlayerHandsData(playerId, ListMap(handId -> handData))
              }
            playerHandsData match {
              case Left(msg) => Left(msg)
              case Right(data) =>
                dealCards(remainingActivePlayers, playerHands + (data.playerId -> data), playersBets, result._2)
            }
          }
        }
      }
    }
  }
}
