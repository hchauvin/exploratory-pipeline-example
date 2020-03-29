# Install all the required R packages

pak::pkg_install(c(
  "magrittr",
  "limma",
  "base64enc",
  "reshape2",
  "ComplexHeatmap",
  "rJava"
))