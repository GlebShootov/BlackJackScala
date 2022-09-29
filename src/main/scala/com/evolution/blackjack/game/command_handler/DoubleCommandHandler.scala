package com.evolution.blackjack.game.command_handler

import akka.pattern.StatusReply
import cats.effect.Sync
import cats.{Applicative, Id}
import com.evolution.blackjack.domain.data.Balance
import com.evolution.blackjack.game.BalanceService
import com.evolution.blackjack.game.blackjack_actor_state.{BlackjackActorState => S}
import com.evolution.blackjack.game.command_handler.{BlackjackCommand => C}
import com.evolution.blackjack.game.data.PlayerTurnData
import com.evolution.blackjack.game.event_handler.BlackjackEvent.StartGameEvent
import com.evolution.blackjack.game.event_handler.{BlackjackEvent => E}

class DoubleCommandHandler[F[_]: Sync : Applicative](balanceService: BalanceService[F]) extends BlackjackCommandHandler[F, S, C.Double, E, String] {

  override type A = Balance

  override def loadContext(command: C.Double, state: S): F[A] = {
    balanceService.loadBalance(command.playerId)
  }

  override def validateCommand(command: C.Double, context: A, state: S): Either[String, Unit] = {
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
              if (hand.playerTurnHistory.isEmpty) Right(())
              else (Left(s"Double is not allowed already"))
            }
          }
        } else Left(s"Rejected due to not your turn yet")
      }
      case _ => Left(s"Rejected due to not your turn yet")
    }
  }

  override def handleError(c: C.Double, r: String): F[Unit] = {
    Sync[F].delay(c.replyTo ! StatusReply.error(r))
  }

  override def produceEvent(command: C.Double, context: A): List[E] = List(E.DoubleEvent(command.playerId, command.handId))

  override def runEffect(command: C.Double, context: A, updatedState: S): F[Unit] = {
    Sync[F].delay(command.replyTo ! StatusReply.ack())
  }
}
