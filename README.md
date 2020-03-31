# Bioinformatics workflows with Spark and Reflow: an example

[![CircleCI](https://circleci.com/gh/hchauvin/exploratory-pipeline-example/tree/master.svg?style=svg)](https://circleci.com/gh/hchauvin/exploratory-pipeline-example/tree/master) [![scala: 2.12](https://img.shields.io/badge/scala-2.12-red.svg)](https://opensource.org/licenses/MIT) [![spark: 3.0](https://img.shields.io/badge/spark-3.0-orange.svg)](https://opensource.org/licenses/MIT) [![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

This project showcases a simple bioinformatics workflow.  The end product is a
heatmap of normalized gene expressions across related samples.  The workflow
features [Spark](https://spark.apache.org/) and
[Reflow](https://github.com/grailbio/reflow) heavily.  The approach developed
here is suitable for both small sample series and large-scale meta-analyses.

<img alt="Heatmap" src = "https://github.com/hchauvin/exploratory-pipeline-example/raw/master/doc/heatmap.png" width="300" />

The workflow starts from a series of RNAseq reads fetched from the Sequence
Read Archive (SRA, NCBI).  The reads are aligned on a reference genome from
Ensembl using [hisat2](http://daehwankimlab.github.io/hisat2/), the alignments
are sorted and indexed using [samtools](http://www.htslib.org/doc/samtools.html),
and the sorted alignments are assembled into potential transcripts using
[stringtie](http://ccb.jhu.edu/software/stringtie/).  We do not perform
any quality control.  The gene abundances given by stringtie are normalized using
the quantile method of the [limma](https://bioconductor.org/packages/release/bioc/html/limma.html)
bioconductor package, and the heatmap is produced with
[ComplexHeatmap](https://bioconductor.org/packages/release/bioc/html/ComplexHeatmap.html).

Overall, the workflow is far from authoritative, there
are many other ways to approach feature counting, and it should only be
viewed as an example of how to conduct bioinformatics exploratory analysis.

Spark is used to do calculation on dataframes that can be massive.  Scala is used
for its type system and the JVM ecosystem, particularly suited to network calls
and interacting with third-party APIs (such as the NCBI or Ensembl APIs).  R
is used for its rich ecosystem of statistical techniques.  Reflow is used to
coordinate interrelated jobs on AWS S3.  The whole analysis, from feature counting
to producing a heatmap of normalized gene expressions, lies in a single [Polynote](https://polynote.org/)
notebook, [./RnaSeqExample.ipynb]()

## Development

Unit and integration tests:

```bash
sbt test
```

Code formatting:

```
sbt scalafmtAll
```

## License

`exploratory-pipeline-example` is licensed under [The MIT License](./LICENSE).

[![FOSSA Status](https://app.fossa.com/api/projects/git%2Bgithub.com%2Fhchauvin%2Fexploratory-pipeline-example.svg?type=large)](https://app.fossa.com/projects/git%2Bgithub.com%2Fhchauvin%2Fexploratory-pipeline-example?ref=badge_large)
