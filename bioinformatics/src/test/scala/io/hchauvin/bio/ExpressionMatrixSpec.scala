// SPDX-License-Identifier: MIT
// Copyright (c) 2020 Hadrien Chauvin

package io.hchauvin.bio

import com.holdenkarau.spark.testing.DataFrameSuiteBase
import io.hchauvin.r
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ExpressionMatrixSpec
    extends AnyFlatSpec
    with Matchers
    with DataFrameSuiteBase {
  behavior of "normalizeQuantiles"

  it should "normalize quantiles" in {
    val sqlCtx = sqlContext
    import sqlCtx.implicits._

    val df = Seq(
      ("ACC1", "Ref1", 10),
      ("ACC1", "Ref2", 20),
      ("ACC2", "Ref1", 30),
      ("ACC2", "Ref2", 0),
      ("ACC2", "Ref3", 40)
    ).toDF("sraAcc", "Reference", "value")

    val mat = Stringtie.toExpressionMatrix(df, "value")

    val mat2 = mat.normalizeQuantiles()

    r.assign("mat", mat2.rexp)
    r.$("all(dim(mat) == c(2, 2))").asBool().isTRUE should be(true)

    mat.rexp.asDoubleMatrix()(1)(1) should be(0)
  }
}
