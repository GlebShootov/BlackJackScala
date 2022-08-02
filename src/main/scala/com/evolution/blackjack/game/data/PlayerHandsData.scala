package com.evolution.blackjack.game.data

import com.evolution.blackjack.domain.data.PlayerId

import scala.collection.immutable.ListMap

/*
 * player id
 * list of hands
 *  hand - bet, cards
 *
 */

final case class PlayerHandsData(playerId: PlayerId, hands: ListMap[HandId, HandData]) // TODO: need better name
