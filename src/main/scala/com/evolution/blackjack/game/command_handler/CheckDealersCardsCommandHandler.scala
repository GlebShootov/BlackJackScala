package com.evolution.blackjack.game.command_handler

import akka.pattern.StatusReply
import cats.Id
import com.evolution.blackjack.game.blackjack_actor_state.{BlackjackActorState => S}
import com.evolution.blackjack.game.command_handler.{BlackjackCommand => C}
import com.evolution.blackjack.game.data.PlayerTurnData
import com.evolution.blackjack.game.event_handler.{BlackjackEvent => E}

object CheckDealersCardsCommandHandler extends BlackjackCommandHandler[Id, S, C.CheckDealersCard, E, String] {

  override type A = Unit

  override def loadContext(command: C.CheckDealersCard, state: S): Id[A] = ()

  override def validateCommand(command: C.CheckDealersCard, context: A, state: S): Either[String, Unit] = {
    state match {
      case _ : S.DealerTurnState => Right(())
      case _ => Left(s"Rejected due to current state $state")
    }
  }

  override def handleError(c: C.CheckDealersCard, r: String): Id[Unit] = ()

  override def produceEvent(command: C.CheckDealersCard, context: A): List[E] = List(E.CheckDealersCardsEvent())

  override def runEffect(command: C.CheckDealersCard, context: A, updatedState: S): Id[Unit] = ()
}

// ws messages from players
// deserialize message to command (on decoding failure send failure)
// actor processes command
// command -> event
// process event, update game table state
// event persistence
// send ws message to player