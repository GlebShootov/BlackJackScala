package com.evolution.blackjack.game.data

final case class TableInfo(tableId: String,
                           currentPlayersQuantity: Integer,
                           maxPlayersQuantity: Integer,
                           minBets: BigDecimal)
