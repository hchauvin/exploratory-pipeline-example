// SPDX-License-Identifier: MIT
// Copyright (c) 2020 Hadrien Chauvin

package io.hchauvin.bio

import io.hchauvin.r
import org.rosuda.JRI.REXP

/**
  * Represents an expression matrix, with features in rows and
  * samples in columns.
  *
  * @param rexp The R representation of the expression matrix.
  */
class ExpressionMatrix(val rexp: REXP) {

  /**
    * Normalizes the expression matrix by ensuring that all the
    * samples have the same expression quantiles.
    *
    * @return The normalized expression matrix.
    */
  def normalizeQuantiles(): ExpressionMatrix = {
    r.assign("mat", rexp)
    val rexpResult = r.$("limma::normalizeQuantiles(mat)")
    new ExpressionMatrix(rexpResult)
  }

}
