package com.evolution.blackjack.game.command_handler

import cats.Id
import com.evolution.blackjack.game.blackjack_actor_state.{BlackjackActorState => S}
import com.evolution.blackjack.game.command_handler.{BlackjackCommand => C}
import com.evolution.blackjack.game.event_handler.{BlackjackEvent => E}

object DealerHitCommandHandler extends BlackjackCommandHandler[Id, S, C.DealerHit, E, String] {

  override type A = Unit

  override def loadContext(command: C.DealerHit, state: S): Id[A] = ()

  override def validateCommand(command: C.DealerHit, context: A, state: S): Either[String, Unit] = {
    state match {
      case _ : S.DealerTurnState => Right(())
      case _ => Left(s"Rejected due to current state $state")
    }
  }

  override def handleError(c: C.DealerHit, r: String): Id[Unit] = ()

  override def produceEvent(command: C.DealerHit, context: A): List[E] = List(E.DealerHitEvent())

  override def runEffect(command: C.DealerHit, context: A, updatedState: S): Id[Unit] = ()
}
