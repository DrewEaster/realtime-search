package actors

import akka.actor.{Actor}
import java.util.Random
import play.api.libs.json.Json
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import models.{LogEntry, Tick}

/**
  */
class LogEntryProducerActor extends Actor {

  val timestampFormat = ISODateTimeFormat.dateTime()

  val devices = Array("Desktop", "Tablet", "Phone", "TV")

  val userAgents = Array("Chrome", "Firefox", "Internet Explorer", "Safari", "HttpClient")

  val paths = Array("/a", "/b", "/c", "/d", "/e")

  val methods = Array("GET", "POST", "PUT", "DELETE")

  val statuses = Array(200, 404, 201, 500)

  def receive = {
    case Tick => sender ! LogEntry(generateLogEntry)
  }

  private def generateLogEntry = {
    Json.obj(
      "timestamp" -> currentTimestamp,
      "response_time" -> randomResponseTime,
      "method" -> randomElement(methods),
      "path" -> randomElement(paths),
      "status" -> randomElement(statuses),
      "device" -> randomElement(devices),
      "user_agent" -> randomElement(userAgents)
    )
  }

  private def randomElement[A](list: Array[A]) = {
    val rand = new Random(System.currentTimeMillis())
    val randomIndex = rand.nextInt(list.length)
    list(randomIndex)
  }

  private def randomResponseTime = new Random(System.currentTimeMillis()).nextInt(1000)

  private def currentTimestamp = timestampFormat.print(new DateTime(System.currentTimeMillis()))
}
