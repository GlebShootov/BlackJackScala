package com.evolution.blackjack.game.command_handler

import akka.Done
import akka.actor.typed.ActorRef
import akka.pattern.StatusReply
import com.evolution.blackjack.domain.data.{Balance, PlayerId}
import com.evolution.blackjack.game.data.HandId

sealed trait BlackjackCommand {
  type A
  def context: Option[Either[String, A]]
}

object BlackjackCommand {

  final case class StartGame(context: Option[Either[String, Unit]]) extends BlackjackCommand {
    type A = Unit
  }

  final case class JoinGame(playerId: PlayerId, seatId: Int, replyTo: ActorRef[StatusReply[Done]], context: Option[Either[String, Balance]]) extends BlackjackCommand {
    type A = Balance
  }

  final case class QuitGame(playerId: PlayerId, replyTo: ActorRef[StatusReply[Done]], context: Option[Either[String, Unit]]) extends BlackjackCommand {
    type A = Unit
  }

  final case class DealCards(context: Option[Either[String, Unit]]) extends BlackjackCommand {
    type A = Unit
  }

  final case class PlaceBet(playerId: PlayerId, betAmount: BigDecimal, replyTo: ActorRef[StatusReply[Done]], context: Option[Either[String, Unit]]) extends BlackjackCommand {
    type A = Unit
  }

  final case class PlaceBetTimeout(context: Option[Either[String, Unit]]) extends BlackjackCommand {
    type A = Unit
  }

  final case class CheckBets(context: Option[Either[String, Map[PlayerId, Balance]]]) extends BlackjackCommand {
    type A = Map[PlayerId, Balance]
  }

//  final case class GameEnded() extends BlackjackCommand
  final case class Double(playerId: PlayerId, handId: HandId, replyTo: ActorRef[StatusReply[Done]], context: Option[Either[String, Balance]]) extends BlackjackCommand {
    type A = Balance
  }

  //final case class Insurance(playerId: PlayerId, handId: HandId, replyTo: ActorRef[StatusReply[Done]]) extends BlackjackCommand
  final case class Hit(playerId: PlayerId, handId: HandId, replyTo: ActorRef[StatusReply[Done]], context: Option[Either[String, Unit]]) extends BlackjackCommand {
    type A = Unit
  }
  //  final case class Surrender(playerId: PlayerId, handId: HandId, replyTo: ActorRef[StatusReply[Done]]) extends BlackjackCommand
  final case class Stay(playerId: PlayerId, handId: HandId, replyTo: ActorRef[StatusReply[Done]], context: Option[Either[String, Unit]]) extends BlackjackCommand {
    type A = Unit
  }
  //  final case class EvenMoney(playerId: PlayerId, handId: HandId, replyTo: ActorRef[StatusReply[Done]]) extends BlackjackCommand
  final case class CheckDealersCard(context: Option[Either[String, Unit]]) extends BlackjackCommand {
    type A = Unit
  }
  final case class DealerHit(context: Option[Either[String, Unit]]) extends BlackjackCommand {
    type A = Unit
  }
  final case class DetermineWinners(context: Option[Either[String, Unit]]) extends BlackjackCommand {
    type A = Unit
  }
}
