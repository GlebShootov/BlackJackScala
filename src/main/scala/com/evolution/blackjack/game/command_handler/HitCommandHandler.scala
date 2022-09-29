package com.evolution.blackjack.game.command_handler

import akka.pattern.StatusReply
import cats.Id
import com.evolution.blackjack.game.blackjack_actor_state.{BlackjackActorState => S}
import com.evolution.blackjack.game.command_handler.{BlackjackCommand => C}
import com.evolution.blackjack.game.data.{PlayerTurnData, PlayersTurnOption}
import com.evolution.blackjack.game.event_handler.{BlackjackEvent => E}

object HitCommandHandler extends BlackjackCommandHandler[Id, S, C.Hit, E, String] {

  override type A = Unit

  override def loadContext(command: C.Hit, state: S): Id[A] = ()

  override def validateCommand(command: C.Hit, context: A, state: S): Either[String, Unit] = {
    state match {
      case playerTurnState : S.PlayersTurnState => {
        if (PlayerTurnData(command.playerId, command.handId) == playerTurnState.currentPlayerTurnData) {
          val playerHand = for {
            playerHandsData <- playerTurnState.playersHandsDataMap.get(command.playerId).toRight(s"Failed to find active player with id ${command.playerId}")
            playerHand <- playerHandsData.hands.get(command.handId).toRight(s"Failed to find player ${command.playerId} hand ${command.handId}")
          } yield playerHand

          playerHand match {
            case Left(msg) => Left(msg)
            case Right(hand) => {
              if (hand.playerTurnHistory.forall(_ == PlayersTurnOption.Hit)) Right(())
              else (Left(s"Double is not allowed already"))
            }
          }
        } else Left(s"Rejected due to not your turn yet")
      }
      case _ => Left(s"Rejected due to not your turn yet")
    }
  }

  override def handleError(c: C.Hit, r: String): Id[Unit] = {
    c.replyTo ! StatusReply.error(r)
  }

  override def produceEvent(command: C.Hit, context: A): List[E] = List(E.HitEvent(command.playerId, command.handId))

  override def runEffect(command: C.Hit, context: A, updatedState: S): Id[Unit] = {
    command.replyTo ! StatusReply.ack()
  }
}
