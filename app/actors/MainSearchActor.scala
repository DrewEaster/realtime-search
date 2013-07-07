package actors

import akka.actor.{Props, Actor}
import play.api.libs.iteratee.{Concurrent}
import models._
import scala.collection.mutable.HashMap
import java.util.UUID
import play.api.libs.json.JsValue

import scala.concurrent.duration._
import models.StartSearch
import models.SearchMatch
import models.StopSearch

import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global

/**
  */
class MainSearchActor extends Actor {

  var channels = new HashMap[UUID, Concurrent.Channel[JsValue]]

  val elasticSearchActor = context.system.actorOf(Props[ElasticsearchActor], "elasticSearchActor")

  val logEntryProducerActor = context.system.actorOf(Props[LogEntryProducerActor], "logEntryProducerActor")

  val cancellable = context.system.scheduler.schedule(0 second, 1 second, self, Tick)

  def receive = {
    case startSearch: StartSearch => sender ! SearchFeed(startSearching(startSearch))
    case stopSearch: StopSearch => stopSearching(stopSearch)
    case searchMatch: SearchMatch => broadcastMatch(searchMatch)
    case logEntry: LogEntry => elasticSearchActor ! logEntry
    case Tick => logEntryProducerActor ! Tick
  }

  override def postStop() {
    cancellable.cancel
    super.postStop
  }

  private def broadcastMatch(searchMatch: SearchMatch) {
    searchMatch.matchingChannelIds.foreach {
      channels.get(_).map {
        _ push searchMatch.logEntry.data
      }
    }
  }

  private def startSearching(startSearch: StartSearch) =
    Concurrent.unicast[JsValue](
      onStart = (c) => {
        channels += (startSearch.id -> c)
        elasticSearchActor ! startSearch
      },
      onComplete = {
        self ! StopSearch(startSearch.id)
      },
      onError = (str, in) => {
        self ! StopSearch(startSearch.id)
      }
    ).onDoneEnumerating(
      callback = {
        self ! StopSearch(startSearch.id)
      }
    )

  private def stopSearching(stopSearch: StopSearch) {
    channels -= stopSearch.id
    elasticSearchActor ! stopSearch
  }
}
