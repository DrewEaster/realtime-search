package actors
import play.api.Logger
import play.api.Play.current
import akka.actor.{ActorRef, Actor}
import models.{SearchMatch, StopSearch, LogEntry, StartSearch, RegisterQuery}
import play.api.libs.ws.WS
import play.api.libs.json.{JsArray, JsValue, Json}
import java.util.UUID
import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global
/**
  */
class ElasticsearchActor extends Actor {

  val logger = Logger("actors.ElasticsearchActor")
  
  def receive = {
    case LogEntry(data) => percolate(data, sender)
    case StartSearch(id, searchString) => registerQuery(id, searchString)
    case StopSearch(id) => unregisterQuery(id)
  }

  private def percolate(logJson: JsValue, requestor: ActorRef) {
    logger.debug(s"ElasticSearchActor.percolate called with ${logJson}")
    WS.url("http://localhost:9200/logentries/logentry/_percolate").post(Json.stringify(Json.obj("doc" -> logJson))).map {
      response =>
        val body = response.json
        if ((body \ "total").as[Int] > 0) {  
          val matchingIds = (body \ "matches").as[JsArray].value.foldLeft(List[UUID]()){(acc,v) => UUID.fromString((v \ "_id").as[String]) :: acc } 
          logger.debug(s"ElasticSearchActor.percolate response matchingIds = ${matchingIds}\n")
          if (!matchingIds.isEmpty) {
            requestor ! SearchMatch(LogEntry(logJson), matchingIds)
          } 
        }
    }
  }

  private def unregisterQuery(id: UUID) {
    WS.url("http://localhost:9200/_percolator/logentries/" + id.toString).delete
  }

  private def registerQuery(id: UUID, searchString: String) {
    logger.debug(s"ElasticSearchActor.registerQuery called with id = ${id} and searchString = ${searchString}")
    WS.url("http://localhost:9200/logentries/.percolator/" + id.toString).put(LogEntryProducerActor.queryAllStringFields(searchString)).map {
      response => 
        logger.debug(s"ElasticSearchActor.registerQuery response = ${response}" ) 
    }
  }
  
}
