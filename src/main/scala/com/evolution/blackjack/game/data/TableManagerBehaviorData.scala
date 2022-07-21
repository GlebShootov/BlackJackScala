package com.evolution.blackjack.game.data

import akka.actor.ActorRef
import com.evolution.blackjack.game.BlackjackTableActor

// TODO: try to change actor refs on typed actors refs
final case class TableManagerBehaviorData(tableActorRefs: Map[String, ActorRef],
                                          tablesInfo: Map[String, TableInfo]) {

}
