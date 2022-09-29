package com.evolution.blackjack.game.command_handler

/**
 * A - context
 * C - command
 * E - event
 * R - error
 * S - state
 */

trait BlackjackCommandHandler[F[_], S, C, E, R] {
  type A

  def loadContext(command: C, state: S): F[A]

  def validateCommand(command: C, context: A, state: S): Either[R, Unit]

  def handleError(c: C, r: R): F[Unit]

  def produceEvent(command: C, context: A): List[E]

  def runEffect(command: C, context: A, updatedState: S): F[Unit]
}
