import java.nio.ByteBuffer

import scala.concurrent._, duration._, ExecutionContext.Implicits._

import gigahorse._, support.okhttp.Gigahorse

import sjsonnew.shaded.scalajson.ast.unsafe._
import sjsonnew.support.scalajson.unsafe.{ Converter, Parser }

object GetScala3Next {
  val asJson = (r: FullResponse) => Parser.parseFromByteBuffer(r.bodyAsByteBuffer).get

  def get(): String = {
    val req = Gigahorse.url("https://api.github.com/repos/lampepfl/dotty/releases")
        .get.addQueryString("per_page" -> "1")

    val http = Gigahorse.http(Gigahorse.config)

    try {
      val f = http.run(req, asJson)

      val f2 = f.collect {
        case JArray(Array(JObject(fields))) => fields.collectFirst {
          case JField("tag_name", JString(version)) => version
        }
      }.map(_.getOrElse(sys.error(s"Expected an array of 1 string, got $f")))

      Await.result(f2, 120.seconds)
    } finally http.close()
  }
}
