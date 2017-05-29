package com.postcodelotterychecker

import java.nio.file.{Files, Paths}
import java.util.Base64

import com.typesafe.scalalogging.StrictLogging
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.DefaultHttpClient
import io.circe._
import cats.syntax.either._
import io.circe.parser._

import scala.io.Source

object VisionAPI extends StrictLogging {

  def makeRequest(imageByteArray: Array[Byte]) = {
    val imageBase64 = convertImageToBase64(imageByteArray)
    val apiResponse = getApiResponse(imageBase64)
    logger.info(s"Vision Api response: $apiResponse")
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
    val client = new DefaultHttpClient
    val post = new HttpPost(url)
    post.setHeader("Content-type", "application/json")
    post.setEntity(new StringEntity(requests))

    val response = client.execute(post)
    val inputStream = response.getEntity.getContent
    val content = Source.fromInputStream(inputStream).getLines.mkString
    inputStream.close()
    content
  }

  private def convertImageToBase64(imageByteArray: Array[Byte]): Array[Byte] = {
    Base64.getEncoder.encode(imageByteArray)
  }
}

