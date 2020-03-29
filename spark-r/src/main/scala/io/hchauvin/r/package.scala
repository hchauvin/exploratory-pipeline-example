// SPDX-License-Identifier: MIT
// Copyright (c) 2020 Hadrien Chauvin

package io.hchauvin

import org.rosuda.JRI.{REXP, Rengine}

/**
  * Interface with the R Statistical Language, through a JRI bridge.
  *
  * There can be only one instance of JRI bridge per JVM, as R is
  * mono-threaded.
  */
package object r {

  /**
    * Exposes the JRI directly.
    */
  lazy val engine: Rengine = {
    if (!Rengine.versionCheck()) {
      throw new RuntimeException("version mismatch")
    }

    val re = new Rengine(Array[String]("--vanilla"), false, null)

    if (!re.waitForR()) {
      throw new RuntimeException("cannot load")
    }

    val rexp = re.eval("options(device=png)")
    assert(rexp != null)

    re
  }

  /**
    * Evaluates a piece of code and returns its result.
    *
    * An error is thrown if there is an error.  Multiple statements
    * can be given, and all will be executed.
    */
  def $(code: String): REXP = {
    // TODO: Catch and display errors
    val rexp = engine.eval(s"{${code}}")
    assert(rexp != null)
    rexp
  }

  /**
    * Assigns an R expression to a global variable.
    *
    * @param name The name of the global variable.
    * @param value The value to give to the global variable.
    */
  def assign(name: String, value: REXP): Unit = {
    val assigned = engine.assign(name, value)
    assert(assigned)
  }

  /**
    * Represents an image as produced by some R code, typically
    * during plotting.
    *
    * Such an image will be displayed as such by Polynote, thanks
    * to the "ReprsOf" pattern.
    *
    * @param format Format of the image (e.g., "png").
    * @param content Base64 content.
    */
  case class Image(format: String, content: String)

  import polynote.runtime.{ReprsOf, ValueRepr, MIMERepr}

  trait ImageReprsOf[T] extends ReprsOf[T]

  object ImageReprsOf {
    implicit val image: ImageReprsOf[Image] =
      new ImageReprsOf[Image] {
        def apply(value: Image): Array[ValueRepr] = Array(
          MIMERepr(
            mimeType = s"image/${value.format}",
            content = value.content
          )
        )
      }
  }

  /**
    * Executes a piece of code, captures the last image being plot and returns it.
    *
    * @param code Piece of code to execute.
    * @return An image. Such an image is displayed correctly by Polynote, thanks
    *         to the "ReprsOf" pattern.
    */
  def plot(code: String): Image = {
    val assigned = engine.assign("plot.code", code)
    assert(assigned)

    val base64PngImage = $(s"""
        gr <- NULL
        evaluate::evaluate(
          plot.code,
          new_device=TRUE,
          output_handler=evaluate::new_output_handler(
            graphics=function(g) { gr <<- g }
        ))

        base64enc::base64encode(repr::repr_png(gr))
    """)

    Image(format = "png", content = base64PngImage.asString)
  }
}
