package com.evolution.blackjack.game.data

import com.evolution.blackjack.domain.data.PlayerId

/*
 * player id
 * list of hands
 *  hand - bet, cards
 *
 */

final case class PlayerHandsData(playerId: PlayerId, hands: Map[HandId, HandData]) // TODO: need better name
