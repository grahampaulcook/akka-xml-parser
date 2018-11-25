/*
 * Copyright 2018 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import akka.stream.scaladsl.{Keep, Source}
import akka.util.ByteString
import org.scalatest
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, Matchers}
import uk.gov.hmrc.akka.xml.{FastParsingStage, XMLElement, XMLExtract, XMLParserFixtures}
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global

class OutputXMLMatchesInputXMLSpec extends UnitSpec with BeforeAndAfterEach with Matchers with ScalaFutures with MockitoSugar with Eventually with XMLParserFixtures {

  val inputXml                        = "<Address><Line>Line 1</Line><Line>Line 2</Line><PostCode>Tf3 4NT</PostCode></Address>"
  val inputXmlWithSelfClosingElement  = "<Address><Line>Line 1</Line><Line>Line 2</Line><Line/><PostCode>Tf3 4NT</PostCode></Address>"
  //                                                                                    ^^^^^^^

  val inputXmlWithBlankElement        = "<Address><Line>Line 1</Line><Line>Line 2</Line><Line></Line><PostCode>Tf3 4NT</PostCode></Address>"
  //                                                                                    ^^^^^^^^^^^^^


  val f = fixtures

  def xpathValue(xmlElements: Set[XMLElement], xPath: Seq[String]): Option[String] = xmlElements.collectFirst { case XMLElement(`xPath`, _, Some(xpathValue)) => xpathValue }

  def parseAndCompare(inputXml: String): scalatest.Assertion = {
    val inputXmlSource: Source[ByteString, _] = Source.single(ByteString(inputXml))

    await(
      for {
        parsedXmlElements <- inputXmlSource
          .via(FastParsingStage.parser(Seq(XMLExtract(Seq("Address"), Map.empty, true))))
          .via(f.flowXMLElements)
          .toMat(f.collectXMLElements)(Keep.right)
          .run()(f.mat)

        parsedXml = xpathValue(parsedXmlElements, Seq("Address"))
      } yield {

        val outputXml = parsedXml.get

        println(s"INPUT  XML = $inputXml")
        println(s"OUTPUT XML = $outputXml")
        println()

        outputXml shouldBe inputXml
      }
    )
  }


  "The output XML" should {
    "match the input XML" when {
      "blank elements *** ARE *** present"            in parseAndCompare(inputXmlWithBlankElement)
      "self closing elements are *** NOT *** present" in parseAndCompare(inputXml)
      "self closing elements *** ARE *** present"     in parseAndCompare(inputXmlWithSelfClosingElement)
    }
  }


}
