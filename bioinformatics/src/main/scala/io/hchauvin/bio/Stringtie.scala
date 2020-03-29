// SPDX-License-Identifier: MIT
// Copyright (c) 2020 Hadrien Chauvin

package io.hchauvin.bio

import io.hchauvin.r
import org.apache.spark.sql.DataFrame

/**
  * Interface with the stringtie program.
  *
  * @see http://ccb.jhu.edu/software/stringtie/
  */
object Stringtie {

  /** Schema of the abundance file that is produced with the -A option. */
  val abundanceSchema = {
    import org.apache.spark.sql.types._

    StructType(
      Array(
        StructField("Gene ID", StringType, nullable = false),
        StructField("Gene Name", StringType, nullable = false),
        StructField("Reference", StringType, nullable = false),
        StructField("Strand", StringType, nullable = false),
        StructField("Start", IntegerType, nullable = false),
        StructField("End", IntegerType, nullable = false),
        StructField("Coverage", DoubleType, nullable = false),
        StructField("FPKM", DoubleType, nullable = false),
        StructField("TPM", DoubleType, nullable = false)
      )
    )
  }

  /** Canonical name of the abundance file. */
  val abundanceFile = "abundance.txt"

  /**
    * Converts an abundance DataFrame to an expression matrix of log-transformed
    * abundance data.
    *
    * @param df A DataFrame that contains abundance data.  The following
    *           columns are expected: "sraAcc" (sample ID), "Reference" (gene ID),
    *           values (the column with the values to put in the matrix).
    * @param values The name of the value column.
    * @return An expression matrix.
    */
  def toExpressionMatrix(
      df: DataFrame,
      values: String = "FPKM"
  ): ExpressionMatrix = {
    r.spark.assignRDF("df", df)
    r.engine.assign("values", values)

    val rexp = r.$("""
      library(magrittr)

      mat <- df %>%
        subset(Reference != "") %>%
        reshape2::acast(
          Reference ~ sraAcc,
          fun.aggregate=mean,
          value.var=values) %>%
        asinh() %>%
        .[apply(., 1, function (row) all(is.finite(row))),]
    """)

    new ExpressionMatrix(rexp)
  }
}
