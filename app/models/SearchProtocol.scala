package models

import play.api.libs.json.{Json, JsValue}
import java.util.UUID
import play.api.libs.iteratee.Enumerator

case object Tick

case class LogEntry(data: JsValue) {
  def stringify = Json.stringify(data)
}

case object InitializeES

case class SearchFeed(out: Enumerator[JsValue])

case class SearchMatch(logEntry: LogEntry, matchingChannelIds: List[UUID])

case class StartSearch(id: UUID = UUID.randomUUID(), searchString: String)

case class StopSearch(id: UUID)

case object RegisterQuery
