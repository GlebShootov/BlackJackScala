package com.evolution.blackjack.game.blackjack_actor_state

import com.evolution.blackjack.domain.data.{Card, CardDeck, PlayerId}
import com.evolution.blackjack.game.BlackjackTableActor.{Seats, State}
import com.evolution.blackjack.game.data.{PlayerHandsData, PlayerTurnData}

import scala.collection.immutable.SortedMap

sealed trait BlackjackActorState { // TODO extract states, commands and events to separate classes
  def seats: Seats
}

object BlackjackActorState {
  final case class AwaitingPlayersState(seats: Seats) extends BlackjackActorState

  final case class PlaceBetsState(seats: Seats,
                                  playersBets: Map[PlayerId, BigDecimal]
                                 ) extends BlackjackActorState

  final case class CheckBetsState(seats: Seats,
                                  playersBets: Map[PlayerId, BigDecimal]
                                 ) extends BlackjackActorState

  final case class DealCardsState(seats: Seats,
                                  playersBets: Map[PlayerId, BigDecimal]
                                 ) extends BlackjackActorState

  final case class PlayersTurnState(seats: Seats,
                                    currentPlayerTurnData: PlayerTurnData,
                                    playersHandsDataMap: Map[PlayerId, PlayerHandsData],
                                    dealerHand: List[Card],
                                    cardDeck: CardDeck
                                   ) extends BlackjackActorState

  final case class DealerTurnState(seats: Seats,
                                   playerHandsData: Map[PlayerId, PlayerHandsData],
                                   dealerHand: List[Card],
                                   cardDeck: CardDeck
                                  ) extends BlackjackActorState

  final case class GameEndedState(seats: Seats) extends BlackjackActorState

  final case class ErrorState(seats: Seats, reason: String) extends BlackjackActorState
}
