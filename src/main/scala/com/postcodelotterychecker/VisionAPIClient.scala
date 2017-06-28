package com.postcodelotterychecker

import java.util.Base64

import cats.syntax.either._
import com.typesafe.scalalogging.StrictLogging
import io.circe.parser._

import scalaj.http.{Http, HttpOptions}

class VisionAPIClient(visionApiConfig: VisionApiConfig) extends StrictLogging {

  def makePostcodeCheckerRequest(imageByteArray: Array[Byte]): Option[String] = {
    val descriptionStrings = getDescriptionWords(imageByteArray)
    descriptionStrings.map(words => {
        val precedingBlock = words.indexWhere(_.startsWith("Main dra"))
        words(precedingBlock + 1).replaceAll(" ", "")
      })
  }

  def makeSurveyDrawCheckerRequest(imageByteArray: Array[Byte]): Option[String] = {
    val descriptionStrings = getDescriptionWords(imageByteArray)
    descriptionStrings.map(words => {
        val precedingBlock = words.indexWhere(_.startsWith("Survey Dra"))
        words(precedingBlock + 1).replaceAll(" ", "")
      })
  }

  def makeStackpotCheckerRequest(imageByteArray: Array[Byte]): Option[List[String]] = {
    val descriptionStrings = getDescriptionWords(imageByteArray)
    descriptionStrings.map(words => {
        val wordsNoSpaces = words.map(_.replaceAll(" ", ""))
        val precedingBlock = wordsNoSpaces.indexWhere(_.startsWith("Stackpot"))
        val procedingBlock = wordsNoSpaces.indexWhere(_.length == 3)
        wordsNoSpaces.slice(precedingBlock + 1, procedingBlock)
          .map(_.take(7))
      })
  }

  private def getApiResponse(imageBase64: Array[Byte]): String = {
    val url = s"https://vision.googleapis.com/v1/images:annotate?key=${visionApiConfig.apiKey}"
    val requests =
      s"""
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
         |      ],
         |       "imageContext": {
         |        "languageHints": "en"
         |      }
         |    }
         |  ]
         |}
    """.stripMargin

    Http(url)
      .postData(requests)
      .header("content-type", "application/json")
      .options(HttpOptions.followRedirects(true)).asString.body
  }

  private def getDescriptionWords(imageByteArray: Array[Byte]): Option[List[String]] = {
    val imageBase64 = convertImageToBase64(imageByteArray)
    val apiResponse = getApiResponse(imageBase64)
    parse(apiResponse).toOption.flatMap(json => {
      val resultOpt = json.hcursor.downField("responses").downArray
        .downField("textAnnotations").downArray
        .get[String]("description").toOption
      resultOpt.map(strings => {
        strings.split("\n").toList
      })
    })
  }

  private def convertImageToBase64(imageByteArray: Array[Byte]): Array[Byte] = {
    Base64.getEncoder.encode(imageByteArray)
  }
}

