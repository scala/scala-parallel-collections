/*
 * Scala (https://www.scala-lang.org)
 *
 * Copyright EPFL and Lightbend, Inc.
 *
 * Licensed under Apache License 2.0
 * (http://www.apache.org/licenses/LICENSE-2.0).
 *
 * See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.
 */

package testutil

import scala.compiletime.testing._

/**
 * Ensures that a code snippet does not typecheck.
 */
object ShouldNotTypecheck {
  inline def apply(code: String): Unit = assert(!typeChecks(code))
  inline def apply(code: String, expected: String): Unit = apply(code)
}
