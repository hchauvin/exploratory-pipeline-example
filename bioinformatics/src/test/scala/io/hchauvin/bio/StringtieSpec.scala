// SPDX-License-Identifier: MIT
// Copyright (c) 2020 Hadrien Chauvin

package io.hchauvin.bio

import com.holdenkarau.spark.testing.DataFrameSuiteBase
import org.scalactic.TolerantNumerics
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import io.hchauvin.r

class StringtieSpec extends AnyFlatSpec with Matchers with DataFrameSuiteBase {
  implicit val doubleEq = TolerantNumerics.tolerantDoubleEquality(1e-6)

  behavior of "toExpressionMatrix"

  it should "convert a data frame" in {
    val sqlCtx = sqlContext
    import sqlCtx.implicits._

    val df = Seq(
      ("ACC1", "Ref1", 10),
      ("ACC1", "Ref2", 20),
      ("ACC2", "Ref1", 30),
      ("ACC2", "Ref2", 0),
      ("ACC2", "Ref3", 40)
    ).toDF("sraAcc", "Reference", "value")

    val ans = Stringtie.toExpressionMatrix(df, "value").rexp
    val mat = ans.asDoubleMatrix

    def asinh(x: Double): Double = math.log(x + math.sqrt(x * x + 1.0))
    mat.length should ===(2)
    mat(0).length should ===(2)
    mat(0)(0) should ===(asinh(10.0))
    mat(0)(1) should ===(asinh(30.0))
    mat(1)(0) should ===(asinh(20.0))
    mat(1)(1) should ===(0.0)

    r.assign("mat", ans)
    assert(r.$("all(dim(mat) == c(2, 2))").asBool().isTRUE)
  }
}
