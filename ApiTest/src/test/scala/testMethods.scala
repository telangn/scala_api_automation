import java.io.{File => JFile}
import java.util.Calendar

import akka.http.scaladsl.model.RequestEntity
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{Date, RawHeader}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.unmarshalling.Unmarshal
import org.scalatest.{Matchers, WordSpec}
import org.scalatest.concurrent.ScalaFutures
import scala.concurrent.Future
import scala.collection.immutable.Seq
import akka.http.scaladsl.model.Multipart.FormData.BodyPart
import com.google.protobuf.util.JsonFormat
import scala.reflect.io.File
import com.pgi.cabinet.api.File.{Recording, RecordingResponse}
import com.pgi.cabinet.api.FileOuterClass.RecordingMetaData.SourceApps
import scala.util.{Failure, Success}

trait testMethods extends WordSpec with Matchers with ScalatestRouteTest with ScalaFutures {

  def responseFuturePostFile(uri: String, fileLocation: String, headers: Seq[HttpHeader]=Seq()): Future[HttpResponse] = {

    val file = new File(new JFile(getClass.getResource(fileLocation).getPath))
    val formData = Multipart.FormData(BodyPart("convertPDFToFormData", HttpEntity(MediaTypes.`application/pdf`, file.toByteArray)))
    val fileEntity = formData.toEntity()

    Http().singleRequest(HttpRequest(uri = uri, method = HttpMethods.POST).withHeadersAndEntity(headers,fileEntity))
  }

  def convertPostData(data: String): RequestEntity = {
    HttpEntity(ContentTypes.`application/json`, data.getBytes())
  }

  def convertHeaders(headerMap: Map[String, String]): Seq[HttpHeader] = {
    headerMap.map { case (key, value) => RawHeader(key, value) }.to[Seq]
}

  def responseFutureGet(uri: String, headers: Seq[HttpHeader]=Seq()): Future[HttpResponse] = {
    Http().singleRequest(HttpRequest(uri = uri, method = HttpMethods.GET).withHeaders(headers))
  }

  def responseFuturePost(uri: String, body: RequestEntity, headers: Seq[HttpHeader]=Seq()): Future[HttpResponse] = {
    Http().singleRequest(HttpRequest(uri = uri, method = HttpMethods.POST).withHeadersAndEntity(headers, body))
  }

  def checkStatus200(response: HttpResponse) = {
    response.status shouldBe StatusCodes.OK
  }

  def checkStatus201(response: HttpResponse) = {
    response.status shouldBe StatusCodes.Created
  }

  def checkStatus404(response: HttpResponse) = {
    response.status shouldBe StatusCodes.NotFound
  }

  def checkResponseTypeText(response: HttpResponse) = {
    response.entity.contentType shouldBe ContentTypes.`text/plain(UTF-8)`
  }

  def checkResponseTypeJson(response: HttpResponse) = {
    response.entity.contentType shouldBe ContentTypes.`application/json`
  }

  def printJsonResponse(response: HttpResponse) = {
    print(response.entity.toString)
  }

  def checkSuccesfulBodyContains(responseBodyExpected: String, response: HttpResponse) = {
    val bodyFt: Future[String] = response match {
      case HttpResponse(StatusCodes.OK, _, _, _) => Unmarshal(response.entity).to[String]
      case _ => Future.failed(new Exception(s"Invalid response code: ${response.status}"))
    }
    whenReady(bodyFt) { body: String => body shouldBe responseBodyExpected }
  }

  def checkHeaderContains(responseHeaderExpected: String, response: HttpResponse) = {
    val header = response.headers.find(header => header.name.equalsIgnoreCase("server"))
    header.nonEmpty shouldBe true
    header.get.value shouldBe responseHeaderExpected
  }

  def checkRecordingResponse(response: HttpResponse) = {

    val changedResponseType = Unmarshal(response.entity).to[String]

    def buildResponse(json: String): RecordingResponse = {
      val builder = RecordingResponse.newBuilder()
      JsonFormat.parser().ignoringUnknownFields().merge(json, builder)
      builder.build()
    }

    val recordingFt: Future[Recording] = for {
      responseType <- changedResponseType
      recordingResponse <- Future(buildResponse(responseType))
      recording <- Future.successful(recordingResponse.getRecording)
    } yield recording

 /*   recordingFt.onComplete {

      case Success(recording) =>

        recording.getId shouldBe "123"
        recording.getClientId shouldBe "123456"
        recording.getCompanyId shouldBe "789456"
        recording.getMetaData.getFileName shouldBe "Recording A"
        recording.getMetaData.getContentType shouldBe "Content Type"
        recording.getMetaData.getCreatedOn shouldBe Calendar.getInstance().getTime
        recording.getMetaData.getSourceApp shouldBe 0
        recording.getDownloadUrl shouldBe "Download URL"
        recording.getPlaybackUrl shouldBe "Playback URL"
        recording.getRoomId shouldBe "123456"
        recording.getDuration shouldBe "PT10.5S"

      case Failure(exception) => print(exception)
    }*/

    whenReady(recordingFt) {recording: Recording =>

      recording.getId shouldBe "123"
      recording.getClientId shouldBe "123456"
      recording.getCompanyId shouldBe "789456"
      recording.getMetaData.getFileName shouldBe "Recording A"
      recording.getMetaData.getContentType shouldBe "Content Type"
      //recording.getMetaData.getCreatedOn shouldBe Calendar.getInstance().getTime
      recording.getMetaData.getSourceApp shouldBe SourceApps.valueOf(SourceApps.GM5_VALUE)
      recording.getDownloadUrl shouldBe "Download URL"
      recording.getPlaybackUrl shouldBe "Playback URL"
      recording.getRoomId shouldBe "123456"
      recording.getDuration shouldBe "PT10.5S"

    }
 }

}
