package com.evolution.blackjack.game.command_handler

import akka.pattern.StatusReply
import cats.Id
import com.evolution.blackjack.game.blackjack_actor_state.{BlackjackActorState, BlackjackActorState => S}
import com.evolution.blackjack.game.command_handler.{BlackjackCommand => C}
import com.evolution.blackjack.game.event_handler.{BlackjackEvent => E}

object QuitGameCommandHandler extends BlackjackCommandHandler[Id, S, C.QuitGame, E, String] {

  type A = Unit

  override def loadContext(command: C.QuitGame, state: S): Id[A] = ()

  override def validateCommand(command: C.QuitGame, context: A, state: S): Either[String, Unit] = {
    state match {
      case _ => {
        if (state.seats.values.flatten.toList.contains(command.playerId)) Right()
        else Left(s"Failed to find player ${command.playerId} in the game")
      }
    }
  }

  override def handleError(command: C.QuitGame, msg: String): Id[Unit] = {
    command.replyTo ! StatusReply.error(msg)
  }

  override def produceEvent(command: C.QuitGame, context: A): List[E] = List(E.QuitGameEvent(command.playerId))

  override def runEffect(command: C.QuitGame, context: A, updatedState: S): Id[Unit] = { // TODO: change to unit
    command.replyTo ! StatusReply.ack()
  }
}
