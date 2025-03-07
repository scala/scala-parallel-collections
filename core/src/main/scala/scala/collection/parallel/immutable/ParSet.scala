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

import scala.collection.generic._
import scala.collection.parallel.ParSetLike
import scala.collection.parallel.Combiner

/** An immutable variant of `ParSet`.
 *
 *  @define Coll `mutable.ParSet`
 *  @define coll mutable parallel set
 */
trait ParSet[T]
extends GenericParTemplate[T, ParSet]
   with parallel.ParSet[T]
   with ParIterable[T]
   with ParSetLike[T, ParSet, ParSet[T], scala.collection.immutable.Set[T]]
{
self =>
  override def empty: ParSet[T] = ParHashSet[T]()

  override def companion: GenericParCompanion[ParSet] = ParSet

  override def stringPrefix = "ParSet"

  // ok, because this could only violate `apply` and we can live with that
  override def toSet[U >: T]: ParSet[U] = this.asInstanceOf[ParSet[U]]
}

/** $factoryInfo
 *  @define Coll `mutable.ParSet`
 *  @define coll mutable parallel set
 */
object ParSet extends ParSetFactory[ParSet] {
  def newCombiner[T]: Combiner[T, ParSet[T]] = HashSetCombiner[T]

  implicit def canBuildFrom[S, T]: CanCombineFrom[ParSet[S], T, ParSet[T]] = new GenericCanCombineFrom[S, T]
}
