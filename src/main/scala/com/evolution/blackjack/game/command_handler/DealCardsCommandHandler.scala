package com.evolution.blackjack.game.command_handler

import cats.Id
import com.evolution.blackjack.game.blackjack_actor_state.BlackjackActorState.DealCardsState
import com.evolution.blackjack.game.blackjack_actor_state.{BlackjackActorState => S}
import com.evolution.blackjack.game.command_handler.{BlackjackCommand => C}
import com.evolution.blackjack.game.event_handler.{BlackjackEvent => E}

object DealCardsCommandHandler extends BlackjackCommandHandler[Id, S, C.DealCards, E, String] {

  override type A = Unit

  override def loadContext(command: C.DealCards, state: S): Id[A] = ()

  override def validateCommand(command: C.DealCards, context: A, state: S): Either[String, Unit] = {
    state match {
      case _ : DealCardsState => Right(())
      case _ => Left(s"Rejected to deal cards at state $state")
    }
  }

  override def handleError(c: C.DealCards, r: String): Id[Unit] = ()

  override def produceEvent(command: C.DealCards, context: A): List[E] = List(E.DealCardsEvent())

  override def runEffect(command: C.DealCards, context: A, updatedState: S): Id[Unit] = ()
}
