// SPDX-License-Identifier: MIT
// Copyright (c) 2020 Hadrien Chauvin

package io.hchauvin.r

import com.holdenkarau.spark.testing.DataFrameSuiteBase
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import io.hchauvin.r

class SparkSpec extends AnyFlatSpec with Matchers with DataFrameSuiteBase {
  behavior of "sparkDF"

  it should "convert a data.frame" in {
    val df = r.spark.sparkDF($("data.frame(a = c(1, 2), b = c('a', 'b'))"))

    /*import sqlContext.implicits._
    assertDataFrameEquals(df, Seq(
      (1, "a"),
      (2, "b")
    ).toDF)*/
  }

  behavior of "rDF"

  it should "convert a DataFrame" in {
    import sqlContext.implicits._

    val df = Seq(
      (1, "a"),
      (2, "b")
    ).toDF
    r.spark.assignRDF("df", df)

    val df2 = r.spark.sparkDF($("df"))
    /*assertDataFrameEquals(df2, df)*/
  }
}
