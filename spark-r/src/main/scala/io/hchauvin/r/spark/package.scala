// SPDX-License-Identifier: MIT
// Copyright (c) 2020 Hadrien Chauvin

package io.hchauvin.r

import org.apache.spark.sql.{DataFrame, Row, SparkSession}
import org.rosuda.JRI.REXP
import org.apache.spark.sql.types.{
  BooleanType,
  DataType,
  DoubleType,
  FloatType,
  IntegerType,
  LongType,
  StringType,
  StructField,
  StructType
}

/**
  * Crude R/Scala Spark interface.
  */
package object spark {
  // TODO: Do not collect before sending to R, use
  // https://spark.apache.org/docs/latest/sparkr.html#from-local-data-frames

  import io.hchauvin.r
  private val re = r.engine

  /**
    * Assigns a Spark DataFrame to a global variable in R, of class
    * "data.frame".
    *
    * @param name The name of the global variable.
    * @param df The dataframe to assign.
    */
  def assignRDF(name: String, df: DataFrame): Unit = {
    val assigned = re.rniAssign(name, rDF(df), 0)
    assert(assigned)
  }

  /**
    * Converts a Spark DataFrame into an R dataframe, and returns its ID
    * in the JRI bridge.
    *
    * This method is low-level, see [[assignRDF]] for a method more
    * high-level.
    *
    * @param df The dataframe to convert.
    * @return
    */
  def rDF(df: DataFrame): Long = {
    val rows = df.collect()

    val xpColumns: Seq[Long] = df.schema.fields.zipWithIndex
      .map {
        case (field, j) =>
          putJvmArray(field.dataType, rows.map(row => row.get(j)))
      }
    val xpVec = re.rniPutVector(xpColumns.toArray)

    val xpColumnNames = re.rniPutStringArray(df.columns)
    re.rniSetAttr(xpVec, "names", xpColumnNames)

    val xpClass = re.rniPutString("data.frame")
    re.rniSetAttr(xpVec, "class", xpClass)

    xpVec
  }

  /**
    * Converts an R expression into a Spark DataFrame.
    *
    * @param df The R expression that represents a dataframe.
    * @return The converted DataFrame.
    */
  def sparkDF(df: REXP): DataFrame = {
    val session = SparkSession.active

    // TODO: Check class "data.frame", check for empty data frames, ...

    val columnNames = df.asList().keys()
    val columns = columnNames.map(column => df.asList().at(column))
    val convertedColumns = columns.map(convertRArray)
    val nrow = convertedColumns(0).size
    import collection.JavaConverters._
    session.createDataFrame(
      (0 until nrow)
        .map(i => Row.fromSeq(convertedColumns.map(col => col(i))))
        .asJava,
      StructType(
        columns
          .map(convertRType)
          .zip(columnNames)
          .map {
            case (columnType, columnName) =>
              StructField(columnName, columnType, false)
          }
      )
    )
  }

  /**
    * Evaluates some piece of R code and converts the result to a Spark DataFrame.
    *
    * @param code The code to execute.
    * @return The Spark DataFrame.
    */
  def evalSparkDF(code: String): DataFrame = {
    sparkDF(re.eval(code))
  }

  private def putJvmArray(dataType: DataType, r: Seq[Any]): Long = {
    dataType match {
      case StringType =>
        re.rniPutStringArray(r.asInstanceOf[Seq[String]].toArray)
      case IntegerType =>
        re.rniPutDoubleArray(
          r.asInstanceOf[Seq[Int]].map { _.toDouble }.toArray
        )
      case DoubleType =>
        re.rniPutDoubleArray(r.asInstanceOf[Seq[Double]].toArray)
      case LongType =>
        re.rniPutDoubleArray(
          r.asInstanceOf[Seq[Long]].map { _.toDouble }.toArray
        )
      case FloatType =>
        re.rniPutDoubleArray(
          r.asInstanceOf[Seq[Float]].map { _.toDouble }.toArray
        )
      case BooleanType =>
        re.rniPutBoolArray(r.asInstanceOf[Seq[Boolean]].toArray)
    }
  }

  private def convertRArray(r: REXP): Seq[Any] = {
    r.getType() match {
      case REXP.XT_ARRAY_DOUBLE => r.asDoubleArray()
      case REXP.XT_ARRAY_INT    => r.asIntArray()
      case REXP.XT_ARRAY_STR    => r.asStringArray()
      case REXP.XT_FACTOR => {
        val f = r.asFactor()
        (0 to f.size()).map(i => f.at(i))
      }
      case REXP.XT_ARRAY_BOOL_INT => r.asIntArray().map(n => n == 1)
    }
  }

  private def convertRType(r: REXP): DataType = {
    r.getType() match {
      case REXP.XT_ARRAY_DOUBLE   => DoubleType
      case REXP.XT_ARRAY_INT      => IntegerType
      case REXP.XT_ARRAY_STR      => StringType
      case REXP.XT_FACTOR         => StringType
      case REXP.XT_ARRAY_BOOL_INT => BooleanType
    }
  }
}
