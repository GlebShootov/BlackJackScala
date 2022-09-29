package com.evolution.blackjack.game.command_handler

import akka.pattern.StatusReply
import cats.Id
import cats.effect.Sync
import com.evolution.blackjack.domain.data.Balance
import com.evolution.blackjack.game.BalanceService
import com.evolution.blackjack.game.BlackjackTableActor.Seats
import com.evolution.blackjack.game.blackjack_actor_state.{BlackjackActorState, BlackjackActorState => S}
import com.evolution.blackjack.game.event_handler.{BlackjackEvent => E}
import com.evolution.blackjack.game.command_handler.{BlackjackCommand => C}

class JoinGameCommandHandler[F[_]: Sync](balanceService: BalanceService[F]) extends BlackjackCommandHandler[F, S, C.JoinGame, E, String] {

  type A = Balance

  override def loadContext(command: C.JoinGame, state: S): F[A] = {
    balanceService.loadBalance(command.playerId)
  }

  override def validateCommand(command: C.JoinGame, context: A, state: S): Either[String, Unit] = {
    state match {
      case _ : BlackjackActorState.ErrorState => Left("Rejected to join due to game is in error state")
      case _ => {
        state.seats.get(command.seatId) match {
          case Some(v) => Left(s"Seat $v is already taken")
          case None => Right(())
        }
      }
    }
  }

  override def handleError(command: C.JoinGame, msg: String): F[Unit] = {
    Sync[F].delay(command.replyTo ! StatusReply.error(msg))
  }

  override def produceEvent(command: C.JoinGame, context: A): List[E] = {
    val event = E.JoinGameEvent(command.playerId, command.seatId)
    List(event)
  }

  override def runEffect(command: C.JoinGame, context: A, updatedState: S): F[Unit] = {
    Sync[F].delay(command.replyTo ! StatusReply.ack())
  }
}
