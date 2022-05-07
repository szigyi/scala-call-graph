package hu.szigyi.scala.graph.output

import com.typesafe.scalalogging.StrictLogging
import hu.szigyi.scala.graph.Model._
import hu.szigyi.scala.graph.output.JsonOutput.transform
import hu.szigyi.scala.graph.service.Service

import scala.compiletime.summonAll
import scala.deriving.Mirror

class JsonOutput(service: Service) extends StrictLogging {

  def toJson(classLevels: Set[ClassLevel], separateRefTypes: Boolean): String = {
    val flattenedInvokations = service.toOutputInvokation(classLevels, separateRefTypes)
    logger.info(s"Converting references to ${flattenedInvokations.size} JSON objects")
    transform(flattenedInvokations)
  }

}

object JsonOutput {

  def transform(invokations: Seq[OutputInvokation]): String = {
    val nodes = invokations.flatMap(i => Seq(i.caller, i.referenced)).map { clazz =>
      val pckg = getPackageOfClass(clazz)
      s"""{
        "id": "$clazz",
        "group": "$pckg"
      }"""
    }
    val links = invokations.sortBy(_.count).map { i =>
      s"""{
        "source": "${i.caller}",
        "target": "${i.referenced}",
        "value": "${i.count}",
        "type": "${i.referenceType}"
      }"""
    }
    s"""{
       "nodes": [${nodes.mkString(",")}],
       "links": [${links.mkString(",")}]
    }"""
  }

  def getPackageOfClass(clazz: String): String =
    clazz.split("\\.").toList.dropRight(1).mkString(".")
}
