package com.evolution.blackjack.domain.helper

import java.util.concurrent.atomic.AtomicInteger

object ActorHelper {

  val actorId: AtomicInteger = new AtomicInteger(0)

  def createUniqueActorName[T](actorClass: Class[T]): String = {
    s"${actorClass.toString}-${actorId.incrementAndGet()}"
  }
}
