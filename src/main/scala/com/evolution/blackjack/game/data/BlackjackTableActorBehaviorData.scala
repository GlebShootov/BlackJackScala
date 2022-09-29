package com.evolution.blackjack.game.data

import com.evolution.blackjack.domain.data.{Card, CardDeck, PlayerId}

@Deprecated
object BlackjackTableActorBehaviorData {

  sealed trait BehaviorData

  final case class AwaitingBehaviorData() extends BehaviorData // TODO: should be object if no parameters would be added

  final case class PlaceBetsBehaviorData(activePlayers: List[PlayerId],
                                         inactivePlayers: List[PlayerId],
                                         playerBets: Map[PlayerId, BigDecimal]
                                        ) extends BehaviorData

  final case class PlayersTurnBehaviorData(activePlayers: List[PlayerId],
                                           inactivePlayers: List[PlayerId],
                                           playerHandsData: Map[PlayerId, PlayerHandsData],
                                           dealerHand: List[Card],
                                           cardDeck: CardDeck
                                          ) extends BehaviorData

  def emptyPlaceBetsBehaviorData(activePlayersIds: List[PlayerId]): PlaceBetsBehaviorData = {
    PlaceBetsBehaviorData(activePlayersIds, List(), Map())
  }

  def empty(): BlackjackTableActorBehaviorData = {
    BlackjackTableActorBehaviorData(List(), List(), Map(), CardDeck.getCardDeck(), Map())
  }
}

final case class BlackjackTableActorBehaviorData(activePlayers: List[PlayerId],
                                                 inactivePlayers: List[PlayerId],
                                                 playerBets: Map[PlayerId, BigDecimal],
                                                 cardDeck: CardDeck,
                                                 playersCards: Map[PlayerId, List[Card]])
