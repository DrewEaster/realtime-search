package utils

import org.elasticsearch.node.NodeBuilder._
import org.elasticsearch.common.settings.ImmutableSettings._
import java.io.File

/**
  */
class EmbeddedESServer(val dataDirectory: File) {

  val settings = settingsBuilder.put("path.data", dataDirectory.getAbsolutePath)

  val node = nodeBuilder.local(true).settings(settings.build).node

  val client = node.client

  def shutdown() {
    node.close
  }
}
