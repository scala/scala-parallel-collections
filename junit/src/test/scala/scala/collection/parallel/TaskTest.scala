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

package scala.collection.parallel

import org.junit.Test
import org.junit.Assert._

import java.util.concurrent.{ForkJoinPool, ForkJoinWorkerThread}, ForkJoinPool._

import CollectionConverters._

class TaskTest {
  @Test
  def `t10577 task executes on foreign pool`(): Unit = {
    def mkFactory(name: String) = new ForkJoinWorkerThreadFactory {
      override def newThread(pool: ForkJoinPool) = {
        val t = new ForkJoinWorkerThread(pool) {}
        t.setName(name)
        t
      }
    }
    def mkPool(name: String) = new ForkJoinPool(1, mkFactory(name), null, false)

    val one = List(1).par
    val two = List(2).par

    one.tasksupport = new ForkJoinTaskSupport(mkPool("one"))
    two.tasksupport = new ForkJoinTaskSupport(mkPool("two"))

    for (x <- one ; y <- two) assertEquals("two", Thread.currentThread.getName)
  }
}
