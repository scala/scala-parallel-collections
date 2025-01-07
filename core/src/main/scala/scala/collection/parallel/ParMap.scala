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

import scala.collection.Map
import scala.collection.generic.ParMapFactory
import scala.collection.generic.GenericParMapTemplate
import scala.collection.generic.GenericParMapCompanion
import scala.collection.generic.CanCombineFrom

/** A template trait for parallel maps.
 *
 *  $sideeffects
 *
 *  @tparam K    the key type of the map
 *  @tparam V    the value type of the map
 */
trait ParMap[K, +V]
extends GenericParMapTemplate[K, V, ParMap]
   with ParIterable[(K, V)]
   with ParMapLike[K, V, ParMap, ParMap[K, V], Map[K, V]]
{
self =>

  def mapCompanion: GenericParMapCompanion[ParMap] = ParMap

  //protected[this] override def newCombiner: Combiner[(K, V), ParMap[K, V]] = ParMap.newCombiner[K, V]

  def empty: ParMap[K, V] = new mutable.ParHashMap[K, V]

  override def stringPrefix = "ParMap"

}



object ParMap extends ParMapFactory[ParMap, collection.Map] {
  def empty[K, V]: ParMap[K, V] = new mutable.ParHashMap[K, V]

  def newCombiner[K, V]: Combiner[(K, V), ParMap[K, V]] = mutable.ParHashMapCombiner[K, V]

  implicit def canBuildFrom[FromK, FromV, K, V]: CanCombineFrom[ParMap[FromK, FromV], (K, V), ParMap[K, V]] = new CanCombineFromMap[FromK, FromV, K, V]

  /** An abstract shell used by { mutable, immutable }.Map but not by collection.Map
   *  because of variance issues.
   */
  abstract class WithDefault[A, +B](underlying: ParMap[A, B], d: A => B) extends ParMap[A, B] {
    def size                        = underlying.size
    def get(key: A)                 = underlying.get(key)
    def splitter                    = underlying.splitter
    override def default(key: A): B = d(key)
  }
}
