package com.evolution.blackjack.game.command_handler

import akka.actor.typed.javadsl.AskPattern
import akka.actor.typed.scaladsl.{ActorContext, TimerScheduler}
import akka.pattern.StatusReply
import akka.persistence.typed.scaladsl.Effect
import cats.effect.Sync
import com.evolution.blackjack.game.BalanceService
import com.evolution.blackjack.game.command_handler.{BlackjackCommand => C}
import com.evolution.blackjack.game.blackjack_actor_state.{BlackjackActorState => S}
import com.evolution.blackjack.game.event_handler.{BlackjackEvent => E}
import com.evolutiongaming.catshelper.ToFuture

import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success, Try}

class GameCommandHandler[F[_]: Sync : ToFuture] {

  val balanceService: BalanceService[F] = ???
  val joinGameCommandHandler: JoinGameCommandHandler[F] = new JoinGameCommandHandler(balanceService)
  val startGameCommandHandler: StartGameCommandHandler[F] = new StartGameCommandHandler()
  val checkBetsCommandHandler: CheckBetsCommandHandler[F] = new CheckBetsCommandHandler(balanceService)
  val doubleCommandHandler: DoubleCommandHandler[F] = new DoubleCommandHandler[F](balanceService)

  def apply(state: S, command: C, ctx: ActorContext[C], timer: TimerScheduler[C]): Effect[E, S] = {
    command match {
      case cmd : C.StartGame => {
        cmd.context match {
          case None => {
            ctx.pipeToSelf(
              ToFuture[F].apply(startGameCommandHandler.loadContext(cmd, state))
            ) {
              case Failure(e) => cmd.copy(context = Some(Left(e.toString)))
              case Success(v) => cmd.copy(context = Some(Right(v)))
            }
            Effect.none
          }
          case Some(v) => {
            startGameCommandHandler.validateCommand(cmd, v, state) match {
              case Left(msg) => Effect.none.thenRun(_ => startGameCommandHandler.handleError(cmd, msg))
              case Right(_) => {
                val events = startGameCommandHandler.produceEvent(cmd, ())
                Effect.persist(events)
                  .thenRun(s => startGameCommandHandler.runEffect(cmd, (), s))
              }
            }
          }
        }
      }
      case cmd: C.JoinGame => {
        cmd.context match {
          case None => {
            ctx.pipeToSelf {
              ToFuture[F].apply(joinGameCommandHandler.loadContext(cmd, state))
            } {
              case Success(v) => cmd.copy(context = Some(Right(v)))
              case Failure(e) => cmd.copy(context = Some(Left(e.toString)))
            }
            Effect.none
          }
          case Some(v) => {
            v match {
              case Left(msg) => {
                Effect.none.thenRun(_ => joinGameCommandHandler.handleError(cmd, msg))
              }
              case Right(balance) => {
                val events = joinGameCommandHandler.produceEvent(cmd, balance)
                Effect.persist(events)
                  .thenRun(s => {
                    joinGameCommandHandler.runEffect(cmd, balance, s)
                    s match {
                      case S.AwaitingPlayersState => {
                        if (!timer.isTimerActive(C.StartGame)) {
                          timer.startSingleTimer(C.StartGame(None), 10.seconds)
                        }
                      }
                    }
                  })
              }
            }
          }
        }
      }
      case cmd : C.QuitGame => {
        QuitGameCommandHandler.validateCommand(cmd, (), state) match {
          case Left(msg) => Effect.none.thenRun(_ => QuitGameCommandHandler.handleError(cmd, msg))
          case Right(_) => {
            val events = QuitGameCommandHandler.produceEvent(cmd, ())
            Effect.persist(events)
              .thenRun(s => QuitGameCommandHandler.runEffect(cmd, (), s))
          }
        }
      }
      case cmd : C.DealCards => {
        DealCardsCommandHandler.validateCommand(cmd, (), state) match {
          case Left(msg) => Effect.none.thenRun(_ => DealCardsCommandHandler.handleError(cmd, msg))
          case Right(_) => {
            val events = DealCardsCommandHandler.produceEvent(cmd, ())
            Effect.persist(events)
              .thenRun(s => DealCardsCommandHandler.runEffect(cmd, (), s))
          }
        }
      }
      case cmd : C.PlaceBet => {
        PlaceBetCommandHandler.validateCommand(cmd, (), state) match {
          case Left(msg) => Effect.none.thenRun(_ => PlaceBetCommandHandler.handleError(cmd, msg))
          case Right(_) => {
            val events = PlaceBetCommandHandler.produceEvent(cmd, ())
            Effect.persist(events)
              .thenRun(s => PlaceBetCommandHandler.runEffect(cmd, (), s))
          }
        }
      }
      case cmd : C.PlaceBetTimeout => {
        PlaceBetTimeoutCommandHandler.validateCommand(cmd, (), state) match {
          case Left(msg) => Effect.none.thenRun(_ => PlaceBetTimeoutCommandHandler.handleError(cmd, msg))
          case Right(_) => {
            val events = PlaceBetTimeoutCommandHandler.produceEvent(cmd, ())
            Effect.persist(events)
              .thenRun(s => PlaceBetTimeoutCommandHandler.runEffect(cmd, (), s))
          }
        }
      }
      case cmd : C.CheckBets => {
        cmd.context match {
          case None => {
            ctx.pipeToSelf {
              ToFuture[F].apply(checkBetsCommandHandler.loadContext(cmd, state))
            } {
              case Success(v) => cmd.copy(context = Some(Right(v)))
              case Failure(e) => cmd.copy(context = Some(Left(e.toString)))
            }
            Effect.none
          }
          case Some(v) => {
            v match {
              case Left(msg) => {
                Effect.none.thenRun(s => checkBetsCommandHandler.handleError(cmd, msg))
              }
              case Right(balance) => {
                val events = checkBetsCommandHandler.produceEvent(cmd, balance)
                Effect.persist(events)
                  .thenRun(s => checkBetsCommandHandler.runEffect(cmd, balance, s))
              }
            }
          }
        }
      }
      case cmd : C.Double => {
        cmd.context match {
          case None => {
            ctx.pipeToSelf {
              ToFuture[F].apply(doubleCommandHandler.loadContext(cmd, state))
            } {
              case Success(v) => cmd.copy(context = Some(Right(v)))
              case Failure(e) => cmd.copy(context = Some(Left(e.toString)))
            }
            Effect.none
          }
          case Some(v) => {
            v match {
              case Left(msg) => {
                Effect.none.thenRun(s => doubleCommandHandler.handleError(cmd, msg))
              }
              case Right(balance) => {
                val events = doubleCommandHandler.produceEvent(cmd, balance)
                Effect.persist(events)
                  .thenRun(s => doubleCommandHandler.runEffect(cmd, balance, s))
              }
            }
          }
        }
      }
      case cmd : C.Hit => {
        HitCommandHandler.validateCommand(cmd, (), state) match {
          case Left(msg) => Effect.none.thenRun(_ => HitCommandHandler.handleError(cmd, msg))
          case Right(_) => {
            val events = HitCommandHandler.produceEvent(cmd, ())
            Effect.persist(events)
              .thenRun(s => HitCommandHandler.runEffect(cmd, (), s))
          }
        }
      }
      case cmd : C.Stay => {
        StayCommandHandler.validateCommand(cmd, (), state) match {
          case Left(msg) => Effect.none.thenRun(_ => StayCommandHandler.handleError(cmd, msg))
          case Right(_) => {
            val events = StayCommandHandler.produceEvent(cmd, ())
            Effect.persist(events)
              .thenRun(s => StayCommandHandler.runEffect(cmd, (), s))
          }
        }
      }
      case cmd : C.CheckDealersCard => {
        CheckDealersCardsCommandHandler.validateCommand(cmd, (), state) match {
          case Left(msg) => Effect.none.thenRun(_ => CheckDealersCardsCommandHandler.handleError(cmd, msg))
          case Right(_) => {
            val events = CheckDealersCardsCommandHandler.produceEvent(cmd, ())
            Effect.persist(events)
              .thenRun(s => CheckDealersCardsCommandHandler.runEffect(cmd, (), s))
          }
        }
      }
      case cmd : C.DealerHit => {
        DealerHitCommandHandler.validateCommand(cmd, (), state) match {
          case Left(msg) => Effect.none.thenRun(_ => DealerHitCommandHandler.handleError(cmd, msg))
          case Right(_) => {
            val events = DealerHitCommandHandler.produceEvent(cmd, ())
            Effect.persist(events)
              .thenRun(s => DealerHitCommandHandler.runEffect(cmd, (), s))
          }
        }
      }
      case cmd : C.DetermineWinners => {
        DetermineWinnersCommandHandler.validateCommand(cmd, (), state) match {
          case Left(msg) => Effect.none.thenRun(_ => DetermineWinnersCommandHandler.handleError(cmd, msg))
          case Right(_) => {
            val events = DetermineWinnersCommandHandler.produceEvent(cmd, ())
            Effect.persist(events)
              .thenRun(s => DetermineWinnersCommandHandler.runEffect(cmd, (), s))
          }
        }
      }
    }
  }
}
