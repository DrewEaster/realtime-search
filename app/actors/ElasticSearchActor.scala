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

  def receive = {
    case LogEntry(data) => percolate(data, sender)
    case StartSearch(id, searchString) => registerQuery(id, searchString)
    case StopSearch(id) => unregisterQuery(id)
  }
  
  // Used for debugging... having this available and using it in place of the percolate method above 
  // in the receive method let me get the mapping sorted out
  // so that I could it could be registered before the application starts which is a requirement in ES now!
  // This was done by empirically looking at the default mapping that ES creates
  // Nice blog about exploring data and working with mappings: https://www.elastic.co/blog/found-mapping-workflow
  private def indexLogEntry(logJson: JsValue, requestor: ActorRef) {
    Logger.debug(s"ElasticSearchActor.indexLogEntry called with ${logJson}")
    // The version of WS below adds entries to the logentries index, it does not do percolation though.
    WS.url("http://localhost:9200/logentries/logentry/_percolate").post(Json.stringify(Json.obj("doc" -> logJson))).map {
      response =>
        Logger.debug(s">>>>>>>   ElasticSearchActor for indexLogEntry RESPONSE = ${response}")
    }
  }

  private def percolate(logJson: JsValue, requestor: ActorRef) {
    Logger.debug(s"ElasticSearchActor.percolate called with ${logJson}")
    WS.url("http://localhost:9200/logentries/logentry/_percolate").post(Json.stringify(Json.obj("doc" -> logJson))).map {
      response =>
        val body = response.json
        Logger.debug(s"          ElasticSearchActor response.json = ${body}\n\n")          
        if ((body \ "total").as[Int] > 0) {  
          val matchingIds = (body \ "matches").as[JsArray].value.foldLeft(List[UUID]()){(acc,v) => UUID.fromString((v \ "_id").as[String]) :: acc } 
          Logger.debug(s">>> percolate >>> matchingIds ${matchingIds}\n")
          if (!matchingIds.isEmpty) {
            Logger.debug(s">>> percolate >>> percolate >>> matchingIds=${matchingIds}")
            requestor ! SearchMatch(LogEntry(logJson), matchingIds)
          } 
        }
    }
  }

  private def unregisterQuery(id: UUID) {
    WS.url("http://localhost:9200/_percolator/logentries/" + id.toString).delete
  }

  private def registerQuery(id: UUID, searchString: String) {
    Logger.debug(s">>>> registerQuery called with id = ${id} and searchString = ${searchString}")
    val queryString = s"""{
        "query" : {
          "match" : { ${searchString}
          }
        }
      }"""
    WS.url("http://localhost:9200/logentries/.percolator/" + id.toString).put(queryString).map {
      response => 
        Logger.debug(s">>>>>>>> registerQuery response = ${response}" )
    }
  }
}
