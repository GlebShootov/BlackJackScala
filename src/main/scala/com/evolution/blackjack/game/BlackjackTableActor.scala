package com.evolution.blackjack.game

import akka.Done
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior, SupervisorStrategy}
import akka.pattern.StatusReply
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, RetentionCriteria}
import com.evolution.blackjack.domain.data.{Card, CardDeck, PlayerId}
import com.evolution.blackjack.game.data.{HandData, HandId, PlayerHandsData}

import java.util.UUID
import scala.annotation.tailrec
import scala.concurrent.duration.DurationInt

object BlackjackTableActor {

  sealed trait State
  final object AwaitingPlayersState extends State

  final case class PlaceBetsState(activePlayers: List[PlayerId],
                                  inactivePlayers: List[PlayerId],
                                  playerBets: Map[PlayerId, BigDecimal]
                                 ) extends State

  final case class DealCardsState(inactivePlayers: List[PlayerId],
                                  playerBets: Map[PlayerId, BigDecimal]
                                 ) extends State

  final case class PlayersTurnState(inactivePlayers: List[PlayerId],
                                    playerHandsData: Map[PlayerId, PlayerHandsData],
                                    dealerHand: List[Card],
                                    cardDeck: CardDeck
                                   ) extends State

  final case class ErrorState(inactivePlayers: List[PlayerId], reason: String) extends State

  sealed trait Command
  final case class JoinGame(playerId: PlayerId, replyTo: ActorRef[StatusReply[Done]]) extends Command
  private object DealCards extends Command

  final case class PlaceBet(playerId: PlayerId, betAmount: BigDecimal, replyTo: ActorRef[StatusReply[Done]]) extends Command
  object PlaceBetSuccResp extends Command
  final case class PlaceBetErrResp(errMsg: String) extends Command

  final case class Double(playerId: PlayerId, handId: HandId, replyTo: ActorRef[StatusReply[Done]]) extends Command
  //final case class Insurance(playerId: PlayerId, handId: HandId, replyTo: ActorRef[StatusReply[Done]]) extends Command
  final case class Hit(playerId: PlayerId, handId: HandId, replyTo: ActorRef[StatusReply[Done]]) extends Command
  final case class Surrender(playerId: PlayerId, handId: HandId, replyTo: ActorRef[StatusReply[Done]]) extends Command
  final case class Stay(playerId: PlayerId, handId: HandId, replyTo: ActorRef[StatusReply[Done]]) extends Command
  final case class EvenMoney(playerId: PlayerId, handId: HandId, replyTo: ActorRef[StatusReply[Done]]) extends Command

  sealed trait Event
  private final case class JoinGameEvent(playerId: PlayerId) extends Event
  private final case class PlaceBetEvent(playerId: PlayerId, betAmount: BigDecimal) extends Event
  private final case class DealCardsEvent(playerHandsData: Map[PlayerId, PlayerHandsData], dealerCards: List[Card], cardDeck: CardDeck) extends Event
  private final case class DealCardsFailEvent(reason: String) extends Event
  private final case class DoubleEvent(playerId: PlayerId, handId: HandId) extends Event
  //private final case class Insurance(playerId: PlayerId, handId: HandId) extends Event
  private final case class HitEvent(playerId: PlayerId, handId: HandId) extends Event
  private final case class SurrenderEvent(playerId: PlayerId, handId: HandId) extends Event
  private final case class StayEvent(playerId: PlayerId, handId: HandId) extends Event
  private final case class EvenMoneyEvent(playerId: PlayerId, handId: HandId) extends Event

  def apply(gameId: String): Behavior[Command] = {
    Behaviors.withTimers[Command] { timer =>
      Behaviors.setup[Command] { ctx =>
        EventSourcedBehavior[Command, Event, State](
          PersistenceId("BlackjackGame", gameId),
          AwaitingPlayersState,
          (state, command) => commandHandler(state, command, ctx, timer),
          (state, event) => eventHandler(state, event, ctx, timer))
          .withRetention(RetentionCriteria.snapshotEvery(numberOfEvents = 100, keepNSnapshots = 3))
          .onPersistFailure(SupervisorStrategy.restartWithBackoff(200.millis, 5.seconds, 0.1))
      }
    }
  }

  private def commandHandler(state: State, command: Command,
                             ctx: ActorContext[Command], timer: TimerScheduler[Command]): Effect[Event, State] = {
    state match {
      case AwaitingPlayersState => awaitingPlayersCommandHandler(state, command, ctx, timer)
      case placeBetsState : PlaceBetsState => placingBetsCommandHandler(placeBetsState, command, ctx, timer)
      case dealCardsState: DealCardsState => dealCardsCommandHandler(dealCardsState, command, ctx, timer)
      case playersTurnState: PlayersTurnState => playersTurnCommandHandler(playersTurnState, command, ctx, timer)
    }
  }

  private def awaitingPlayersCommandHandler(state: State, command: Command,
                                            ctx: ActorContext[Command], timer: TimerScheduler[Command]): Effect[Event, State] = {
    command match {
      case JoinGame(playerId, replyTo) => {
        Effect
          .persist(JoinGameEvent(playerId))
          .thenRun(_ => replyTo ! StatusReply.Ack)
      }
    }
  }

  private def placingBetsCommandHandler(state: PlaceBetsState, command: Command,
                                        ctx: ActorContext[Command], timer: TimerScheduler[Command]): Effect[Event, State] = {
    command match {
      case JoinGame(playerId, replyTo) => {
        Effect.persist(JoinGameEvent(playerId))
          .thenRun(_ => replyTo ! StatusReply.Ack)
      }
      case PlaceBet(playerId, amount, replyTo) => {
        ctx.log.debug(s"Player with id $playerId attempt to place bet with amount $amount.")
        state.playerBets.get(playerId) match {
          case Some(value) => {
            Effect.none
              .thenRun(_ => replyTo ! StatusReply.error(s"Player $playerId already place bet with amount $value"))
          }
          case None => {
            Effect
              .persist(PlaceBetEvent(playerId, amount))
              .thenRun(_ => replyTo ! StatusReply.ack())
          }
        }
      }
    }
  }
  private def dealCardsCommandHandler(state: DealCardsState, command: Command,
                                      ctx: ActorContext[Command], timer: TimerScheduler[Command]): Effect[Event, State] = {
    command match {
      case DealCards => {
        val deck = CardDeck.getShuffledDeck(8) // TODO:  wrap to F[] // extract to settings

        @tailrec
        def dealCards(activePlayers: List[PlayerId],
                      playerHands: Map[PlayerId, PlayerHandsData],
                      deck: CardDeck): Either[String, (Map[PlayerId, PlayerHandsData], CardDeck)] = {
          activePlayers match {
            case playerId :: remainingActivePlayers => {
              deck.takeFirstCards(2).toRight(s"Not enough cards in deck to deal player $playerId").flatMap {
                result => {
                  val playerHandData = for {
                    amount <- state.playerBets.get(playerId).toRight(s"Failed to find bet amount for player $playerId")
                    handId = HandId(UUID.randomUUID().toString)
                    handData = HandData(handId, amount, result._1)
                    playerHandsData = PlayerHandsData(playerId, Map(handId -> handData))
                  } yield playerHandsData

                  playerHandData.flatMap {
                    data => dealCards(remainingActivePlayers, playerHands + (data.playerId), result._2) // TODO: fix tail recursion issue
                  }
                }
              }
            }
            case Nil => Right((playerHands, deck))
          }
        }

        dealCards(state.playerBets.keySet.toList, Map.empty, deck) match {
          case Left(msg) => Effect.persist(DealCardsFailEvent(msg))
          case Right((playerHandsData, remainingDeck)) => {
            remainingDeck.takeFirstCards(2) match {
              case Some((dealerCards, remainingDeck)) =>
                Effect.persist(DealCardsEvent(playerHandsData, dealerCards, remainingDeck))
              case None =>  Effect.persist(DealCardsFailEvent(s"Not enough cards in deck to deal dealer"))
            }
          }
        }
      }
    }
  }

  private def dealCardsCommandHandler(state: PlayersTurnState, command: Command,
                                      ctx: ActorContext[Command], timer: TimerScheduler[Command]): Effect[Event, State] = {
    command match {
      case Double(playerId, handId, replyTo) => {
        val handData = state.playerHandsData.get(playerId).toRight(s"Failed to find active player with id $playerId")
          .flatMap {
            playerHandsData => playerHandsData.hands.get(handId).toRight(s"Failed to find player $playerId hand $handId")
              .map {
                handData => handData.copy(betAmount = handData.betAmount * 2) // TODO: store players turn activities in hand
              }
          }
        handData match {
          case Left(msg) => Effect.none
            .thenRun(_ => replyTo ! StatusReply.error(msg))
          case Right(handData) => {
            state.playerHandsData + (playerId, handData)
          }
        }
      }
    }
  }

  private def eventHandler(state: State, event: Event, ctx: ActorContext[Command], timer: TimerScheduler[Command]): State = {
    state match {
      case AwaitingPlayersState => awaitingPlayersEventHandler(event, ctx, timer)
      case placeBetState: PlaceBetsState => placingBetsEventHandler(placeBetState, event, ctx, timer)
      case dealCardsState: DealCardsState => dealingCardsEventHandler(dealCardsState, event, ctx, timer)
      case playersTurnState: PlayersTurnState => playersTurnEventHandler(playersTurnState, event, ctx, timer)
    }
  }

  private def awaitingPlayersEventHandler(event: Event, ctx: ActorContext[Command], timer: TimerScheduler[Command]): State = {
    event match {
      case JoinGameEvent(playerId) => PlaceBetsState(List(playerId), List.empty, Map.empty)
    }
  }

  private def placingBetsEventHandler(state: PlaceBetsState, event: Event, ctx: ActorContext[Command], timer: TimerScheduler[Command]): State = {
    event match {
      case JoinGameEvent(playerId) => state.copy(inactivePlayers = playerId :: state.inactivePlayers)
      case PlaceBetEvent(playerId, amount) => {
        val newPlayersBets = state.playerBets + (playerId -> amount)
        if (newPlayersBets.keySet.subsetOf(state.activePlayers.toSet)) {
          ctx.self ! DealCards
          DealCardsState(state.inactivePlayers, newPlayersBets)
        } else {
          state.copy(playerBets = newPlayersBets)
        }
      }
    }
  }

  private def dealingCardsEventHandler(state: DealCardsState, event: Event, ctx: ActorContext[Command], timer: TimerScheduler[Command]): State = {
    event match {
      case JoinGameEvent(playerId) => state.copy(inactivePlayers = playerId :: state.inactivePlayers)
      case DealCardsEvent(playerHandsData, dealerCards, cardDeck) => {
        PlayersTurnState(state.inactivePlayers, playerHandsData, dealerCards, cardDeck)
      }
      case DealCardsFailEvent(msg) => {
        val players = state.inactivePlayers.toSet ++ state.playerBets.keySet
        ErrorState(players.toList, msg)
      }
    }
  }

  private def playersTurnEventHandler(state: PlayersTurnState, event: Event, ctx: ActorContext[Command], timer: TimerScheduler[Command]): State = {
    event match {
      case DoubleEvent(playerId, handId) =>
    }
  }


  //      case DoubleEvent(playerId) => {
  //
  //      }
  //      case HitEvent(playerId) =>
  //      case SurrenderEvent(playerId) =>
  //      case StayEvent(playerId) =>
  //      case EvenMoneyEvent(playerId) =>
}
