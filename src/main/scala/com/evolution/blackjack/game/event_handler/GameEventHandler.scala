package com.evolution.blackjack.game.event_handler

import akka.actor.typed.scaladsl.{ActorContext, TimerScheduler}
import com.evolution.blackjack.game.blackjack_actor_state.{BlackjackActorState => S}
import com.evolution.blackjack.game.command_handler.{BlackjackCommand => C}
import com.evolution.blackjack.game.event_handler.{BlackjackEvent => E}

class GameEventHandler(ctx: ActorContext[C], timer: TimerScheduler[C]) {

  val startGameEventHandler: StartGameEventHandler = new StartGameEventHandler(ctx, timer)
  val joinGameEventHandler: JoinGameEventHandler = new JoinGameEventHandler(ctx, timer)
  val quitGameEventHandler: QuitGameEventHandler = new QuitGameEventHandler(ctx, timer)
  val placeBetEventHandler: PlaceBetEventHandler = new PlaceBetEventHandler(ctx, timer)
  val placeBetTimeoutEventHandler: PlaceBetTimeoutEventHandler = new PlaceBetTimeoutEventHandler(ctx, timer)
  val checkBetsEventHandler: CheckBetsEventHandler = new CheckBetsEventHandler(ctx, timer)
  val dealCardsEventHandler: DealCardsEventHandler = new DealCardsEventHandler(ctx, timer)
  val doubleEventHandler: DoubleEventHandler = new DoubleEventHandler(ctx, timer)
  val hitEventHandler: HitEventHandler = new HitEventHandler(ctx, timer)
  val stayEventHandler: StayEventHandler = new StayEventHandler(ctx, timer)
  val checkDealersCardsEventHandler: CheckDealersCardsEventHandler = new CheckDealersCardsEventHandler(ctx, timer)
  val dealerHitEventHandler: DealerHitEventHandler = new DealerHitEventHandler(ctx, timer)
  val determineWinnersEventHandler: DetermineWinnersEventHandler = new DetermineWinnersEventHandler(ctx, timer)

  def apply(state: S, event: E): S = {
    event match {
      case e : E.StartGameEvent.type => startGameEventHandler.handle(e, state)
      case e : E.JoinGameEvent => joinGameEventHandler.handle(e, state)
      case e : E.QuitGameEvent => quitGameEventHandler.handle(e, state)
      case e : E.PlaceBetEvent => placeBetEventHandler.handle(e, state)
      case e : E.PlaceBetTimeoutEvent.type => placeBetTimeoutEventHandler.handle(e, state)
      case e : E.CheckBetsEvent => checkBetsEventHandler.handle(e, state)
      case e : E.DealCardsEvent.type => dealCardsEventHandler.handle(e, state)
      case e : E.DoubleEvent => doubleEventHandler.handle(e, state)
      case e : E.HitEvent => hitEventHandler.handle(e, state)
      case e : E.StayEvent => stayEventHandler.handle(e, state)
      case e : E.CheckDealersCardsEvent.type => checkDealersCardsEventHandler.handle(e, state)
      case e : E.DealerHitEvent.type => dealerHitEventHandler.handle(e, state)
      case e : E.DetermineWinnersEvent.type => determineWinnersEventHandler.handle(e, state)
    }
  }
}
