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
package generic

import scala.collection.parallel.ParMap
import scala.collection.parallel.ParMapLike
import scala.collection.parallel.Combiner

/** A template class for companion objects of `ParMap` and subclasses thereof.
 *  This class extends `TraversableFactory` and provides a set of operations
 *  to create `$Coll` objects.
 *
 *  @define coll parallel map
 *  @define Coll `ParMap`
 *  @define factoryInfo
 *    This object provides a set of operations needed to create `$Coll` values.
 */
abstract class ParMapFactory[CC[X, Y] <: ParMap[X, Y] with ParMapLike[X, Y, CC, CC[X, Y], Sequential[X, Y]], Sequential[X, Y] <: collection.Map[X, Y] with collection.MapOps[X, Y, Sequential, Sequential[X, Y]]]
extends GenericParMapCompanion[CC] {

  // `apply` and `empty` methods were previously inherited from `GenMapFactory`, which
  // has been removed from the Scala library in 2.13

  /** A collection of type $Coll that contains given key/value bindings.
    *  @param elems   the key/value pairs that make up the $coll
    *  @tparam K      the type of the keys
    *  @tparam V      the type of the associated values
    *  @return        a new $coll consisting key/value pairs given by `elems`.
    */
  def apply[K, V](elems: (K, V)*): CC[K, V] = (newCombiner[K, V] ++= elems).result()

  def empty[K, V]: CC[K, V]

  /** The default builder for $Coll objects.
   *  @tparam K      the type of the keys
   *  @tparam V      the type of the associated values
   */
  def newBuilder[K, V]: mutable.Builder[(K, V), CC[K, V]] = newCombiner[K, V]

  /** The default combiner for $Coll objects.
   *  @tparam K     the type of the keys
   *  @tparam V     the type of the associated values
   */
  def newCombiner[K, V]: Combiner[(K, V), CC[K, V]]

  class CanCombineFromMap[FromK, FromV, K, V] extends CanCombineFrom[CC[FromK, FromV], (K, V), CC[K, V]] {
    def apply(from: CC[FromK, FromV]) = from.genericMapCombiner[K, V].asInstanceOf[Combiner[(K, V), CC[K, V]]]
    def apply() = newCombiner[K, V]
  }

}
