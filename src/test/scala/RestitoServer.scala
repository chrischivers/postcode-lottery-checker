
import com.xebialabs.restito.server.StubServer
import org.eclipse.jetty.http.HttpStatus
import org.jsoup.Connection.Method

class RestitoServer(val port: Int) {

  val server = new StubServer(port)

  def start() = {
    server.run()
  }

  def stop() = {
    server.stop
  }
}