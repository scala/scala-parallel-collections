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
package collection.parallel

import scala.collection.generic._
import scala.collection.parallel.mutable.ParArrayCombiner

/** A template trait for parallel iterable collections.
 *
 *  $paralleliterableinfo
 *
 *  $sideeffects
 *
 *  @tparam T    the element type of the collection
 */
trait ParIterable[+T]
  extends GenericParTemplate[T, ParIterable]
    with ParIterableLike[T, ParIterable, ParIterable[T], Iterable[T]] {
  def companion: GenericParCompanion[ParIterable] = ParIterable

  def stringPrefix = "ParIterable"
}

/** $factoryInfo
 */
object ParIterable extends ParFactory[ParIterable] {
  implicit def canBuildFrom[T, S]: CanCombineFrom[ParIterable[S], T, ParIterable[T]] = new GenericCanCombineFrom[S, T]

  def newBuilder[T]: Combiner[T, ParIterable[T]] = ParArrayCombiner[T]()

  def newCombiner[T]: Combiner[T, ParIterable[T]] = ParArrayCombiner[T]()
}

