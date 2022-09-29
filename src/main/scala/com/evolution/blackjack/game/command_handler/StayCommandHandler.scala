package com.evolution.blackjack.game.command_handler

import akka.pattern.StatusReply
import cats.Id
import com.evolution.blackjack.game.blackjack_actor_state.{BlackjackActorState => S}
import com.evolution.blackjack.game.command_handler.{BlackjackCommand => C}
import com.evolution.blackjack.game.data.{PlayerTurnData, PlayersTurnOption}
import com.evolution.blackjack.game.event_handler.{BlackjackEvent => E}

object StayCommandHandler extends BlackjackCommandHandler[Id, S, C.Stay, E, String] {

  override type A = Unit

  override def loadContext(command: C.Stay, state: S): Id[A] = ()

  override def validateCommand(command: C.Stay, context: A, state: S): Either[String, Unit] = {
    state match {
      case playerTurnState : S.PlayersTurnState => {
        if (PlayerTurnData(command.playerId, command.handId) == playerTurnState.currentPlayerTurnData) {
          val playerHand = for {
            playerHandsData <- playerTurnState.playersHandsDataMap.get(command.playerId).toRight(s"Failed to find active player with id ${command.playerId}")
            playerHand <- playerHandsData.hands.get(command.handId).toRight(s"Failed to find player ${command.playerId} hand ${command.handId}")
          } yield playerHand

          playerHand match {
            case Left(msg) => Left(msg)
            case Right(_) => Right(())
          }
        } else Left(s"Rejected due to not your turn yet")
      }
      case _ => Left(s"Rejected due to not your turn yet")
    }
  }

  override def handleError(c: C.Stay, r: String): Id[Unit] = {
    c.replyTo ! StatusReply.error(r)
  }

  override def produceEvent(command: C.Stay, context: A): List[E] = List(E.HitEvent(command.playerId, command.handId))

  override def runEffect(command: C.Stay, context: A, updatedState: S): Id[Unit] = {
    command.replyTo ! StatusReply.ack()
  }
}
