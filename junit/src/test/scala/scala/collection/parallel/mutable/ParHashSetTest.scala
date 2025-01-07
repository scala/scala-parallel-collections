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

package scala.collection.parallel.mutable

import org.junit.Test
import org.junit.Assert._

// based on run/hashset.scala partest
class ParHashSetTest {
  @Test
  def testPar: Unit = {
    val h1 = new ParHashSet[Int]
    for (i <- 0 until 20) h1 += i
    for (i <- 0 until 20) assertTrue(h1.contains(i))
    for (i <- 20 until 40) assertFalse(h1.contains(i))
    assertEquals((0 until 20).toList.sorted, h1.toList.sorted)

    val h2 = new ParHashSet[String]
    h2 += null
    for (i <- 0 until 20) h2 +=  "" + i
    assertTrue(h2 contains null)
    for (i <- 0 until 20) assertTrue(h2.contains("" + i))
    for (i <- 20 until 40) assertFalse(h2.contains("" + i))
    assertEquals((0 until 20).map("" + _).toList.sorted.mkString(",") + ",null", h2.toList.map("" + _).sorted.mkString(","))

    h2 -= null
    h2 -= "" + 0
    assertFalse(h2 contains null)
    assertEquals((1 until 20).map("" + _).toList.sorted.mkString(","), h2.toList.map("" + _).sorted.mkString(","))
  }
}
