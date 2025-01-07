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

trait IntValues {
  def values = Seq(
      arbitrary[Int],
      arbitrary[Int] suchThat (_ >= 0),
      arbitrary[Int] suchThat (_ < 0),
      choose(0, 0),
      choose(0, 10),
      choose(0, 100),
      choose(0, 1000) suchThat (_ % 2 == 0),
      choose(0, 1000) suchThat (_ % 2 != 0),
      choose(0, 1000) suchThat (n => (n % 2 == 0) || (n % 3 == 0))
    )
}
