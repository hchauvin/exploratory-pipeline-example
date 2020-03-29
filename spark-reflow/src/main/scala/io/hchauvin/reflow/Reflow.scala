package io.hchauvin.reflow

import java.net.URI
import java.nio.charset.StandardCharsets
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.{
  FileSystems,
  FileVisitResult,
  Files,
  Path,
  Paths,
  SimpleFileVisitor,
  StandardCopyOption
}

import org.apache.spark.sql.{DataFrame, SaveMode}

/**
  * Wrapper for the Reflow command-line program.
  *
  * Reflow allows the execution of distributed workflows with memoization and first-class
  * support for AWS.
  *
  * @param batchParentPath Where to put the batch configurations and outputs.
  */
class Reflow(batchParentPath: Path) {

  /**
    * Runs a workflow.
    *
    * @param program Local path to the workflow.
    * @param params Parameters to pass to the workflow (see the `Param` section
    *               in Reflow files).
    * @param local Whether to run the workflow locally if possible, or directly
    *              execute on some AWS EC2 instance.
    */
  def run(
      program: String,
      params: Map[String, Any] = Map(),
      local: Boolean = false
  ): Unit = {
    import sys.process._

    val stdout = new StringBuilder
    val stderr = new StringBuilder
    val status = Process(
      Seq("reflow", "run")
        ++ (if (local) Seq("-local") else Seq())
        ++ Seq(program)
        ++ params.map { case (k, v) => Seq("-" + k, v.toString) }.reduce {
          _ ++ _
        }
    ).!(ProcessLogger(stdout append _, stderr append _))

    if (status != 0) {
      println(status)
      println("stdout: " + stdout)
      println("stderr: " + stderr)

      throw new RuntimeException("non-zero exit status!")
    }

    println("stdout: " + stdout)
    println("stderr: " + stderr)
  }

  /**
    * Runs a workflow in batch mode.
    *
    * @param name    Name of the batch. All the files for the batch will be located
    *                in "${batchParentPath}/${name}".
    * @param program Local path to the workflow.
    * @param runs    Parameters to pass to the workflow, one row per run (see the `Param`
    *                section in Reflow files).
    * @param local   Whether to run the workflow locally if possible, or directly
    *                execute on some AWS EC2 instance.
    */
  def runbatch(
      name: String,
      program: String,
      runs: DataFrame,
      local: Boolean = false
  ): Unit = {
    val batchPath = batchParentPath.resolve(name).toAbsolutePath
    Files.createDirectories(batchPath)

    val runsFolder = batchPath.resolve("batch_csv")
    // TODO: delete runsFolder before coalesce
    runs
      .coalesce(1)
      .write
      .mode(SaveMode.Overwrite)
      .option("header", "true")
      .csv(runsFolder.toString)

    val runsFile = Files
      .newDirectoryStream(runsFolder, "part-*.csv")
      .iterator()
      .next()

    {
      import org.json4s.native.JsonMethods._
      import org.json4s.JsonDSL.WithDouble._

      val batch = ("program" -> program) ~
        ("runs_file" -> runsFolder.resolve(runsFile).toString)

      Files.write(
        batchPath.resolve("config.json"),
        pretty(render(batch)).getBytes(StandardCharsets.UTF_8)
      )
    }

    {
      import sys.process._

      val stdout = new StringBuilder
      val stderr = new StringBuilder
      val status = Process(
        Seq("reflow", "runbatch")
        // TODO: -local not supported
        // ++ (if (local) Seq("-local") else Seq())
        ,
        batchPath.toFile
      ).!(ProcessLogger(stdout append _, stderr append _))

      // TDO: Better error reporting
      if (status != 0) {
        println(status)
        println("stdout: " + stdout)
        println("stderr: " + stderr)

        throw new RuntimeException("non-zero exit status!")
      }

      println("stdout: " + stdout)
      println("stderr: " + stderr)
    }
  }
}

object Reflow {
  def resourceWorkflowPath(rootURI: URI): Path =
    if (rootURI.getScheme.equals("file")) Paths.get(rootURI.getPath)
    else {
      val target = Files.createTempDirectory("")

      println(s"rootURI: ${rootURI}")
      val fileSystem = FileSystems.newFileSystem(
        rootURI,
        null
      )

      val jarPath = fileSystem.getPath("/")
      Files.walkFileTree(
        jarPath,
        new SimpleFileVisitor[Path]() {
          private var currentTarget: Path = _

          override def preVisitDirectory(
              dir: Path,
              attrs: BasicFileAttributes
          ): FileVisitResult = {
            currentTarget = target.resolve(jarPath.relativize(dir).toString)
            Files.createDirectories(currentTarget)
            FileVisitResult.CONTINUE
          }

          override def visitFile(
              file: Path,
              attrs: BasicFileAttributes
          ): FileVisitResult = {
            Files.copy(
              file,
              target.resolve(jarPath.relativize(file).toString),
              StandardCopyOption.REPLACE_EXISTING
            )
            FileVisitResult.CONTINUE
          }
        }
      )

      target
    }
}
