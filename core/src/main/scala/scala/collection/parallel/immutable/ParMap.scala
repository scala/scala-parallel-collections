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
package collection
package parallel.immutable

import scala.collection.generic.ParMapFactory
import scala.collection.generic.GenericParMapTemplate
import scala.collection.generic.GenericParMapCompanion
import scala.collection.generic.CanCombineFrom
import scala.collection.parallel.Combiner

/** A template trait for immutable parallel maps.
 *
 *  $sideeffects
 *
 *  @tparam K    the key type of the map
 *  @tparam V    the value type of the map
 *
 */
trait ParMap[K, +V]
extends GenericParMapTemplate[K, V, ParMap]
   with parallel.ParMap[K, V]
   with ParIterable[(K, V)]
   with ParMapLike[K, V, ParMap, ParMap[K, V], scala.collection.immutable.Map[K, V]]
{
self =>

  override def mapCompanion: GenericParMapCompanion[ParMap] = ParMap

  override def empty: ParMap[K, V] = new ParHashMap[K, V]

  override def stringPrefix = "ParMap"

  /** The same map with a given default function.
   *  Note: `get`, `contains`, `iterator`, `keys`, etc are not affected by `withDefault`.
   *
   *  Invoking transformer methods (e.g. `map`) will not preserve the default value.
   *
   *  @param d     the function mapping keys to values, used for non-present keys
   *  @return      a wrapper of the map with a default value
   */
  def withDefault[U >: V](d: K => U): scala.collection.parallel.immutable.ParMap[K, U] = new ParMap.WithDefault[K, U](this, d)

  /** The same map with a given default value.
   *
   *  Invoking transformer methods (e.g. `map`) will not preserve the default value.
   *
   *  @param d     default value used for non-present keys
   *  @return      a wrapper of the map with a default value
   */
  def withDefaultValue[U >: V](d: U): scala.collection.parallel.immutable.ParMap[K, U] = new ParMap.WithDefault[K, U](this, x => d)

}

trait ParMapLike[
    K,
    +V,
    +CC[X, Y] <: ParMap[X, Y],
    +Repr <: ParMapLike[K, V, ParMap, Repr, Sequential] with ParMap[K, V],
    +Sequential <: Map[K, V] with MapOps[K, V, Map, Sequential]]
  extends parallel.ParMapLike[K, V, CC, Repr, Sequential]
    with parallel.ParIterableLike[(K, V), ParIterable, Repr, Sequential] {

  def mapCompanion: GenericParMapCompanion[CC]

  def empty: Repr

  override def toMap[P, Q](implicit ev: (K, V) <:< (P, Q)): ParMap[P, Q] = this.asInstanceOf[ParMap[P, Q]]

  override def updated [U >: V](key: K, value: U): CC[K, U] = this + ((key, value))

  def + [U >: V](kv: (K, U)): CC[K, U]

  def - (key: K): Repr

}



object ParMap extends ParMapFactory[ParMap] {
  def empty[K, V]: ParMap[K, V] = new ParHashMap[K, V]

  def newCombiner[K, V]: Combiner[(K, V), ParMap[K, V]] = HashMapCombiner[K, V]

  implicit def canBuildFrom[K, V]: CanCombineFrom[Coll, (K, V), ParMap[K, V]] = new CanCombineFromMap[K, V]

  class WithDefault[K, +V](underlying: ParMap[K, V], d: K => V)
  extends scala.collection.parallel.ParMap.WithDefault[K, V](underlying, d) with ParMap[K, V] {
    override def knownSize = underlying.knownSize
    override def empty = new WithDefault(underlying.empty, d)
    override def updated[U >: V](key: K, value: U): WithDefault[K, U] = new WithDefault[K, U](underlying.updated[U](key, value), d)
    override def + [U >: V](kv: (K, U)): WithDefault[K, U] = updated(kv._1, kv._2)
    override def - (key: K): WithDefault[K, V] = new WithDefault(underlying - key, d)
    override def withDefault[U >: V](d: K => U): ParMap[K, U] = new WithDefault[K, U](underlying, d)
    override def withDefaultValue[U >: V](d: U): ParMap[K, U] = new WithDefault[K, U](underlying, x => d)
    override def seq = underlying.seq.withDefault(d)
  }

}
