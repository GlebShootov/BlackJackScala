package com.evolution.blackjack.game.command_handler

import cats.{Applicative, Id}
import com.evolution.blackjack.game.blackjack_actor_state.{BlackjackActorState => S}
import com.evolution.blackjack.game.command_handler.{BlackjackCommand => C}
import com.evolution.blackjack.game.event_handler.BlackjackEvent.StartGameEvent
import com.evolution.blackjack.game.event_handler.{BlackjackEvent => E}

class StartGameCommandHandler[F[_]: Applicative] extends BlackjackCommandHandler[F, S, C.StartGame, E, String] {

  override type A = Unit

  override def loadContext(command: C.StartGame, state: S): F[A] = Applicative[F].unit

  override def validateCommand(command: C.StartGame, context: A, state: S): Either[String, Unit] = {
    state match {
      case S.AwaitingPlayersState || S.GameEndedState => {
        if (state.seats.values.flatten.isEmpty) Left("No players found to start game")
        else Right(())
      }
      case _ => Left(s"Cant start game in state: $state")
    }
  }

  override def handleError(c: C.StartGame, r: String): F[Unit] = Applicative[F].unit

  override def produceEvent(command: C.StartGame, context: A): List[E] = List(StartGameEvent)

  override def runEffect(command: C.StartGame, context: A, updatedState: S): F[Unit] = Applicative[F].unit
}
