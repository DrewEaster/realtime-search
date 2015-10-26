package actors

import models._

import play.api.Logger
import java.util.UUID
import scala.collection.mutable.HashMap
import scala.concurrent.duration._

import akka.actor.{Props, Actor, ActorRef}
import play.api.Logger
import play.api.libs.iteratee.{Concurrent}
import play.api.libs.ws.WS
import play.api.Play.current
import play.api.libs.json.{JsArray, JsValue, Json}

import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global

import play.api.libs.json.Json


/**
  */
class MainSearchActor extends Actor {

  var channels = new HashMap[UUID, Concurrent.Channel[JsValue]]

  val elasticSearchActor = context.system.actorOf(Props[ElasticsearchActor], s"elasticSearchActor")

  val logEntryProducerActor = context.system.actorOf(Props[LogEntryProducerActor], s"logEntryProducerActor")
  val initializeElasticSearch = context.system.scheduler.scheduleOnce(3 second, self, InitializeES)
  val cancellable = context.system.scheduler.schedule(3 second, 1 second, self, Tick)

  def receive = {
    case InitializeES => init()
    case startSearch: StartSearch => sender ! SearchFeed(startSearching(startSearch))
    case stopSearch: StopSearch => stopSearching(stopSearch)
    case searchMatch: SearchMatch => broadcastMatch(searchMatch)
    case logEntry: LogEntry => elasticSearchActor ! logEntry
    case Tick => logEntryProducerActor ! Tick
  }

  // Used the scheduler and the IntializeES message to trigger the initialization of ES  
  // NOTE: having a mapping registered prior to doing percolate is now required by ES
  def init(): Unit = { 
    val mappingUrl = "http://localhost:9200/logentries/logentry/_mapping"
    WS.url("http://localhost:9200/logentries/logentry/_mapping").post(Json.stringify(LogEntryProducerActor.logEntryESMapping))
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
