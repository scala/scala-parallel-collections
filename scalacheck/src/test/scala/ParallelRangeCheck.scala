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
package immutable

import org.scalacheck._
import org.scalacheck.Gen
import org.scalacheck.Gen._
import org.scalacheck.Prop._
import org.scalacheck.Properties
import org.scalacheck.Arbitrary._

import scala.collection._
import scala.collection.parallel.ops._

abstract class ParallelRangeCheck(val tasksupport: TaskSupport) extends ParallelSeqCheck[Int]("ParallelRange[Int]") with ops.IntSeqOperators {
  // ForkJoinTasks.defaultForkJoinPool.setMaximumPoolSize(Runtime.getRuntime.availableProcessors * 2)
  // ForkJoinTasks.defaultForkJoinPool.setParallelism(Runtime.getRuntime.availableProcessors * 2)

  type CollType = collection.parallel.ParSeq[Int]

  def hasStrictOrder = true

  def ofSize(vals: Seq[Gen[Int]], sz: Int) = throw new UnsupportedOperationException

  override def instances(vals: Seq[Gen[Int]]): Gen[Seq[Int]] = sized { start =>
    sized { end =>
      sized { step =>
        Range(start, end, if (step != 0) step else 1)
      }
    }
  }

  def fromSeq(a: Seq[Int]) = a match {
    case r: Range =>
      val pr = ParRange(r.start, r.end, r.step, false)
      pr.tasksupport = tasksupport
      pr
    case _ =>
      val pa = new parallel.mutable.ParArray[Int](a.length)
      pa.tasksupport = tasksupport
      for (i <- 0 until a.length) pa(i) = a(i)
      pa
  }

  def values = Seq(choose(-100, 100))

}
