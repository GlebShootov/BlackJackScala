package com.evolution.blackjack.game

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.evolution.blackjack.domain.helper.ActorHelper
import com.evolution.blackjack.game.TableManagerActor.{JoinGameReq, TablesInfoReq, TablesInfoResp}
import com.evolution.blackjack.game.data.{TableInfo, TableManagerBehaviorData}

class TableManagerActor extends Actor with ActorLogging { // todo rename on engine or gameEngine

  override def receive: Receive = workingBehavior(createInitialBehaviorData())

  def workingBehavior(behaviorData: TableManagerBehaviorData): Receive = {
    case req: TablesInfoReq => handleTableInfoReq(behaviorData)
    case req: JoinGameReq => handleJoinGameReq(req, behaviorData)
  }

  def createInitialBehaviorData(): TableManagerBehaviorData = {
    val tableId = ActorHelper.createUniqueActorName(BlackjackTableActor.getClass)
    val tableActorRef = context.actorOf(Props[BlackjackTableActor], tableId)

    val tableInfo: TableInfo = TableInfo(tableId, 0, 5, 10) // TODO: extract to settings
    val tableActorRefs: Map[String, ActorRef] = Map(tableId -> tableActorRef)
    val tablesInfo: Map[String, TableInfo] = Map(tableId -> tableInfo)

    TableManagerBehaviorData(tableActorRefs, tablesInfo)
  }

  def handleTableInfoReq(behaviorData: TableManagerBehaviorData): Unit = {
    val tablesInfo = behaviorData.tablesInfo.values.toList
    sender() ! TablesInfoResp(tablesInfo)
  }

  def handleJoinGameReq(req: JoinGameReq, behaviorData: TableManagerBehaviorData): Unit = {
    ??? // TODO:
//    val tableId = req.tableId
//    behaviorData.tablesInfo.

//    val res = for {
//      tableInfo <- behaviorData.tablesInfo.get(tableId).toRight(s"Table info with id $tableId not found")
//      tableActorRef <- behaviorData.tableActorRefs.get(tableId).toRight(s"Table with id $tableId not found")
//      _ <- Either.cond(tableInfo.currentPlayersQuantity >= tableInfo.maxPlayersQuantity, (),
//        s"Table with id $tableId contains max players quantity")
//
//    }
  }
}

object TableManagerActor {

  final case class TablesInfoReq()
  final case class TablesInfoResp(tablesInfo: List[TableInfo])

  final case class JoinGameReq(tableId: String)
  final case class JoinGameResp(joinError: Option[String]) // TODO: make sealed trait for error

}
