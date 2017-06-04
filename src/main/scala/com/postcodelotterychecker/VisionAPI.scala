package com.postcodelotterychecker

import java.util.Base64

import cats.syntax.either._
import com.typesafe.scalalogging.StrictLogging
import io.circe.parser._

import scalaj.http.{Http, HttpOptions}

object VisionAPI extends StrictLogging {

  def makeRequest(imageByteArray: Array[Byte]) = {
    val imageBase64 = convertImageToBase64(imageByteArray)
    val apiResponse = getApiResponse(imageBase64)
    parse(apiResponse).toOption.flatMap(json => {
      val postcodeOpt = json.hcursor.downField("responses").downArray
        .downField("textAnnotations").downArray
        .get[String]("description").toOption
      postcodeOpt.map(_.replaceAll("[\n ]", ""))
    })
  }

  private def getApiResponse(imageBase64: Array[Byte]): String = {
    val url = "https://vision.googleapis.com/v1/images:annotate" + "?key=***REMOVED***"
    val requests =
      s"""
        |
        |  {"requests":[
        |    {
        |      "image":{
        |        "content": "${new String(imageBase64)}"
        |      },
        |      "features":[
        |        {
        |          "type":"TEXT_DETECTION",
        |          "maxResults":1
        |        }
        |      ]
        |    }
        |  ]
        |}
        |
    """.stripMargin

    Http(url)
      .postData(requests)
      .header("content-type", "application/json")
      .options(HttpOptions.followRedirects(true)).asString.body
  }

  private def convertImageToBase64(imageByteArray: Array[Byte]): Array[Byte] = {
    Base64.getEncoder.encode(imageByteArray)
  }
}

