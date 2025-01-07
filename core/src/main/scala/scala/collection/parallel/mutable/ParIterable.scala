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
package parallel.mutable

import scala.collection.generic._
import scala.collection.parallel.{ ParIterableLike, Combiner }

/** A template trait for mutable parallel iterable collections.
 *
 *  $paralleliterableinfo
 *
 *  $sideeffects
 *
 *  @tparam T    the element type of the collection
 */
trait ParIterable[T] extends scala.collection.parallel.ParIterable[T]
                        with GenericParTemplate[T, ParIterable]
                        with ParIterableLike[T, ParIterable, ParIterable[T], Iterable[T]] {
  override def companion: GenericParCompanion[ParIterable] = ParIterable
  //protected[this] override def newBuilder = ParIterable.newBuilder[T]

  // if `mutable.ParIterableLike` is introduced, please move these methods there
  override def toIterable: ParIterable[T] = this

  override def toSeq: ParSeq[T] = toParCollection[T, ParSeq[T]](() => ParSeq.newCombiner[T])

  def seq: scala.collection.mutable.Iterable[T]
}

/** $factoryInfo
 */
object ParIterable extends ParFactory[ParIterable] {
  implicit def canBuildFrom[S, T]: CanCombineFrom[ParIterable[S], T, ParIterable[T]] = new GenericCanCombineFrom[S, T]

  def newBuilder[T]: Combiner[T, ParIterable[T]] = ParArrayCombiner[T]()
  def newCombiner[T]: Combiner[T, ParIterable[T]] = ParArrayCombiner[T]()
}
