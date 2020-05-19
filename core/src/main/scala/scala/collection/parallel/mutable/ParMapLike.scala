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
package collection.parallel
package mutable

import scala.collection.mutable.Cloneable

/** A template trait for mutable parallel maps. This trait is to be mixed in
 *  with concrete parallel maps to override the representation type.
 *
 *  $sideeffects
 *
 *  @tparam K    the key type of the map
 *  @tparam V    the value type of the map
 *  @define Coll `ParMap`
 *  @define coll parallel map
 *
 *  @author Aleksandar Prokopec
 *  @since 2.9
 */
trait ParMapLike[K,
                 V,
                 +CC[X, Y] <: ParMap[X, Y],
                 +Repr <: ParMapLike[K, V, ParMap, Repr, Sequential] with ParMap[K, V],
                 +Sequential <: scala.collection.mutable.Map[K, V] with scala.collection.mutable.MapOps[K, V, scala.collection.mutable.Map, Sequential]]
extends scala.collection.parallel.ParIterableLike[(K, V), ParIterable, Repr, Sequential]
   with scala.collection.parallel.ParMapLike[K, V, CC, Repr, Sequential]
   with scala.collection.mutable.Growable[(K, V)]
   with scala.collection.mutable.Shrinkable[K]
   with Cloneable[Repr]
{
  // note: should not override toMap

  override def knownSize: Int = -1

  def put(key: K, value: V): Option[V]

  def +[U >: V](kv: (K, U)) = this.clone().asInstanceOf[CC[K, U]] += kv

  def -(key: K) = this.clone() -= key

  def clear(): Unit

  override def clone(): Repr = empty ++= this
}
