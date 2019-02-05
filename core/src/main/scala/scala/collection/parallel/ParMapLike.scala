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

import scala.collection.generic.GenericParMapCompanion
import scala.collection.{IterableOnce, MapOps}
import scala.collection.Map

import scala.annotation.unchecked.uncheckedVariance
import scala.language.higherKinds

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
                 +V,
                 +CC[X, Y] <: ParMap[X, Y],
                 +Repr <: ParMapLike[K, V, ParMap, Repr, Sequential] with ParMap[K, V],
                 +Sequential <: Map[K, V] with MapOps[K, V, Map, Sequential]]
extends ParIterableLike[(K, V), ParIterable, Repr, Sequential]
  with Equals
{
self =>

  // --- Previously inherited from GenMapLike
  def get(key: K): Option[V]

  def canEqual(that: Any): Boolean = true

  /** Compares two maps structurally; i.e., checks if all mappings
    *  contained in this map are also contained in the other map,
    *  and vice versa.
    *
    *  @param that the other map
    *  @return     `true` if both maps contain exactly the
    *              same mappings, `false` otherwise.
    */
  override def equals(that: Any): Boolean = that match {
    case that: ParMap[b, _] =>
      (this eq that) ||
        (that canEqual this) &&
          (this.size == that.size) && {
          try {
            this forall {
              case (k, v) => that.get(k.asInstanceOf[b]) match {
                case Some(`v`) =>
                  true
                case _ => false
              }
            }
          } catch {
            case ex: ClassCastException => false
          }}
    case _ =>
      false
  }

  // This hash code must be symmetric in the contents but ought not
  // collide trivially.
  override def hashCode(): Int = scala.util.hashing.MurmurHash3.unorderedHash(this, "ParMap".hashCode)

  def +[V1 >: V](kv: (K, V1)): CC[K, V1]
  def updated [V1 >: V](key: K, value: V1): CC[K, V1] = this + ((key, value))
  def - (key: K): Repr
  // ---

  def mapCompanion: GenericParMapCompanion[CC]

  def default(key: K): V = throw new NoSuchElementException("key not found: " + key)

  def empty: Repr

  def apply(key: K) = get(key) match {
    case Some(v) => v
    case None => default(key)
  }

  def getOrElse[U >: V](key: K, default: => U): U = get(key) match {
    case Some(v) => v
    case None => default
  }

  def contains(key: K): Boolean = get(key).isDefined

  def isDefinedAt(key: K): Boolean = contains(key)

  private[this] def keysIterator(s: IterableSplitter[(K, V)] @uncheckedVariance): IterableSplitter[K] =
    new IterableSplitter[K] {
      i =>
      val iter = s
      def hasNext = iter.hasNext
      def next() = iter.next()._1
      def split = {
        val ss = iter.split.map(keysIterator(_))
        ss.foreach { _.signalDelegate = i.signalDelegate }
        ss
      }
      def remaining = iter.remaining
      def dup = keysIterator(iter.dup)
    }

  def keysIterator: IterableSplitter[K] = keysIterator(splitter)

  private[this] def valuesIterator(s: IterableSplitter[(K, V)] @uncheckedVariance): IterableSplitter[V] =
    new IterableSplitter[V] {
      i =>
      val iter = s
      def hasNext = iter.hasNext
      def next() = iter.next()._2
      def split = {
        val ss = iter.split.map(valuesIterator(_))
        ss.foreach { _.signalDelegate = i.signalDelegate }
        ss
      }
      def remaining = iter.remaining
      def dup = valuesIterator(iter.dup)
    }

  def valuesIterator: IterableSplitter[V] = valuesIterator(splitter)

  protected class DefaultKeySet extends ParSet[K] {
    def contains(key : K) = self.contains(key)
    def splitter = keysIterator(self.splitter)
    def + (elem: K): ParSet[K] =
      ParSet[K]() ++ this + elem // !!! concrete overrides abstract problem
    def - (elem: K): ParSet[K] =
      ParSet[K]() ++ this - elem // !!! concrete overrides abstract problem
    def size = self.size
    def knownSize = self.knownSize
    override def foreach[U](f: K => U) = for ((k, v) <- self) f(k)
    override def seq = self.seq.keySet
  }

  protected class DefaultValuesIterable extends ParIterable[V] {
    def splitter = valuesIterator(self.splitter)
    def size = self.size
    def knownSize = self.knownSize
    override def foreach[U](f: V => U) = for ((k, v) <- self) f(v)
    def seq = self.seq.values
  }

  def keySet: ParSet[K] = new DefaultKeySet

  def keys: ParIterable[K] = keySet

  def values: ParIterable[V] = new DefaultValuesIterable

  def filterKeys(p: K => Boolean): ParMap[K, V] = new ParMap[K, V] {
    lazy val filtered = self.filter(kv => p(kv._1))
    override def foreach[U](f: ((K, V)) => U): Unit = for (kv <- self) if (p(kv._1)) f(kv)
    def splitter = filtered.splitter
    override def contains(key: K) = self.contains(key) && p(key)
    def get(key: K) = if (!p(key)) None else self.get(key)
    def seq = self.seq.view.filterKeys(p).to(Map)
    def size = filtered.size
    def knownSize = filtered.knownSize
    def + [U >: V](kv: (K, U)): ParMap[K, U] = ParMap[K, U]() ++ this + kv
    def - (key: K): ParMap[K, V] = ParMap[K, V]() ++ this - key
  }

  def mapValues[S](f: V => S): ParMap[K, S] = new ParMap[K, S] {
    override def foreach[U](g: ((K, S)) => U): Unit = for ((k, v) <- self) g((k, f(v)))
    def splitter = self.splitter.map(kv => (kv._1, f(kv._2)))
    def size = self.size
    def knownSize = self.knownSize
    override def contains(key: K) = self.contains(key)
    def get(key: K) = self.get(key).map(f)
    def seq = self.seq.view.mapValues(f).to(Map)
    def + [U >: S](kv: (K, U)): ParMap[K, U] = ParMap[K, U]() ++ this + kv
    def - (key: K): ParMap[K, S] = ParMap[K, S]() ++ this - key
  }

  /** Builds a new map by applying a function to all elements of this $coll.
    *
    *  @param f      the function to apply to each element.
    *  @return       a new $coll resulting from applying the given function
    *                `f` to each element of this $coll and collecting the results.
    */
  def map[K2, V2](f: ((K, V)) => (K2, V2)): CC[K2, V2] =
    tasksupport.executeAndWaitResult(new Map[(K2, V2), CC[K2, V2]](
      f,
      combinerFactory(() => mapCompanion.newCombiner[K2, V2]),
      splitter
    ) mapResult { _.resultWithTaskSupport })

  /** Builds a new collection by applying a partial function to all elements of this $coll
    *  on which the function is defined.
    *
    *  @param pf     the partial function which filters and maps the $coll.
    *  @tparam K2    the key type of the returned $coll.
    *  @tparam V2    the value type of the returned $coll.
    *  @return       a new $coll resulting from applying the given partial function
    *                `pf` to each element on which it is defined and collecting the results.
    *                The order of the elements is preserved.
    */
  def collect[K2, V2](pf: PartialFunction[(K, V), (K2, V2)]): CC[K2, V2] =
    tasksupport.executeAndWaitResult(new Collect[(K2, V2), CC[K2, V2]](
      pf,
      combinerFactory(() => mapCompanion.newCombiner[K2, V2]),
      splitter
    ) mapResult { _.resultWithTaskSupport })

  /** Builds a new map by applying a function to all elements of this $coll
    *  and using the elements of the resulting collections.
    *
    *  @param f      the function to apply to each element.
    *  @return       a new $coll resulting from applying the given collection-valued function
    *                `f` to each element of this $coll and concatenating the results.
    */
  def flatMap[K2, V2](f: ((K, V)) => IterableOnce[(K2, V2)]): CC[K2, V2] =
    tasksupport.executeAndWaitResult(new FlatMap[(K2, V2), CC[K2, V2]](
      f,
      combinerFactory(() => mapCompanion.newCombiner[K2, V2]),
      splitter
    ) mapResult { _.resultWithTaskSupport })

  /** Returns a new $coll containing the elements from the left hand operand followed by the elements from the
    *  right hand operand. The element type of the $coll is the most specific superclass encompassing
    *  the element types of the two operands.
    *
    *  @param that   the collection or iterator to append.
    *  @return       a new $coll which contains all elements
    *                of this $coll followed by all elements of `suffix`.
    */
  def concat[V2 >: V](that: collection.IterableOnce[(K, V2)]): CC[K, V2] =  that match {
    case other: ParIterable[(K, V2)] =>
      // println("case both are parallel")
      val cfactory = combinerFactory(() => mapCompanion.newCombiner[K, V2])
      val copythis = new Copy(cfactory, splitter)
      val copythat = wrap {
        val othtask = new other.Copy(cfactory, other.splitter)
        tasksupport.executeAndWaitResult(othtask)
      }
      val task = (copythis parallel copythat) { _ combine _ } mapResult {
        _.resultWithTaskSupport
      }
      tasksupport.executeAndWaitResult(task)
    case _ =>
      // println("case parallel builder, `that` not parallel")
      val copythis = new Copy(combinerFactory(() => mapCompanion.newCombiner[K, V2]), splitter)
      val copythat = wrap {
        val cb = mapCompanion.newCombiner[K, V2]
        cb ++= that
        cb
      }
      tasksupport.executeAndWaitResult((copythis parallel copythat) { _ combine _ } mapResult { _.resultWithTaskSupport })
  }

  /** Alias for `concat` */
  @`inline` final def ++ [V2 >: V](xs: collection.IterableOnce[(K, V2)]): CC[K, V2] = concat(xs)

  // note - should not override toMap (could be mutable)
}
