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
    def mkPool(name: String) = {
      val parallelism = 1
      val handler: Thread.UncaughtExceptionHandler = null
      val asyncMode = false
      new ForkJoinPool(parallelism, mkFactory(name), handler, asyncMode)
    }

    val one = List(1).par
    val two = List(2).par

    one.tasksupport = new ForkJoinTaskSupport(mkPool("one"))
    two.tasksupport = new ForkJoinTaskSupport(mkPool("two"))

    for (x <- one ; y <- two) assertEquals("two", Thread.currentThread.getName)
  }

  @Test
  def `t152 pass on task support`(): Unit = {
    val myTs = new ExecutionContextTaskSupport()
    val c = List(1).par
    c.tasksupport = myTs
    val r = c.filter(_ != 0).map(_ + 1)
    assertSame(myTs, r.tasksupport)
  }

  // was: Wrong exception: expected scala.collection.parallel.TaskTest$SpecialControl$1 but was java.lang.IllegalArgumentException
  @Test
  def `t10276 exception does not suppress itself when merging`: Unit = {
    import TestSupport._
    import scala.util.control.ControlThrowable
    class SpecialControl extends ControlThrowable("special")
    val SpecialExcept = new SpecialControl

    class Special {
      def add(other: Special): Special = throw SpecialExcept
    }

    def listed(n: Int) = List.fill(n)(new Special)
    val specials = listed(1000).par
    assertThrows[SpecialControl](_ eq SpecialExcept)(specials.reduce(_ add _))
  }
}
object TestSupport {
  import scala.reflect.ClassTag
  import scala.util.control.{ControlThrowable, NonFatal}
  private val Unthrown = new ControlThrowable {}

  def assertThrows[T <: Throwable: ClassTag](checker: T => Boolean)(body: => Any): Unit =
    try {
      body
      throw Unthrown
    } catch {
      case Unthrown => fail("Expression did not throw!")
      case e: T if checker(e) => ()
      case failed: T =>
        val ae = new AssertionError(s"Exception failed check: $failed")
        ae.addSuppressed(failed)
        throw ae
      case NonFatal(other) =>
        val ae = new AssertionError(s"Wrong exception: expected ${implicitly[ClassTag[T]]} but was ${other.getClass.getName}")
        ae.addSuppressed(other)
        throw ae
    }
}
