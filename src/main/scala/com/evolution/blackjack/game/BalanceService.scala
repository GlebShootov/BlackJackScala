package com.evolution.blackjack.game

import com.evolution.blackjack.domain.data.{Balance, PlayerId}

trait BalanceService[F[_]] {

  def loadBalance(playerId: PlayerId): F[Balance]

  def loadBalances(playerIds: Set[PlayerId]): F[Map[PlayerId, Balance]]
}
