package hu.szigyi.scala.graph.output

import hu.szigyi.scala.graph.output.JsonOutput.getPackageOfClass
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

class JsonOutputSpec extends AnyFreeSpec with Matchers {

  "getPackageOfClass" - {
    "return the package where the class is in" in {
      getPackageOfClass("hu.szigyi.package.Anything") shouldBe "hu.szigyi.package"
    }
  }
}
