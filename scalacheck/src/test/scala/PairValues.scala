/*
 * Scala (https://www.scala-lang.org)
 *
 * Copyright EPFL and Lightbend, Inc. dba Akka
 *
 * Licensed under Apache License 2.0
 * (http://www.apache.org/licenses/LICENSE-2.0).
 *
 * See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.
 */

package scala.collection.parallel.ops

import org.scalacheck._
import org.scalacheck.Gen
import org.scalacheck.Gen._
import org.scalacheck.Prop._
import org.scalacheck.Properties
import org.scalacheck.Arbitrary._

trait PairValues[K, V] {
  def kvalues: Seq[Gen[K]]
  def vvalues: Seq[Gen[V]]

  def values = for {
    kg <- kvalues
    vg <- vvalues
  } yield for {
    k <- kg
    v <- vg
  } yield (k, v)
}
