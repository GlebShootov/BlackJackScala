package com.evolution.blackjack.game.command_handler

import akka.pattern.StatusReply
import cats.Id
import com.evolution.blackjack.game.blackjack_actor_state.{BlackjackActorState => S}
import com.evolution.blackjack.game.command_handler.{BlackjackCommand => C}
import com.evolution.blackjack.game.event_handler.BlackjackEvent.PlaceBetEvent
import com.evolution.blackjack.game.event_handler.{BlackjackEvent => E}

object PlaceBetCommandHandler extends BlackjackCommandHandler[Id, S, C.PlaceBet, E, String] {

  override type A = Unit

  override def loadContext(command: C.PlaceBet, state: S): Id[A] = ()

  override def validateCommand(command: C.PlaceBet, context: A, state: S): Either[String, Unit] = {
    state match {
      case S.PlaceBetsState(seats, playersBets) => {
        if (seats.values.flatten.toList.contains(command.playerId)) {
          playersBets.get(command.playerId) match {
            case Some(value) => Left(s"Player ${command.playerId} already place bet with amount $value")
            case None => Right(())
          }
        } else Left(s"Rejected to place bet for player ${command.playerId} due to he is not sitting under the table")
      }
      case _ => Left(s"Currently place bet is not allowed")
    }
  }

  override def handleError(c: C.PlaceBet, r: String): Id[Unit] = {
    c.replyTo ! StatusReply.error(r)
  }

  override def produceEvent(command: C.PlaceBet, context: A): List[E] = {
    List(PlaceBetEvent(command.playerId, command.betAmount))
  }

  override def runEffect(command: C.PlaceBet, context: A, updatedState: S): Id[Unit] = {
    command.replyTo ! StatusReply.ack()
  }
}
