package com.evolution.blackjack.game.event_handler

import com.evolution.blackjack.domain.data.{Balance, Card, CardDeck, PlayerId}
import com.evolution.blackjack.game.data.{HandId, PlayerHandsData, PlayerTurnData}

sealed trait BlackjackEvent

object BlackjackEvent {
  final object StartGameEvent extends BlackjackEvent
  final case class JoinGameEvent(playerId: PlayerId, seatId: Int) extends BlackjackEvent
  final case class QuitGameEvent(playerId: PlayerId) extends BlackjackEvent
  final case class PlaceBetEvent(playerId: PlayerId, betAmount: BigDecimal) extends BlackjackEvent
  final object PlaceBetTimeoutEvent extends BlackjackEvent
  final case class CheckBetsEvent(balances: Map[PlayerId, Balance]) extends BlackjackEvent
  final object DealCardsEvent/*(/*playerHandsData: Map[PlayerId, PlayerHandsData], dealerCards: List[Card],
                                  cardDeck: CardDeck, nextPlayerTurn: PlayerTurnData*/)*/ extends BlackjackEvent
//  final case class DealCardsFailEvent(reason: String) extends BlackjackEvent
  final case class DoubleEvent(playerId: PlayerId, handId: HandId) extends BlackjackEvent
  //final case class Insurance(playerId: PlayerId, handId: HandId) extends BlackjackEvent
  final case class HitEvent(playerId: PlayerId, handId: HandId) extends BlackjackEvent
  //  final case class SurrenderEvent(playerId: PlayerId, handId: HandId) extends BlackjackEvent
  final case class StayEvent(playerId: PlayerId, handId: HandId) extends BlackjackEvent
  //  final case class EvenMoneyEvent(playerId: PlayerId, handId: HandId) extends BlackjackEvent
  final object CheckDealersCardsEvent extends BlackjackEvent
  final object DealerHitEvent extends BlackjackEvent
  final object DetermineWinnersEvent extends BlackjackEvent
}
