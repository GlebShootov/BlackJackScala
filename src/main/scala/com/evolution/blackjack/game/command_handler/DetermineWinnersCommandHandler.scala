package com.evolution.blackjack.game.command_handler

import cats.Id
import com.evolution.blackjack.game.blackjack_actor_state.{BlackjackActorState => S}
import com.evolution.blackjack.game.command_handler.{BlackjackCommand => C}
import com.evolution.blackjack.game.event_handler.{BlackjackEvent => E}

object DetermineWinnersCommandHandler extends BlackjackCommandHandler[Id, S, C.DetermineWinners, E, String] {

  override type A = Unit

  override def loadContext(command: C.DetermineWinners, state: S): Id[A] = ()

  override def validateCommand(command: C.DetermineWinners, context: A, state: S): Either[String, Unit] = {
    state match {
      case _ : S.GameEndedState => Right(())
      case _ => Left(s"Rejected due to current state $state")
    }
  }

  override def handleError(c: C.DetermineWinners, r: String): Id[Unit] = ()

  override def produceEvent(command: C.DetermineWinners, context: A): List[E] = List(E.DetermineWinnersEvent())

  override def runEffect(command: C.DetermineWinners, context: A, updatedState: S): Id[Unit] = ()
}
