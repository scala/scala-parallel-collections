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
package collection
package parallel.immutable

import scala.collection.generic.{CanCombineFrom, GenericParCompanion, GenericParTemplate, ParFactory}
import scala.collection.parallel.ParSeqLike
import scala.collection.parallel.Combiner

/** An immutable variant of `ParSeq`.
 *
 *  @define Coll `mutable.ParSeq`
 *  @define coll mutable parallel sequence
 */
trait ParSeq[+T]
extends scala.collection.parallel.ParSeq[T]
   with ParIterable[T]
   with GenericParTemplate[T, ParSeq]
   with ParSeqLike[T, ParSeq, ParSeq[T], scala.collection.immutable.Seq[T]]
{
  override def companion: GenericParCompanion[ParSeq] = ParSeq
  override def toSeq: ParSeq[T] = this
}

/** $factoryInfo
 *  @define Coll `mutable.ParSeq`
 *  @define coll mutable parallel sequence
 */
object ParSeq extends ParFactory[ParSeq] {
  implicit def canBuildFrom[S, T]: CanCombineFrom[ParSeq[S], T, ParSeq[T]] = new GenericCanCombineFrom[S, T]

  def newBuilder[T]: Combiner[T, ParSeq[T]] = ParVector.newBuilder[T]
  def newCombiner[T]: Combiner[T, ParSeq[T]] = ParVector.newCombiner[T]
}
