import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{Matchers, WordSpec}
import scala.collection.immutable.Seq
import akka.http.scaladsl.model.headers.RawHeader

class endpointTester extends WordSpec with Matchers with ScalatestRouteTest with ScalaFutures with testMethods {

  implicit val defaultPatience: PatienceConfig =
    PatienceConfig(timeout = Span(20, Seconds), interval = Span(1000, Millis))

  "GitHub Service - GET" should {

    val endpoint: String = "https://api.github.com/zen"
    val responseBodyExpected: String = "Encourage flow."
    val responseHeaderExpected: String = "GitHub.com"

    "endpoint is up" in {
      whenReady(responseFutureGet(endpoint))(checkStatus200)
    }

    "response is of correct Type" in {
      whenReady(responseFutureGet(endpoint))(checkResponseTypeText)
    }

    "response Body contains" in {
      whenReady(responseFutureGet(endpoint))(response => checkSuccesfulBodyContains(responseBodyExpected, response))
    }

    "response Header contains" in {
      whenReady(responseFutureGet(endpoint))(response => checkHeaderContains(responseHeaderExpected, response))
    }
  }

  "GitHub Service - GET with Headers" should {

    val endpoint: String = "https://api.github.com/user"
    val headers = Map("Authorization" -> "token my_oauth_token_here")
    /*val headers = Seq(RawHeader("-u", "telangn: my_oath_token_here"))*/

    "response is of correct Type" in {
      whenReady(responseFutureGet(endpoint, convertHeaders(headers)))(checkResponseTypeJson)
    }

    "print response" in {
      whenReady(responseFutureGet(endpoint, convertHeaders(headers)))(printJsonResponse)
    }
  }

  "Json-Server - POST JSON Body" should {

    val endpoint: String = "http://localhost:3000/posts"
    val headers = Map("Content-Type" -> "application/json")
    val body = "{\n    \"id\": 24,\n    \"title\": \"scala-akka-test\",\n    \"author\": \"Ninad Telang\"\n  }"

    "post body" in {
      whenReady(responseFuturePost(endpoint, convertPostData(body), convertHeaders(headers)))(checkStatus201)
    }
  }

  "POST PDF File to Google Drive" should {

    val endpoint: String = "https://www.googleapis.com/upload/drive/v3/files?uploadType=media"
    val fileLocation: String = "/example.pdf"
    val headers = Map("Authorization" -> "Bearer my_oauth_token_here", "Content-Type" -> "application/pdf", "Content-Length" -> "100000")

    "post file" in {
      whenReady(responseFuturePostFile(endpoint, fileLocation, convertHeaders(headers)))(checkStatus200)
    }
  }
}



