package com.evolution.blackjack.game.command_handler

import akka.pattern.StatusReply
import cats.Id
import com.evolution.blackjack.game.blackjack_actor_state.{BlackjackActorState => S}
import com.evolution.blackjack.game.command_handler.{BlackjackCommand => C}
import com.evolution.blackjack.game.event_handler.BlackjackEvent.PlaceBetEvent
import com.evolution.blackjack.game.event_handler.{BlackjackEvent => E}

object PlaceBetTimeoutCommandHandler extends BlackjackCommandHandler[Id, S, C.PlaceBetTimeout, E, String] {

  override type A = Unit

  override def loadContext(command: C.PlaceBetTimeout, state: S): Id[A] = ()

  override def validateCommand(command: C.PlaceBetTimeout, context: A, state: S): Either[String, Unit] = {
    state match {
      case _ : S.PlaceBetsState => Right(())
      case _ => Left(s"Currently place bet is not allowed")
    }
  }

  override def handleError(c: C.PlaceBetTimeout, r: String): Id[Unit] = ()

  override def produceEvent(command: C.PlaceBetTimeout, context: A): List[E] = {
    List(E.PlaceBetTimeoutEvent)
  }

  override def runEffect(command: C.PlaceBetTimeout, context: A, updatedState: S): Id[Unit] = ()
}
