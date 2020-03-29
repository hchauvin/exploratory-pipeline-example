// SPDX-License-Identifier: MIT
// Copyright (c) 2020 Hadrien Chauvin

package io.hchauvin.r

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.util.Random

class RengineSpec extends AnyFlatSpec with Matchers {
  behavior of "r.$"

  it should "evaluate a numeric expression" in {
    val rexp = $("1 + 2")
    rexp.asDouble() should equal(3)
  }

  it should "evaluate two expressions in a row" in {
    val rexp = $("x <- 1; x + 2")
    rexp.asDouble() should equal(3)
  }

  it should "assign to a global variable" in {
    val name = randomName()
    assign(name, $("1 + 2"))
    $(name).asDouble() should equal(3)
  }

  behavior of "plot"

  it should "plot as a png image and return base64 image" in {
    val image = plot("plot(sqrt)")
    image.format should equal("png")
    image.content should fullyMatch regex base64Re
  }

  private def randomName(prefix: String = "var"): String =
    prefix + "_" + Random.alphanumeric.take(5).mkString

  private val base64Re =
    """^(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=)?$"""
}
