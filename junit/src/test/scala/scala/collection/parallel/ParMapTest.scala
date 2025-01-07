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

import scala.collection._
import scala.collection.parallel.CollectionConverters._
import org.junit.Test
import org.junit.Assert._

// based on run/parmap-ops.scala partest
class ParMapTest {

  @Test
  def test: Unit = {
    val gm: ParMap[Int, Int] = Map(0 -> 0, 1 -> 1).par

    // ops
    assertTrue(gm.isDefinedAt(1))
    assertTrue(gm.contains(1))
    assertTrue(gm.getOrElse(1, 2) == 1)
    assertTrue(gm.getOrElse(2, 3) == 3)
    assertTrue(gm.keysIterator.toSet == Set(0, 1))
    assertTrue(gm.valuesIterator.toSet == Set(0, 1))
    assertTrue(gm.keySet == ParSet(0, 1))
    assertTrue(gm.keys.toSet == ParSet(0, 1))
    assertTrue(gm.values.toSet == ParSet(0, 1))
    try {
      gm.default(-1)
      assertTrue(false)
    } catch {
      case e: NoSuchElementException => // ok
    }

    assertTrue(gm.filterKeys(_ % 2 == 0)(0) == 0)
    assertTrue(gm.filterKeys(_ % 2 == 0).get(1) == None)
    assertTrue(gm.mapValues(_ + 1)(0) == 1)

    // with defaults
    val pm = parallel.mutable.ParMap(0 -> 0, 1 -> 1)
    val dm = pm.withDefault(x => -x)
    assertTrue(dm(0) == 0)
    assertTrue(dm(1) == 1)
    assertTrue(dm(2) == -2)
    assertTrue(dm.updated(2, 2) == parallel.ParMap(0 -> 0, 1 -> 1, 2 -> 2))
    dm.put(3, 3)
    assertTrue(dm(3) == 3)
    assertTrue(pm(3) == 3)
    assertTrue(dm(4) == -4)

    val imdm = parallel.immutable.ParMap(0 -> 0, 1 -> 1).withDefault(x => -x)
    assertTrue(imdm(0) == 0)
    assertTrue(imdm(1) == 1)
    assertTrue(imdm(2) == -2)
    assertTrue(imdm.updated(2, 2) == parallel.ParMap(0 -> 0, 1 -> 1, 2 -> 2))
  }

}
