// SPDX-License-Identifier: MIT
// Copyright (c) 2020 Hadrien Chauvin

package io.hchauvin

import java.net.URI
import java.nio.file.{Path, Paths}

import io.hchauvin.reflow.Reflow

/**
  * Provides bioinformatics wrappers and tools.
  */
package object bio {
  private val embeddedWorkflows: URI = classOf[ExpressionMatrix].getClassLoader
    .getResource("io/hchauvin/bio/workflows")
    .toURI

  private[bio] def normalizeFilePrefix(prefix: String): String = {
    if (!prefix.endsWith("/")) prefix + '/'
    else prefix
  }

  private[bio] val workflowsPathEnvVar = "WORKFLOWS_PATH"

  private[bio] lazy val workflowsPath: Path = sys.env
    .get(workflowsPathEnvVar)
    .map(envValue => Paths.get(envValue))
    .getOrElse { Reflow.resourceWorkflowPath(embeddedWorkflows) }
}
