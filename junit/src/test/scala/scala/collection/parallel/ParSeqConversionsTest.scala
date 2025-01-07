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

package scala.collection.parallel

import CollectionConverters._

import org.junit.Test
import org.junit.Assert._

// test conversions between collections
// based on run/pc-conversions.scala partest
class ParSeqConversionsTest {

  @Test
  def testConversions: Unit = {
    // par.to* and to*.par tests
    assertToPar(scala.collection.parallel.mutable.ParArray(1 -> 1, 2 -> 2, 3 -> 3))
    assertToPar(scala.collection.parallel.immutable.ParVector(1 -> 1, 2 -> 2, 3 -> 3))
    assertToPar(scala.collection.parallel.mutable.ParHashMap(1 -> 2))
    assertToPar(scala.collection.parallel.mutable.ParHashSet(1 -> 2))
    assertToPar(scala.collection.parallel.immutable.ParHashMap(1 -> 2))
    assertToPar(scala.collection.parallel.immutable.ParHashSet(1 -> 3))

    assertToParWoMap(scala.collection.parallel.immutable.ParRange(1, 10, 2, false))
    assertToParWoMap(scala.collection.parallel.immutable.ParVector(1, 2, 3))
    assertToParWoMap(scala.collection.parallel.mutable.ParArray(1, 2, 3))

    // seq and par again conversions)
    assertSeqPar(scala.collection.parallel.mutable.ParArray(1, 2, 3))
    assertSeqPar(scala.collection.parallel.immutable.ParVector(1, 2, 3))
    assertSeqPar(scala.collection.parallel.immutable.ParRange(1, 50, 1, false))
  }

  def assertSeqPar[T](pc: scala.collection.parallel.ParIterable[T]) = pc.seq.par == pc

  def assertToPar[K, V](xs: scala.collection.parallel.ParIterable[(K, V)]): Unit = {
    assertTrue(xs.toSeq.par == xs.toSeq)
    assertTrue(xs.par.toSeq == xs.toSeq)

//    assertTrue(xs.toSet.par == xs.toSet)
//    assertTrue(xs.par.toSet == xs.toSet)

//    assertTrue(xs.toMap.par == xs.toMap)
//    assertTrue(xs.par.toMap == xs.toMap)
  }

  def assertToParWoMap[T](xs: scala.collection.parallel.ParSeq[T]): Unit = {
//    assertTrue(xs.toIterable.par == xs.toIterable)
//    assertTrue(xs.par.toIterable == xs.toIterable)

    assertTrue(xs.toSeq.par == xs.toSeq)
    assertTrue(xs.par.toSeq == xs.toSeq)

//    assertTrue(xs.toSet.par == xs.toSet)
//    assertTrue(xs.par.toSet == xs.toSet)
  }

}
