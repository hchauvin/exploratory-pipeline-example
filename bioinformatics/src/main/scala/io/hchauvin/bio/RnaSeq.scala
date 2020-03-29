// SPDX-License-Identifier: MIT
// Copyright (c) 2020 Hadrien Chauvin

package io.hchauvin.bio

import io.hchauvin.reflow.Reflow
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions.typedLit

/**
  * RNA Sequencing.
  */
object RnaSeq {

  /**
    * Represents a run of the RNA sequencing workflow.
    *
    * @param sraAcc The accession number for the SRA run.
    * @param fastaURL URL to the reference genome, in a gzipped-fasta format.
    */
  case class Run(sraAcc: String, fastaURL: String)

  /**
    * Executes RNA sequencing in batch mode.
    *
    * @param rf The Reflow client.
    * @param runName The name of the batch run.
    * @param outputPrefix Where to put the output of the run.  All the files for a
    *                     given run "sraAcc" will be under "outputPrefix/sraAcc",
    *                     e.g. "outputPrefix/sraAcc/abundance.txt".
    * @param runs A sequence of runs.
    * @return A lazily-evaluated DataFrame with all the abundance data.  An additional
    *         "sraAcc" column gives the sample ID for the abundance data.
    */
  def batch(
      rf: Reflow,
      runName: String,
      outputPrefix: String,
      runs: Seq[Run]
  ): DataFrame = {
    val normalizedOutputPrefix = normalizeFilePrefix(outputPrefix)

    val session = SparkSession.active
    import session.implicits._

    rf.runbatch(
      "count",
      workflowsPath.resolve("rna_seq.rf").toString,
      runs
        .map(r =>
          (
            r.sraAcc,
            r.sraAcc,
            r.fastaURL,
            normalizedOutputPrefix.replace("s3a://", "s3://") + r.sraAcc + "/"
          )
        )
        .toDF("id", "sraAcc", "referenceGenomeURL", "outputPrefix")
    )

    val dfs = runs.map(run =>
      session.read
        .option("sep", "\t")
        .option("header", "true")
        .schema(Stringtie.abundanceSchema)
        .csv(normalizedOutputPrefix + run.sraAcc + "/abundance.txt")
        .withColumn("sraAcc", typedLit(run.sraAcc))
    )
    dfs.reduce(_ union _)
  }
}
