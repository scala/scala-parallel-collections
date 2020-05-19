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

package scala
package collection.parallel.mutable

import scala.collection.generic._
import scala.collection.parallel.Combiner

/** A mutable variant of `ParSet`.
 */
trait ParSet[T]
extends ParIterable[T]
   with scala.collection.parallel.ParSet[T]
   with GenericParTemplate[T, ParSet]
   with ParSetLike[T, ParSet, ParSet[T], scala.collection.mutable.Set[T]]
{
self =>
  override def knownSize: Int = -1
  override def companion: GenericParCompanion[ParSet] = ParSet
  override def empty: ParSet[T] = ParHashSet()
  def seq: scala.collection.mutable.Set[T]
}


/** $factoryInfo
 *  @define Coll `mutable.ParSet`
 *  @define coll mutable parallel set
 */
object ParSet extends ParSetFactory[ParSet] {
  implicit def canBuildFrom[T]: CanCombineFrom[ParSet[_], T, ParSet[T]] = new GenericCanCombineFrom[T]

  override def newBuilder[T]: Combiner[T, ParSet[T]] = ParHashSet.newBuilder

  override def newCombiner[T]: Combiner[T, ParSet[T]] = ParHashSet.newCombiner
}
