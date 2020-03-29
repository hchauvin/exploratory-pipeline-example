package io.hchauvin.reflow

import java.net.URI
import java.nio.file.Files

import com.holdenkarau.spark.testing.DataFrameSuiteBase
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ReflowSpec extends AnyFlatSpec with Matchers with DataFrameSuiteBase {
  behavior of "run"

  it should "run hello world" in {
    val batchParentPath = Files.createTempDirectory("")
    val rf = new Reflow(batchParentPath)
    rf.run(workflow("hello.rf"), Map("world" -> "qux"), local = true)
  }

  behavior of "runbatch"

  it should "run the hello world batch" in {
    val sqlCtx = sqlContext
    import sqlCtx.implicits._

    val runs = Seq(
      ("id1", "world"),
      ("id2", "qux")
    ).toDF("id", "world")

    val batchParentPath = Files.createTempDirectory("")
    val rf = new Reflow(batchParentPath)
    rf.runbatch("helloWorld", workflow("hello.rf"), runs, local = true)
  }

  private lazy val embeddedWorkflows: URI = classOf[Reflow].getClassLoader
    .getResource("io/hchauvin/reflow/workflows")
    .toURI
  private lazy val resourceWorkflowPath =
    Reflow.resourceWorkflowPath(embeddedWorkflows)
  private def workflow(relative: String): String =
    resourceWorkflowPath.resolve(relative).toString
}
