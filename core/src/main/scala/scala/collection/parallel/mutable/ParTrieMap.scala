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
package collection.parallel.mutable

import scala.collection.generic._
import scala.collection.parallel.Combiner
import scala.collection.parallel.IterableSplitter
import scala.collection.parallel.Task
import scala.collection.concurrent.BasicNode
import scala.collection.concurrent.TNode
import scala.collection.concurrent.LNode
import scala.collection.concurrent.CNode
import scala.collection.concurrent.SNode
import scala.collection.concurrent.INode
import scala.collection.concurrent.TrieMap
import scala.collection.concurrent.TrieMapIterator

/** Parallel TrieMap collection.
 *
 *  It has its bulk operations parallelized, but uses the snapshot operation
 *  to create the splitter. This means that parallel bulk operations can be
 *  called concurrently with the modifications.
 *
 *  @see  [[http://docs.scala-lang.org/overviews/parallel-collections/concrete-parallel-collections.html#parallel_concurrent_tries Scala's Parallel Collections Library overview]]
 *  section on `ParTrieMap` for more information.
 */
final class ParTrieMap[K, V] private[collection] (private val ctrie: TrieMap[K, V])
extends ParMap[K, V]
   with GenericParMapTemplate[K, V, ParTrieMap]
   with ParMapLike[K, V, ParTrieMap, ParTrieMap[K, V], TrieMap[K, V]]
   with ParTrieMapCombiner[K, V]
   with Serializable
{
  def this() = this(new TrieMap)

  override def mapCompanion: GenericParMapCompanion[ParTrieMap] = ParTrieMap

  override def empty: ParTrieMap[K, V] = ParTrieMap.empty

  protected[this] override def newCombiner = ParTrieMap.newCombiner

  override def seq = ctrie

  def splitter = new ParTrieMapSplitter(0, ctrie.readOnlySnapshot().asInstanceOf[TrieMap[K, V]], mustInit = true)

  override def clear() = ctrie.clear()

  def result() = this

  def get(key: K): Option[V] = ctrie.get(key)

  def put(key: K, value: V): Option[V] = ctrie.put(key, value)

  def update(key: K, value: V): Unit = ctrie.update(key, value)

  def remove(key: K): Option[V] = ctrie.remove(key)

  def addOne(kv: (K, V)): this.type = {
    ctrie.+=(kv)
    this
  }

  def subtractOne(key: K): this.type = {
    ctrie.-=(key)
    this
  }

  override def size = {
    val in = ctrie.readRoot()
    val r = in.gcasRead(ctrie)
    (r: @unchecked) match {
      case tn: TNode[?, ?] => tn.cachedSize(ctrie)
      case ln: LNode[?, ?] => ln.cachedSize(ctrie)
      case cn: CNode[?, ?] =>
        tasksupport.executeAndWaitResult(new Size(0, cn.array.length, cn.array))
        cn.cachedSize(ctrie)
    }
  }

  override def knownSize = -1

  override def stringPrefix = "ParTrieMap"

  /* tasks */

  /** Computes TrieMap size in parallel. */
  class Size(offset: Int, howmany: Int, array: Array[BasicNode]) extends Task[Int, Size] {
    var result = -1
    def leaf(prev: Option[Int]) = {
      var sz = 0
      var i = offset
      val until = offset + howmany
      while (i < until) {
        (array(i): @unchecked) match {
          case sn: SNode[?, ?] => sz += 1
          case in: INode[K @unchecked, V @unchecked] => sz += in.cachedSize(ctrie)
        }
        i += 1
      }
      result = sz
    }
    def split = {
      val fp = howmany / 2
      Seq(new Size(offset, fp, array), new Size(offset + fp, howmany - fp, array))
    }
    def shouldSplitFurther = howmany > 1
    override def merge(that: Size) = result = result + that.result
  }
}

private[collection] class ParTrieMapSplitter[K, V](lev: Int, ct: TrieMap[K, V], mustInit: Boolean)
extends TrieMapIterator[K, V](lev, ct, mustInit)
   with IterableSplitter[(K, V)]
{
  // only evaluated if `remaining` is invoked (which is not used by most tasks)
  lazy val totalsize = new ParTrieMap(ct).size
  var iterated = 0

  protected override def newIterator(_lev: Int, _ct: TrieMap[K, V], _mustInit: Boolean): ParTrieMapSplitter[K, V] = new ParTrieMapSplitter[K, V](_lev, _ct, _mustInit)

  override def shouldSplitFurther[S](coll: scala.collection.parallel.ParIterable[S], parallelismLevel: Int) = {
    val maxsplits = 3 + Integer.highestOneBit(parallelismLevel)
    level < maxsplits
  }

  def dup = {
    val it = newIterator(0, ct, _mustInit = false)
    dupTo(it)
    it.iterated = this.iterated
    it
  }

  override def next() = {
    iterated += 1
    super.next()
  }

  def split: Seq[IterableSplitter[(K, V)]] = subdivide().asInstanceOf[Seq[IterableSplitter[(K, V)]]]

  override def isRemainingCheap = false

  def remaining: Int = totalsize - iterated
}

/** Only used within the `ParTrieMap`. */
private[mutable] trait ParTrieMapCombiner[K, V] extends Combiner[(K, V), ParTrieMap[K, V]] {

  def combine[N <: (K, V), NewTo >: ParTrieMap[K, V]](other: Combiner[N, NewTo]): Combiner[N, NewTo] =
    if (this eq other) this
    else throw new UnsupportedOperationException("This shouldn't have been called in the first place.")

  override def canBeShared = true
}

object ParTrieMap extends ParMapFactory[ParTrieMap, TrieMap] {
  def empty[K, V]: ParTrieMap[K, V] = new ParTrieMap[K, V]
  def newCombiner[K, V]: Combiner[(K, V), ParTrieMap[K, V]] = new ParTrieMap[K, V]

  implicit def canBuildFrom[FromK, FromV, K, V]: CanCombineFrom[ParTrieMap[FromK, FromV], (K, V), ParTrieMap[K, V]] = new CanCombineFromMap[FromK, FromV, K, V]
}
