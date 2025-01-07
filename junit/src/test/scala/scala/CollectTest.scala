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

package scala

import org.junit.Assert._
import org.junit.{Ignore, Test}

import scala.collection.mutable.ArrayBuffer

// based on run/t6448.scala partest

// Tests to show that various `collect` functions avoid calling
// both `PartialFunction#isDefinedAt` and `PartialFunction#apply`.
//
class CollectTest {
  class Counter {
    var count = 0
    def apply(i: Int) = synchronized {count += 1; true}
  }

  @Test
  def testParVectorCollect: Unit = {
    val counter = new Counter()
    val res = collection.parallel.immutable.ParVector(1, 2) collect { case x if counter(x) && x < 2 => x}
    assertEquals(collection.parallel.immutable.ParVector(1), res)
    assertEquals(2, counter.synchronized(counter.count))
  }

  @Test
  def testParArrayCollect: Unit = {
    val counter = new Counter()
    val res = collection.parallel.mutable.ParArray(1, 2) collect { case x if counter(x) && x < 2 => x}
    assertEquals(collection.parallel.mutable.ParArray(1), res)
    assertEquals(2, counter.synchronized(counter.count))
  }
}
