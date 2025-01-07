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

import org.junit.Test
import org.junit.Assert._

// based on jvm/serialization-new.scala partest
class SerializationTest {

  @throws(classOf[java.io.IOException])
  def write[A](o: A): Array[Byte] = {
    val ba = new java.io.ByteArrayOutputStream(512)
    val out = new java.io.ObjectOutputStream(ba)
    out.writeObject(o)
    out.close()
    ba.toByteArray()
  }
  @throws(classOf[java.io.IOException])
  @throws(classOf[ClassNotFoundException])
  def read[A](buffer: Array[Byte]): A = {
    val in =
      new java.io.ObjectInputStream(new java.io.ByteArrayInputStream(buffer))
    in.readObject().asInstanceOf[A]
  }
  def check[A, B](x: A, y: B): Unit = {
    assertEquals(x, y)
    assertEquals(y, x)
  }

  @Test
  def testParallel: Unit = {
    import scala.collection.parallel._
    // UnrolledBuffer
    val ub = new collection.mutable.UnrolledBuffer[String]
    ub ++= List("one", "two")
    val _ub: collection.mutable.UnrolledBuffer[String] = read(write(ub))
    check(ub, _ub)

    // mutable.ParArray
    val pa = mutable.ParArray("abc", "def", "etc")
    val _pa: mutable.ParArray[String] = read(write(pa))
    check(pa, _pa)

    // mutable.ParHashMap
    val mpm = mutable.ParHashMap(1 -> 2, 2 -> 4)
    val _mpm: mutable.ParHashMap[Int, Int] = read(write(mpm))
    check(mpm, _mpm)

    // mutable.ParTrieMap
    val mpc = mutable.ParTrieMap(1 -> 2, 2 -> 4)
    val _mpc: mutable.ParTrieMap[Int, Int] = read(write(mpc))
    check(mpc, _mpc)

    // mutable.ParHashSet
    val mps = mutable.ParHashSet(1, 2, 3)
    val _mps: mutable.ParHashSet[Int] = read(write(mps))
    check(mps, _mps)

    // immutable.ParRange
    val pr1 = immutable.ParRange(0, 4, 1, true)
    val _pr1: immutable.ParRange = read(write(pr1))
    check(pr1, _pr1)

    val pr2 = immutable.ParRange(0, 4, 1, false)
    val _pr2: immutable.ParRange = read(write(pr2))
    check(pr2, _pr2)

    // immutable.ParHashMap
    val ipm = immutable.ParHashMap(5 -> 1, 10 -> 2)
    val _ipm: immutable.ParHashMap[Int, Int] = read(write(ipm))
    check(ipm, _ipm)

    // immutable.ParHashSet
    val ips = immutable.ParHashSet("one", "two")
    val _ips: immutable.ParHashSet[String] = read(write(ips))
    check(ips, _ips)
  }
}
