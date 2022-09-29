package com.evolution.blackjack.game.command_handler

import cats.{Applicative, Id}
import com.evolution.blackjack.domain.data.{Balance, PlayerId}
import com.evolution.blackjack.game.BalanceService
import com.evolution.blackjack.game.blackjack_actor_state.{BlackjackActorState => S}
import com.evolution.blackjack.game.command_handler.{BlackjackCommand => C}
import com.evolution.blackjack.game.event_handler.{BlackjackEvent => E}

class CheckBetsCommandHandler[F[_]: Applicative](balanceService: BalanceService[F]) extends BlackjackCommandHandler[F, S, C.CheckBets, E, String] {

  override type A = Map[PlayerId, Balance]

  override def loadContext(command: C.CheckBets, state: S): F[A] = {
    state match {
      case S.CheckBetsState(_, playersBets) => balanceService.loadBalances(playersBets.keySet)
      case _ => balanceService.loadBalances(state.seats.values.flatten.toSet)
    }
  }

  override def validateCommand(command: C.CheckBets, context: A, state: S): Either[String, Unit] = {
    state match {
      case _ : S.CheckBetsState => Right(())
      case _ => Left(s"Wrong state to check players bets. State: $state")
    }
  }

  override def handleError(c: C.CheckBets, r: String): F[Unit] = Applicative[F].unit

  override def produceEvent(command: C.CheckBets, context: A): List[E] = {
    List(E.CheckBetsEvent(context))
  }

  override def runEffect(command: C.CheckBets, context: A, updatedState: S): F[Unit] = Applicative[F].unit
}
