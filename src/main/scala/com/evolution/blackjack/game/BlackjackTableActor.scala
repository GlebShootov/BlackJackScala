package com.evolution.blackjack.game

import akka.Done
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior, SupervisorStrategy}
import akka.pattern.StatusReply
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, RetentionCriteria}
import com.evolution.blackjack.domain.data.{Card, CardDeck, PlayerId}
import com.evolution.blackjack.game.BlackjackTableActor.findNextTurn
import com.evolution.blackjack.game.data.{HandData, HandId, HandScoreData, PlayerHandsData, PlayerTurnData, PlayersTurnOption}

import java.util.UUID
import scala.annotation.tailrec
import scala.collection.immutable.{AbstractSet, ListMap, SortedMap, SortedSet}
import scala.concurrent.duration.{DurationInt, FiniteDuration}

object BlackjackTableActor {

  private val placeBetTimeoutDuration: FiniteDuration = 30.seconds // TODO: settings
  private val startGameDelayDuration: FiniteDuration = 10.seconds // TODO: settings

  type Seats = SortedMap[Int, Option[PlayerId]] // TODO: extract to somewhere

  sealed trait State { // TODO extract states, commands and events to separate classes
    def seats: Seats
  }

  final object AwaitingPlayersState extends State {
    override def seats: Seats = SortedMap()
  }

  final case class PlaceBetsState(seats: Seats,
                                  playersBets: Map[PlayerId, BigDecimal]
                                 ) extends State

  final case class DealCardsState(seats: Seats,
                                  playersBets: Map[PlayerId, BigDecimal]
                                 ) extends State

  final case class PlayersTurnState(seats: Seats,
                                    currentPlayerTurnData: PlayerTurnData,
                                    playersHandsDataMap: Map[PlayerId, PlayerHandsData],
                                    dealerHand: List[Card],
                                    cardDeck: CardDeck
                                   ) extends State

  final case class DealerTurnState(seats: Seats,
                                   playerHandsData: Map[PlayerId, PlayerHandsData],
                                   dealerHand: List[Card],
                                   cardDeck: CardDeck
                                   ) extends State

  final case class GameEndedState(seats: Seats) extends State

  final case class ErrorState(seats: Seats,
                              reason: String) extends State

  sealed trait Command
  final object StartGame extends Command
  final case class JoinGame(playerId: PlayerId, seatId: Int, replyTo: ActorRef[StatusReply[Done]]) extends Command
  private object DealCards extends Command

  final case class PlaceBet(playerId: PlayerId, betAmount: BigDecimal, replyTo: ActorRef[StatusReply[Done]]) extends Command
  final object PlaceBetTimeout extends Command
  final object GameEnded extends Command

  final case class Double(playerId: PlayerId, handId: HandId, replyTo: ActorRef[StatusReply[Done]]) extends Command
  //final case class Insurance(playerId: PlayerId, handId: HandId, replyTo: ActorRef[StatusReply[Done]]) extends Command
  final case class Hit(playerId: PlayerId, handId: HandId, replyTo: ActorRef[StatusReply[Done]]) extends Command
//  final case class Surrender(playerId: PlayerId, handId: HandId, replyTo: ActorRef[StatusReply[Done]]) extends Command
  final case class Stay(playerId: PlayerId, handId: HandId, replyTo: ActorRef[StatusReply[Done]]) extends Command
//  final case class EvenMoney(playerId: PlayerId, handId: HandId, replyTo: ActorRef[StatusReply[Done]]) extends Command

  final object CheckDealersCard extends Command
  final object DealerHit extends Command
  final object DetermineWinners extends Command

  sealed trait Event
  final object StartGameEvent extends Event
  private final case class JoinGameEvent(playerId: PlayerId, seatId: Int) extends Event
  private final case class PlaceBetEvent(playerId: PlayerId, betAmount: BigDecimal) extends Event
  private final object PlaceBetTimeoutEvent extends Event
  private final case class DealCardsEvent(playerHandsData: Map[PlayerId, PlayerHandsData], dealerCards: List[Card],
                                          cardDeck: CardDeck, nextPlayerTurn: PlayerTurnData) extends Event
  private final case class DealCardsFailEvent(reason: String) extends Event
  private final case class DoubleEvent(playerId: PlayerId, handId: HandId, updatedHandsData: Map[PlayerId, PlayerHandsData], remainingDeck: CardDeck) extends Event
  //private final case class Insurance(playerId: PlayerId, handId: HandId) extends Event
  private final case class HitEvent(playerId: PlayerId, handId: HandId, hitCard: Card, remainingDeck: CardDeck) extends Event
//  private final case class SurrenderEvent(playerId: PlayerId, handId: HandId) extends Event
  private final case class StayEvent(playerId: PlayerId, handId: HandId, playersHandsData: Map[PlayerId, PlayerHandsData]) extends Event
//  private final case class EvenMoneyEvent(playerId: PlayerId, handId: HandId) extends Event
  private final object CheckDealersCardEvent extends Event
  final object DealerHitEvent extends Event
  final object DetermineWinnersEvent extends Event

  def apply(tableId: String): Behavior[Command] = {
    // load context (balance of pId) and others

    // JoinGame(pId)

    val seatsTuples = Range.inclusive(1, 5) // TODO: settings
      .map(seatNum => (seatNum, Option.empty[PlayerId]))

    val seats = SortedMap[Int, Option[PlayerId]]() ++ seatsTuples

    Behaviors.withTimers[Command] { timer =>
      Behaviors.setup[Command] { ctx =>
        EventSourcedBehavior[Command, Event, State](
          PersistenceId("BlackjackGame", tableId),
          AwaitingPlayersState,
          (state, command) => commandHandler(state, command, ctx, timer), // TODO extract to package command_handlers with interface of command handlers and implementations
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
      case dealerTurnState: DealerTurnState => dealerTurnCommandHandler(dealerTurnState, command, ctx, timer)
      case gameEndedState: GameEndedState => gameEndedCommandHandler(gameEndedState, command, ctx, timer)
    }
  }

  private def awaitingPlayersCommandHandler(state: State, command: Command,
                                            ctx: ActorContext[Command], timer: TimerScheduler[Command]): Effect[Event, State] = {
    command match {
      case JoinGame(playerId, seatId, replyTo) => {
        ctx.log.debug(s"Player $playerId trying to join the game while awaiting players")
        if (state.seats.get(seatId).flatten.isDefined) {
          Effect.none
            .thenRun(_ => replyTo ! StatusReply.error("Seat is already reserved"))
        } else {
          Effect.persist(JoinGameEvent(playerId, seatId))
            .thenRun(_ => {
              replyTo ! StatusReply.Ack
              if (!timer.isTimerActive(StartGame)) {
                timer.startSingleTimer(StartGame, startGameDelayDuration)
              }
            })
        }
      }
      case StartGame => {
        if (state.seats.values.flatten.isEmpty) {
          Effect.none
        } else {
          Effect.persist(StartGameEvent)
        }
      }
    }
  }

  private def placingBetsCommandHandler(state: PlaceBetsState, command: Command,
                                        ctx: ActorContext[Command], timer: TimerScheduler[Command]): Effect[Event, State] = {
    command match {
      case JoinGame(playerId, seatId, replyTo) => {
        ctx.log.debug(s"Player $playerId trying to join the game while placing bets")
        if (state.seats.get(seatId).flatten.isDefined) {
          Effect.none
            .thenRun(_ => replyTo ! StatusReply.error("Seat is already reserved"))
        } else {
          Effect.persist(JoinGameEvent(playerId, seatId))
            .thenRun(_ => replyTo ! StatusReply.Ack)
        }
      }
      case PlaceBet(playerId, amount, replyTo) => {
        ctx.log.debug(s"Player with id $playerId attempts to place bet with amount $amount.")
        state.playersBets.get(playerId) match {
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
      case PlaceBetTimeout => {
        ctx.log.debug(s"Placing bets time is over")
        Effect.persist(PlaceBetTimeoutEvent)
      }
    }
  }
  private def dealCardsCommandHandler(state: DealCardsState, command: Command,
                                      ctx: ActorContext[Command], timer: TimerScheduler[Command]): Effect[Event, State] = {
    command match {
      case JoinGame(playerId, seatId, replyTo) => {
        ctx.log.debug(s"Player $playerId trying to join the game while dealing cards")
        if (state.seats.get(seatId).flatten.isDefined) {
          Effect.none
            .thenRun(_ => replyTo ! StatusReply.error("Seat is already reserved"))
        } else {
          Effect.persist(JoinGameEvent(playerId, seatId))
            .thenRun(_ => replyTo ! StatusReply.Ack)
        }
      }
      case DealCards => {
        ctx.log.debug("Starting to deal cards")
        val deck = CardDeck.getShuffledDeck(8) // TODO:  wrap to F[] // extract to settings

        @tailrec
        def dealCards(activePlayers: List[PlayerId],
                      playerHands: Map[PlayerId, PlayerHandsData],
                      deck: CardDeck): Either[String, (Map[PlayerId, PlayerHandsData], CardDeck)] = {
          activePlayers match {
            case Nil => Right((playerHands, deck))
            case playerId :: remainingActivePlayers => {
              deck.takeFirstCards(2) match {
                case None => Left(s"Not enough cards in deck to deal player $playerId")
                case Some(result) => {
                  val playerHandsData = state.playersBets.get(playerId).toRight(s"Failed to find bet amount for player $playerId")
                    .map { amount =>
                      val handId = HandId(UUID.randomUUID().toString)
                      val handData = HandData(handId, amount, result._1)
                      PlayerHandsData(playerId, ListMap(handId -> handData))
                    }
                  playerHandsData match {
                    case Left(msg) => Left(msg)
                    case Right(data) =>
                      dealCards(remainingActivePlayers, playerHands + (data.playerId -> data), result._2)
                  }
                }
              }
            }
          }
        }

        val activePlayers = state.seats.values.flatten.toList

        val result = for {
          result <- dealCards(activePlayers, Map.empty, deck)
          firstPlayerTurn <- findFirstPlayerTurn(result._1, state.seats).toRight("Failed to find first player turn")
          result2 <- result._2.takeFirstCards(2).toRight(s"Not enough cards in deck to deal dealer")
        } yield DealCardsEvent(result._1, result2._1, result2._2, firstPlayerTurn)

        result match {
          case Left(msg) => Effect.persist(DealCardsFailEvent(msg))
          case Right(event) => Effect.persist(event)
        }
      }
    }
  }

  private def playersTurnCommandHandler(state: PlayersTurnState, command: Command,
                                        ctx: ActorContext[Command], timer: TimerScheduler[Command]): Effect[Event, State] = {
    command match {
      case JoinGame(playerId, seatId, replyTo) => {
        ctx.log.debug(s"Player $playerId trying to join the game while are player turns")
        if (state.seats.get(seatId).flatten.isDefined) {
          Effect.none
            .thenRun(_ => replyTo ! StatusReply.error("Seat is already reserved"))
        } else {
          Effect.persist(JoinGameEvent(playerId, seatId))
            .thenRun(_ => replyTo ! StatusReply.Ack)
        }
      }
      case Double(playerId, handId, replyTo) => {
        ctx.log.debug(s"Player $playerId tries to double.")
        if (PlayerTurnData(playerId, handId) == state.currentPlayerTurnData) {
          val updatedPlayerHandsDataAndCardDeck = for {
            playerHandsData <- state.playersHandsDataMap.get(playerId).toRight(s"Failed to find active player with id $playerId")
            playerHand <- playerHandsData.hands.get(handId).toRight(s"Failed to find player $playerId hand $handId")
            cardResult <- state.cardDeck.takeFirstCards(1).toRight(s"Failed to find next card for double")
            updatedHand = playerHand.copy(betAmount = playerHand.betAmount * 2, cards = playerHand.cards ::: cardResult._1)
            updatedPlayerHandsData = playerHandsData.copy(hands = playerHandsData.hands + (playerId -> updatedHand))
          } yield (updatedPlayerHandsData, cardResult._2)

          updatedPlayerHandsDataAndCardDeck match {
            case Left(msg) => Effect
              .none
              .thenRun(_ => replyTo ! StatusReply.error(msg))
            case Right((playerHands, remainingDeck)) => {
              val updatedPlayersHands = state.playersHandsDataMap + (playerId -> playerHands)
              Effect.persist(DoubleEvent(playerId, handId, updatedPlayersHands, remainingDeck))
            }
          }
        } else if (playerId != state.currentPlayerTurnData.playerId) {
          Effect.none
            .thenRun(_ => replyTo ! StatusReply.error("Rejected due not your turn yet"))
        } else {
          Effect.none
            .thenRun(_ => replyTo ! StatusReply.error("Rejected due not right hand turn"))
        }
      }
      case Stay(playerId, handId, replyTo) => {
        val updatedPlayerHandsData = for {
          playerHandsData <- state.playersHandsDataMap.get(playerId).toRight(s"Failed to find player $playerId hand $handId")
          playerHand <- playerHandsData.hands.get(handId).toRight(s"Failed to find player $playerId hand $handId")
          updatedPlayerHand = playerHand.copy(playerTurnHistory = playerHand.playerTurnHistory :+ PlayersTurnOption.Stay)
          updatedPlayerHandsData = playerHandsData.copy(hands = playerHandsData.hands + (handId -> updatedPlayerHand))
        } yield updatedPlayerHandsData

        updatedPlayerHandsData match {
          case Left(msg) => Effect.none
            .thenRun(_ => replyTo ! StatusReply.error(msg))
          case Right(data) => {
            val updatedPlayersHand = state.playersHandsDataMap + (playerId -> data)
            Effect.persist(StayEvent(playerId, handId, updatedPlayersHand))
              .thenRun(_ => replyTo ! StatusReply.ack())
          }
        }
      }
      case Hit(playerId, handId, replyTo) => {
        findPlayerHand(playerId, handId, state.playersHandsDataMap) match {
          case Some(data) => {
            state.cardDeck.takeFirstCards(1) match {
              case Some((card :: Nil, remainingDeck)) => {
                if (data.playerTurnHistory.isEmpty || data.playerTurnHistory.forall(_ == PlayersTurnOption.Stay)) {
                  Effect.persist(HitEvent(playerId, handId, card, remainingDeck))
                    .thenRun(_ => replyTo ! StatusReply.ack())
                } else {
                  Effect.none.thenRun(_ => replyTo ! StatusReply.error("Not allowed to hit"))
                }
              }
              case None => {
                Effect.none.thenRun(_ => replyTo ! StatusReply.error("Failed to get hit card from deck"))
              }
            }
          }
          case None => {
            Effect.none
              .thenRun(_ => replyTo ! StatusReply.error(s"Failed to find player $playerId hand $handId"))
          }
        }
      }
    }
  }

  private def dealerTurnCommandHandler(state: DealerTurnState, command: Command,
                                        ctx: ActorContext[Command], timer: TimerScheduler[Command]): Effect[Event, State] = {
    case CheckDealersCard => Effect.persist(CheckDealersCardEvent)
    case DealerHit => Effect.persist(DealerHitEvent)
    case DetermineWinners => Effect.persist(DetermineWinnersEvent)
  }

  private def gameEndedCommandHandler(state: GameEndedState, command: Command,
                                        ctx: ActorContext[Command], timer: TimerScheduler[Command]): Effect[Event, State] = {
    command match {
      case JoinGame(playerId, seatId, replyTo) => {
        ctx.log.debug(s"Player $playerId trying to join the game while game ended")
        if (state.seats.get(seatId).flatten.isDefined) {
          Effect.none
            .thenRun(_ => replyTo ! StatusReply.error("Seat is already reserved"))
        } else {
          Effect.persist(JoinGameEvent(playerId, seatId))
            .thenRun(_ => replyTo ! StatusReply.Ack)
        }
      }
      case StartGame => {
        Effect.persist(StartGameEvent)
      }
    }
  }
















  private def eventHandler(state: State, event: Event, ctx: ActorContext[Command], timer: TimerScheduler[Command]): State = {
    state match {
      case AwaitingPlayersState => awaitingPlayersEventHandler(event, ctx, timer)
      case placeBetState: PlaceBetsState => placingBetsEventHandler(placeBetState, event, ctx, timer)
      case dealCardsState: DealCardsState => dealingCardsEventHandler(dealCardsState, event, ctx, timer)
      case playersTurnState: PlayersTurnState => playersTurnEventHandler(playersTurnState, event, ctx, timer)
      case dealerTurnState: DealerTurnState => dealerTurnEventHandler(dealerTurnState, event, ctx, timer)
      case gameEndedState: GameEndedState => gameEndedEventHandler(gameEndedState, event, ctx, timer)
    }
  }

  private def awaitingPlayersEventHandler(event: Event, ctx: ActorContext[Command], timer: TimerScheduler[Command]): State = {
    event match {
      case JoinGameEvent(playerId, seatId) => {
        val updatedSeats: Seats = AwaitingPlayersState.seats + (playerId -> Some(seatId))
        timer.startSingleTimer(PlaceBetTimeout, placeBetTimeoutDuration)
        PlaceBetsState(updatedSeats, Map.empty)
      }
    }
  }

  private def placingBetsEventHandler(state: PlaceBetsState, event: Event, ctx: ActorContext[Command], timer: TimerScheduler[Command]): State = {
    event match {
      case JoinGameEvent(playerId, seatId) => {
        state.copy(seats = state.seats + (playerId -> Some(seatId)))
      }
      case PlaceBetEvent(playerId, amount) => {
        val newPlayersBets = state.playersBets + (playerId -> amount)
        if (newPlayersBets.keySet.subsetOf(state.seats.values.flatten.toSet)) {
          timer.cancel(PlaceBetTimeout)
          ctx.self ! DealCards
          DealCardsState(state.seats, newPlayersBets)
        } else {
          state.copy(playersBets = newPlayersBets)
        }
      }
      case PlaceBetTimeoutEvent => {
        if (state.playersBets.isEmpty) {
          GameEndedState(state.seats)
        } else {
          DealCardsState(state.seats, state.playersBets)
        }
      }
    }
  }

  private def dealingCardsEventHandler(state: DealCardsState, event: Event, ctx: ActorContext[Command], timer: TimerScheduler[Command]): State = {
    event match {
      case JoinGameEvent(playerId, seatId) => {
        state.copy(seats = state.seats + (playerId -> Some(seatId)))
      }
      case DealCardsEvent(playerHandsData, dealerCards, cardDeck, nextPlayerTurn) => {
        findFirstPlayerTurn(playerHandsData, state.seats)
        PlayersTurnState(state.seats, nextPlayerTurn, playerHandsData, dealerCards, cardDeck)
      }
      case DealCardsFailEvent(msg) => {
        ErrorState(state.seats, msg)
      }
    }
  }

  private def playersTurnEventHandler(state: PlayersTurnState, event: Event, ctx: ActorContext[Command], timer: TimerScheduler[Command]): State = {
    event match {
      case DoubleEvent(playerId, handId, playersHandsData, remainingDeck) => {
        val nextPlayerTurnData = findNextTurn(playerId, handId, state.playersHandsDataMap, state.seats)

        val playerHandsAndPlayerHand = for {
          playerHandsData <- playersHandsData.get(playerId)
          playerHand <- playerHandsData.hands.get(handId)
        } yield (playerHandsData, playerHand)

        playerHandsAndPlayerHand match {
          case Some(data) => {
            val score = HandScoreCalculator.countScore(data._2.cards)
            val updatedPlayerHandsData = if (score <= 21) {
              data._1
            } else {
              data._1.copy(hands = data._1.hands - handId)
            }

            val updatedPlayersHandsData = playersHandsData + (playerId -> updatedPlayerHandsData)

            nextPlayerTurnData match {
              case Some(nextPlayerTurnData) => {
                state.copy(playersHandsDataMap = updatedPlayersHandsData, currentPlayerTurnData = nextPlayerTurnData, cardDeck = remainingDeck)
              }
              case None => {
                ctx.self ! CheckDealersCard
                DealerTurnState(state.seats, updatedPlayersHandsData, state.dealerHand, remainingDeck)
              }
            }
          }
          case None => ErrorState(state.seats, s"Failed to find player $playerId hand $handId")
        }
      }
      case StayEvent(playerId, handId, playersHandsData) => {
        findNextTurn(playerId, handId, playersHandsData, state.seats) match {
          case Some(playerTurnData) => {
            state.copy(playersHandsDataMap = playersHandsData, currentPlayerTurnData = playerTurnData)
          }
          case None => {
            ctx.self ! CheckDealersCard
            DealerTurnState(state.seats, playersHandsData, state.dealerHand, state.cardDeck)
          }
        }
      }
      case HitEvent(playerId, handId, hitCard, remainingDeck) => {
        val result = for {
          playerHands <- state.playersHandsDataMap.get(playerId)
          playerHand <- playerHands.hands.get(handId)
        } yield (playerHands, playerHand)

        result match {
          case Some((playerHands, playerHand)) => {
            val updatedPlayerHand = playerHand.copy(cards = playerHand.cards :+ hitCard)
            val score = HandScoreCalculator.countScore(updatedPlayerHand.cards)
            if (score > 21) {
              val nextPlayerTurn = findNextTurn(playerId, handId, state.playersHandsDataMap, state.seats)
              val updatedPlayerHands = playerHands.copy(hands = playerHands.hands - handId)
              val updatedPlayersHandsMap = state.playersHandsDataMap + (playerId -> updatedPlayerHands)
              nextPlayerTurn match {
                case Some(nextPlayerTurnData) => state.copy(currentPlayerTurnData = nextPlayerTurnData, playersHandsDataMap = updatedPlayersHandsMap, cardDeck = remainingDeck)
                case None => {
                  ctx.self ! CheckDealersCard
                  DealerTurnState(state.seats, updatedPlayersHandsMap, state.dealerHand, remainingDeck)
                }
              }
            } else {
              val updatedHand = playerHand.copy(cards = playerHand.cards :+ hitCard)
              val updatedPlayerHands = playerHands.copy(hands = playerHands.hands + (handId -> updatedHand))
              state.copy(playersHandsDataMap = state.playersHandsDataMap + (playerId, updatedPlayerHands), cardDeck = remainingDeck)
            }
          }
        }
      }
    }
  }

  private def dealerTurnEventHandler(state: DealerTurnState, event: Event, ctx: ActorContext[Command], timer: TimerScheduler[Command]): State = {
    event match {
      case CheckDealersCardEvent => {
        val score = HandScoreCalculator.countScore(state.dealerHand)
        if (score < 17) {
          ctx.self ! DealerHit
        } else {
          ctx.self ! DetermineWinners
        }
        state
      }
      case DealerHitEvent => {
        state.cardDeck.takeFirstCards(1) match {
          case Some((card, remainingDeck)) => {
            val updatedDealerCards = state.dealerHand ::: card
            val score = HandScoreCalculator.countScore(updatedDealerCards)
            if (score < 17) {
              ctx.self ! DealerHit
            } else {
              ctx.self ! DetermineWinners
            }
            state.copy(dealerHand = updatedDealerCards, cardDeck = remainingDeck)
          }
          case None => ErrorState(state.seats, "Failed to get next card from deck on dealer hit")
        }
      }
      case DetermineWinnersEvent => {
        val dealerScore = HandScoreCalculator.countScore(state.dealerHand)
        val dealerHandScoreData = HandScoreData(dealerScore, state.dealerHand)

        val playersHandScoreData = state.playerHandsData.toList.flatMap { data =>
          data._2.hands.map { handData =>
            val score = HandScoreCalculator.countScore(handData._2.cards)
            (data._1, HandScoreData(score, handData._2.cards))
          }
        }

        val winners = playersHandScoreData.filter(_._2.compare(dealerHandScoreData) > 0)
        val drawPlayers = playersHandScoreData.filter(_._2.compare(dealerHandScoreData) == 0)

        ctx.log.debug(s"Winners: $winners")
        ctx.log.debug(s"Draw players: $drawPlayers")

        timer.startSingleTimer(StartGame, startGameDelayDuration)
        GameEndedState(seats = state.seats)
      }
    }
  }

  private def gameEndedEventHandler(state: GameEndedState, event: Event, ctx: ActorContext[Command], timer: TimerScheduler[Command]): State = {
    event match {
      case StartGameEvent => {
        if (state.seats.values.flatten.isEmpty) {
          AwaitingPlayersState
        } else {
          PlaceBetsState(state.seats, Map.empty)
        }
      }
    }
  }








    // helpers
  def findFirstPlayerTurn(handsData: Map[PlayerId, PlayerHandsData], seats: Seats): Option[PlayerTurnData] = {
    val playersTurnsOrders = seats.values.flatten.toList


    def findFirstPlayerTurn(handsData: Map[PlayerId, PlayerHandsData], players: List[PlayerId]): Option[PlayerTurnData] = {
      players match {
        case pId :: _ => {
          handsData.get(pId) match {
            case Some(playerHandsData) => {
              playerHandsData.hands.keySet.toList match {
                case h :: _ => Some(PlayerTurnData(pId, h))
                case _ => None
              }
            }
            case None => None
          }
        }
        case _ => None
      }
    }

      findFirstPlayerTurn(handsData, playersTurnsOrders)
  }

  def findPlayerHand(playerId: PlayerId, handId: HandId, playersHandsData: Map[PlayerId, PlayerHandsData]): Option[HandData] = {
    playersHandsData.get(playerId)
      .flatMap { playerHandsData =>
        playerHandsData.hands.get(handId)
      }
  }

  def findNextTurn(playerId: PlayerId, currentHandIdTurn: HandId, handsData: Map[PlayerId, PlayerHandsData], seats: Seats): Option[PlayerTurnData] = {
    // TODO: wrong due to map

    val playersTurnsOrders = seats.values.flatten.toList.flatMap { data =>
      handsData.get(playerId) match {
        case Some(playerHandsData) => {
          playerHandsData.hands.map { p =>
            PlayerTurnData(playerId, p._1)
          }
        }
        case None => List.empty
      }
    }

    @tailrec
    def findNext(playersAndHandsIds: List[PlayerTurnData]): Option[PlayerTurnData] = {
      playersAndHandsIds match {
        case _ :: Nil => None
        case head :: tail => {
          if (head.playerId != playerId) findNext(tail)
          else if (head.handId != currentHandIdTurn) findNext(tail)
          else Some(tail.head)
        }
        case _ => None
      }
    }

    findNext(playersTurnsOrders)
  }
}
