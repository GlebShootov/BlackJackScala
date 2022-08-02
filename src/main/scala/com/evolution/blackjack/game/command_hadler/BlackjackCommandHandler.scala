package com.evolution.blackjack.game.command_hadler
// F as Id from cats.io

// A - context
// C - command
// E - event
// R - error
// S - state
trait BlackjackCommandHandler[F[_], S, A, C, E, R] {

  def loadContext(command: C): F[A]

  def validateCommand(command: C, context: A): Either[R, Unit]

  def produceEvent(command: C, context: A): List[E]

  //run effect
  def runEffect(command: C, context: A, updatedState: S): F[Unit]
}
