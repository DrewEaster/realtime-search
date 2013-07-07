package actors

import akka.actor.{ActorRef, Actor}
import models.{SearchMatch, StopSearch, LogEntry, StartSearch}
import play.api.libs.ws.WS
import play.api.libs.json.{JsArray, JsValue, Json}
import java.util.UUID
import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global
import play.api.Logger

/**
  */
class ElasticsearchActor extends Actor {

  def receive = {
    case LogEntry(data) => percolate(data, sender)
    case StartSearch(id, searchString) => registerQuery(id, searchString)
    case StopSearch(id) => unregisterQuery(id)
  }

  private def percolate(logJson: JsValue, requestor: ActorRef) {
    WS.url("http://localhost:9200/logentries/logentry/_percolate").post(Json.stringify(Json.obj("doc" -> logJson))).map {
      response =>
        val body = response.json
        val status = (body \ "ok").as[Boolean]
        if (status) {
          val matchingIds = (body \ "matches").asInstanceOf[JsArray].value.foldLeft(List[UUID]())((acc, v) => UUID.fromString(v.as[String]) :: acc)
          if (!matchingIds.isEmpty) {
            requestor ! SearchMatch(LogEntry(logJson), matchingIds)
          }
        }
    }
  }

  private def unregisterQuery(id: UUID) {
    Logger.info("Unregistering percolation query with id '" + id + "'")
    WS.url("http://localhost:9200/_percolator/logentries/" + id.toString).delete
  }

  private def registerQuery(id: UUID, searchString: String) {
    Logger.info("Registering percolation query '" + searchString + "' with id '" + id + "'")
    val query = Json.obj(
      "query" -> Json.obj(
        "query_string" -> Json.obj(
          "query" -> searchString
        )))

    WS.url("http://localhost:9200/_percolator/logentries/" + id.toString).put(Json.stringify(query))
  }
}
