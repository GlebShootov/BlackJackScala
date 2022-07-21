package com.evolution.blackjack.game

import akka.actor.{Actor, ActorLogging}
import com.evolution.blackjack.domain.data.{Card, CardDeck, PlayerId}
import com.evolution.blackjack.game.data.{HandData, PlayerHandsData, BlackjackTableActorBehaviorData => BehaviorData}
import com.evolution.blackjack.game.BlackjackTableActor._
import com.evolution.blackjack.game.data.BlackjackTableActorBehaviorData.PlayersTurnBehaviorData

class BlackjackTableActor extends Actor with ActorLogging {

  override def receive: Receive = awaitingPlayers()

  private def awaitingPlayers(): Receive = {
    case req @ JoinGame(_) => joinGameWhileAwaiting(req)
  }

  private def placingBets(placeBetsBehaviorData: BehaviorData.PlaceBetsBehaviorData): Receive = {
    case req @ JoinGame(_) => joinGame(req, placeBetsBehaviorData)
    case req @ PlaceBet(_, _) => placingBet(req, placeBetsBehaviorData)
  }

  private def dealingCards(): Receive = {
    // case req @ JoinGame(_) => joinGame(req,)
  }

  private def playersTurn(playerTurnBehaviorData: BehaviorData.PlayersTurnBehaviorData): Receive = {
    case _ =>
  }

  private def dealersTurn(behaviorData: BehaviorData): Receive = {
    case _ =>
  }

  private def joinGameWhileAwaiting(req: JoinGame): Unit = {
    log.debug(s"Player with id ${req.playerId} attempt to join game while waiting players.") // TODO: check auto logging of income messages
    val placeBetsBehaviorData = BehaviorData.emptyPlaceBetsBehaviorData(List(req.playerId))
    context.become(placingBets(placeBetsBehaviorData))
    sender() ! JoinGameSuccResp
  }

  private def joinGame(req: JoinGame, placeBetsBehaviorData: BehaviorData.PlaceBetsBehaviorData): Unit = {
    log.debug(s"Player with id ${req.playerId} attempt to join game while game already started.")
    val newInactivePlayers = req.playerId :: placeBetsBehaviorData.inactivePlayers
    val newBehaviorData = (placeBetsBehaviorData.copy(inactivePlayers = newInactivePlayers))
    context.become(placingBets(newBehaviorData))
    sender() ! JoinGameSuccResp
  }

  private def placingBet(req: PlaceBet, placeBetsBehaviorData: BehaviorData.PlaceBetsBehaviorData): Unit = {
    log.debug(s"Player with id ${req.playerId} attempt to place bet with amount ${req.betAmount}.")
    placeBetsBehaviorData.playerBets.get(req.playerId) match {
      case Some(value) => sender() ! PlaceBetErrResp(s"Player ${req.playerId} already place bet with amount $value")
      case None => {
        sender() ! PlaceBetSuccResp
        val newPlayersBets = placeBetsBehaviorData.playerBets + (req.playerId -> req.betAmount)
        val newBehaviorData = placeBetsBehaviorData.copy(playerBets = newPlayersBets)
        if (newBehaviorData.playerBets.keySet.subsetOf(newBehaviorData.activePlayers.toSet)) {
          val deck = CardDeck.getShuffledDeck(8) // TODO: extract to settings

          newBehaviorData.playerBets.map {
            case (pId, betAm) => PlayerHandsData(pId, List(HandData(betAm,)))
          }
          val playerHandsData = PlayerHandsData()

          val playersTurnBehaviorData = PlayersTurnBehaviorData(
            newBehaviorData.activePlayers, newBehaviorData.inactivePlayers, )
          context.become(playersTurn(newBehaviorData)) // TODO:
        } else {
          context.become(placingBets(newBehaviorData))
        }
      }
    }
  }

  private val dealCards: DealCardsResult = (cardDeck: CardDeck, playerIds: List[PlayerId]) => {

  }

  private case class DealCardsResult(remainingDeck: CardDeck, playersCards: Map[PlayerId, List[Card]], dealerCards: List[Card]) // TODO: move somewhere else
}

object BlackjackTableActor {

  sealed trait Api

  final case class JoinGame(playerId: PlayerId) extends Api
  sealed trait JoinGameResp extends Api
  object JoinGameSuccResp extends JoinGameResp
  final case class JoinGameErrResp(errMsg: String) extends JoinGameResp

  final case class PlaceBet(playerId: PlayerId, betAmount: BigDecimal) extends Api
  sealed trait PlaceBetResp extends Api
  object PlaceBetSuccResp extends PlaceBetResp
  final case class PlaceBetErrResp(errMsg: String) extends PlaceBetResp

  final case class Double(playerId: PlayerId) extends Api
  final case class Insurance(playerId: PlayerId) extends Api
  final case class Hit(playerId: PlayerId) extends Api
  final case class Surrender(playerId: PlayerId) extends Api
  final case class Stay(playerId: PlayerId) extends Api
  final case class EvenMoney(playerId: PlayerId) extends Api
}
