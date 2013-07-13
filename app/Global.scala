import java.io.File
import org.apache.commons.io.FileUtils
import play.api._
import utils.EmbeddedESServer

object Global extends GlobalSettings {

  var esServer: EmbeddedESServer = _

  var esDataDirectory: File = _

  override def onStart(app: Application) {
    esDataDirectory = new File(app.path, "elasticsearch-data")
    FileUtils.deleteDirectory(esDataDirectory)
    esServer = new EmbeddedESServer(esDataDirectory)
    esServer.client.admin.indices.prepareCreate("logentries").execute().get
  }

  override def onStop(app: Application) {
    esServer.shutdown
    FileUtils.deleteDirectory(esDataDirectory)
  }
}