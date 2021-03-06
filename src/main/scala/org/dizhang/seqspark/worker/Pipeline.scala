/*
 * Copyright 2017 Zhang Di
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

package org.dizhang.seqspark.worker

import org.dizhang.seqspark.ds.Genotype
import org.dizhang.seqspark.util.{SingleStudyContext, UserConfig}
import org.slf4j.{Logger, LoggerFactory}

import scala.util.parsing.combinator.JavaTokenParsers

/**
  * Pipeline is an abstract program
  * that transforms one type to another
  */
abstract class Pipeline(implicit ssc: SingleStudyContext)

object Pipeline {
  val logger: Logger = LoggerFactory.getLogger(getClass)
  //case object Dummy extends Pipeline
  case class Import[A](key: String)
                      (implicit ssc: SingleStudyContext) extends Pipeline
  case class QC[B, C](key: String, input: Pipeline)
                     (implicit ssc: SingleStudyContext) extends Pipeline
  case class Assoc[B](key: String, input: Pipeline)
                     (implicit ssc: SingleStudyContext) extends Pipeline

  /**
  class Program(implicit ssc: SingleStudyContext) extends JavaTokenParsers {
    def expr: Parser[Pipeline] = input | qc | assoc
    def input: Parser[Pipeline] = "(" ~> "input" ~> "\w+".r <~ ")" ^^ {
      key => Import(key)
    }
    def qc: Parser[Pipeline] = "(" ~> "qc" ~> expr ~ "\w+".r <~ ")" ^^ {
      case data ~ key => QC(key, data)
    }
    def assoc: Parser[Pipeline] = "(" ~> "assoc" ~> expr ~ "\w+".r <~ ")" ^^ {
      case data ~ key => Assoc(key, data)
    }
    def empty: Parser[Pipeline] = "(" ~> "\s*".r <~ ")" ^^ {
      space => Dummy
    }
    def parse(input: String): Pipeline = this.parseAll(expr, input).get
  }

  def make(p: List[String]): String = {
    if (p.isEmpty) {
      logger.warn("no task specified")
      "()"
    } else if (p.length > 2) {
      logger.warn("too many tasks")
      "()"
    } else if (p.length == 1) {
      if (p.head == "qualityControl") {
        "(qc (import input) qualityControl)"
      } else if (p.head == "association") {
        "(assoc (import inport) association)"
      } else {
        "()"
      }
    } else {
      "()"
    }
  }
*/
}