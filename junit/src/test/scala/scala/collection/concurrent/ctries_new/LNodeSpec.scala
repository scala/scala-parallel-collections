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

package scala.collection.concurrent.ctries_new

import collection.concurrent.TrieMap
import org.junit.Test

class LNodeSpec extends Spec {

  val initsz = 1500
  val secondsz = 1750

  @Test
  def test(): Unit = {
    "accept elements with the same hash codes" in {
      val ct = new TrieMap[DumbHash, Int]
      for (i <- 0 until initsz) ct.update(new DumbHash(i), i)
    }

    "lookup elements with the same hash codes" in {
      val ct = new TrieMap[DumbHash, Int]
      for (i <- 0 until initsz) ct.update(new DumbHash(i), i)
      for (i <- 0 until initsz) assert(ct.get(new DumbHash(i)) == Some(i))
      for (i <- initsz until secondsz) assert(ct.get(new DumbHash(i)) == None)
    }

    "remove elements with the same hash codes" in {
      val ct = new TrieMap[DumbHash, Int]
      for (i <- 0 until initsz) ct.update(new DumbHash(i), i)
      for (i <- 0 until initsz) {
        val remelem = ct.remove(new DumbHash(i))
        assert(remelem == Some(i), "removing " + i + " yields " + remelem)
      }
      for (i <- 0 until initsz) assert(ct.get(new DumbHash(i)) == None)
    }

    "put elements with the same hash codes if absent" in {
      val ct = new TrieMap[DumbHash, Int]
      for (i <- 0 until initsz) ct.put(new DumbHash(i), i)
      for (i <- 0 until initsz) assert(ct.getOrElse(new DumbHash(i), null) == i)
      for (i <- 0 until initsz) assert(ct.putIfAbsent(new DumbHash(i), i) == Some(i))
      for (i <- initsz until secondsz) assert(ct.putIfAbsent(new DumbHash(i), i) == None)
      for (i <- initsz until secondsz) assert(ct.getOrElse(new DumbHash(i), null) == i)
    }

    "replace elements with the same hash codes" in {
      val ct = new TrieMap[DumbHash, Int]
      for (i <- 0 until initsz) assert(ct.put(new DumbHash(i), i) == None)
      for (i <- 0 until initsz) assert(ct.getOrElse(new DumbHash(i), null) == i)
      for (i <- 0 until initsz) assert(ct.replace(new DumbHash(i), -i) == Some(i))
      for (i <- 0 until initsz) assert(ct.getOrElse(new DumbHash(i), null) == -i)
      for (i <- 0 until initsz) assert(ct.replace(new DumbHash(i), -i, i) == true)
    }

    "remove elements with the same hash codes if mapped to a specific value" in {
      val ct = new TrieMap[DumbHash, Int]
      for (i <- 0 until initsz) assert(ct.put(new DumbHash(i), i) == None)
      for (i <- 0 until initsz) assert(ct.remove(new DumbHash(i), i) == true)
    }

  }

}
