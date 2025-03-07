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
package mutable

import scala.collection.generic._
import scala.collection.parallel.mutable.ParHashMap.DefaultEntry
import scala.collection.mutable.HashEntry
import scala.collection.mutable.HashTable
import scala.collection.mutable.UnrolledBuffer
import scala.collection.parallel.Task

/** A parallel hash map.
 *
 *  `ParHashMap` is a parallel map which internally keeps elements within a hash table.
 *  It uses chaining to resolve collisions.
 *
 *  @tparam K        type of the keys in the parallel hash map
 *  @tparam V        type of the values in the parallel hash map
 *
 *  @define Coll `ParHashMap`
 *  @define coll parallel hash map
 *
 *  @see  [[http://docs.scala-lang.org/overviews/parallel-collections/concrete-parallel-collections.html#parallel_hash_tables Scala's Parallel Collections Library overview]]
 *  section on Parallel Hash Tables for more information.
 */
@SerialVersionUID(3L)
class ParHashMap[K, V] private[collection] (contents: ParHashTable.Contents[K, DefaultEntry[K, V]])
extends ParMap[K, V]
   with GenericParMapTemplate[K, V, ParHashMap]
   with ParMapLike[K, V, ParHashMap, ParHashMap[K, V], scala.collection.mutable.HashMap[K, V]]
   with ParHashTable[K, V, DefaultEntry[K, V]]
   with Serializable
{
self =>
  initWithContents(contents)

  type Entry = DefaultEntry[K, V]

  def this() = this(null)

  override def mapCompanion: GenericParMapCompanion[ParHashMap] = ParHashMap

  override def empty: ParHashMap[K, V] = new ParHashMap[K, V]

  protected[this] override def newCombiner = ParHashMapCombiner[K, V]

  // TODO Redesign ParHashMap so that it can be converted to a mutable.HashMap in constant time
  def seq = scala.collection.mutable.HashMap.from(this)

  def splitter = new ParHashMapIterator(1, table.length, size, table(0).asInstanceOf[DefaultEntry[K, V]])

  override def knownSize = tableSize

  def clear() = clearTable()

  def get(key: K): Option[V] = {
    val e = findEntry(key)
    if (e eq null) None
    else Some(e.value)
  }

  def put(key: K, value: V): Option[V] = {
    val e = findOrAddEntry(key, value)
    if (e eq null) None
    else { val v = e.value; e.value = value; Some(v) }
  }

  def update(key: K, value: V): Unit = put(key, value)

  def remove(key: K): Option[V] = {
    val e = removeEntry(key)
    if (e ne null) Some(e.value)
    else None
  }

  def addOne(kv: (K, V)): this.type = {
    val e = findOrAddEntry(kv._1, kv._2)
    if (e ne null) e.value = kv._2
    this
  }

  def subtractOne(key: K): this.type = { removeEntry(key); this }

  override def stringPrefix = "ParHashMap"

  class ParHashMapIterator(start: Int, untilIdx: Int, totalSize: Int, e: DefaultEntry[K, V])
  extends EntryIterator[(K, V), ParHashMapIterator](start, untilIdx, totalSize, e) {
    def entry2item(entry: DefaultEntry[K, V]) = (entry.key, entry.value)

    def newIterator(idxFrom: Int, idxUntil: Int, totalSz: Int, es: DefaultEntry[K, V]) =
      new ParHashMapIterator(idxFrom, idxUntil, totalSz, es)
  }

  def createNewEntry(key: K, value: V): Entry = {
    new Entry(key, value)
  }

  private def writeObject(out: java.io.ObjectOutputStream): Unit = {
    serializeTo(out, { entry =>
      out.writeObject(entry.key)
      out.writeObject(entry.value)
    })
  }

  private def readObject(in: java.io.ObjectInputStream): Unit = {
    init(in, createNewEntry(in.readObject().asInstanceOf[K], in.readObject().asInstanceOf[V]))
  }

  private[parallel] override def brokenInvariants = {
    // bucket by bucket, count elements
    val buckets = for (i <- 0 until (table.length / sizeMapBucketSize)) yield checkBucket(i)

    // check if each element is in the position corresponding to its key
    val elems = for (i <- 0 until table.length) yield checkEntry(i)

    buckets.flatMap(x => x) ++ elems.flatMap(x => x)
  }

  private def checkBucket(i: Int) = {
    def count(e: HashEntry[K, DefaultEntry[K, V]]): Int = if (e eq null) 0 else 1 + count(e.next)
    val expected = sizemap(i)
    val found = ((i * sizeMapBucketSize) until ((i + 1) * sizeMapBucketSize)).foldLeft(0) {
      (acc, c) => acc + count(table(c))
    }
    if (found != expected) List("Found " + found + " elements, while sizemap showed " + expected)
    else Nil
  }

  private def checkEntry(i: Int) = {
    def check(e: HashEntry[K, DefaultEntry[K, V]]): List[String] = if (e eq null) Nil else
      if (index(elemHashCode(e.key)) == i) check(e.next)
      else ("Element " + e.key + " at " + i + " with " + elemHashCode(e.key) + " maps to " + index(elemHashCode(e.key))) :: check(e.next)
    check(table(i))
  }
}

/** $factoryInfo
 *  @define Coll `mutable.ParHashMap`
 *  @define coll parallel hash map
 */
object ParHashMap extends ParMapFactory[ParHashMap, scala.collection.mutable.HashMap] {
  var iters = 0

  def empty[K, V]: ParHashMap[K, V] = new ParHashMap[K, V]

  def newCombiner[K, V]: Combiner[(K, V), ParHashMap[K, V]] = ParHashMapCombiner.apply[K, V]

  implicit def canBuildFrom[FromK, FromV, K, V]: CanCombineFrom[ParHashMap[FromK, FromV], (K, V), ParHashMap[K, V]] = new CanCombineFromMap[FromK, FromV, K, V]

  final class DefaultEntry[K, V](val key: K, var value: V) extends HashEntry[K, DefaultEntry[K, V]] with Serializable {
    override def toString: String = s"DefaultEntry($key -> $value)"
  }
}

private[mutable] abstract class ParHashMapCombiner[K, V](private val tableLoadFactor: Int)
extends scala.collection.parallel.BucketCombiner[(K, V), ParHashMap[K, V], DefaultEntry[K, V], ParHashMapCombiner[K, V]](ParHashMapCombiner.numblocks)
   with scala.collection.mutable.HashTable.HashUtils[K]
{
  private val nonmasklen = ParHashMapCombiner.nonmasklength
  private val seedvalue = 27

  def addOne(elem: (K, V)) = {
    sz += 1
    val hc = improve(elemHashCode(elem._1), seedvalue)
    val pos = (hc >>> nonmasklen)
    if (buckets(pos) eq null) {
      // initialize bucket
      buckets(pos) = new UnrolledBuffer[DefaultEntry[K, V]]()
    }
    // add to bucket
    buckets(pos) += new DefaultEntry(elem._1, elem._2)
    this
  }

  def result(): ParHashMap[K, V] = if (size >= (ParHashMapCombiner.numblocks * sizeMapBucketSize)) { // 1024
    // construct table
    val table = new AddingHashTable(size, tableLoadFactor, seedvalue)
    val bucks = buckets.map(b => if (b ne null) b.headPtr else null)
    val insertcount = combinerTaskSupport.executeAndWaitResult(new FillBlocks(bucks, table, 0, bucks.length))
    table.setSize(insertcount)
    // TODO compare insertcount and size to see if compression is needed
    val c = table.hashTableContents
    new ParHashMap(c)
  } else {
    // construct a normal table and fill it sequentially
    // TODO parallelize by keeping separate sizemaps and merging them
    object newTable extends HashTable[K, DefaultEntry[K, V], DefaultEntry[K, V]] with WithContents[K, DefaultEntry[K, V], DefaultEntry[K, V]] {
      type Entry = DefaultEntry[K, V]
      def insertEntry(e: Entry): Unit = { super.findOrAddEntry(e.key, e) }
      def createNewEntry(key: K, entry: Entry): Entry = entry
      sizeMapInit(table.length)
    }
    var i = 0
    while (i < ParHashMapCombiner.numblocks) {
      if (buckets(i) ne null) {
        for (elem <- buckets(i)) newTable.insertEntry(elem)
      }
      i += 1
    }
    new ParHashMap(newTable.hashTableContents)
  }

  /* classes */

  /** A hash table which will never resize itself. Knowing the number of elements in advance,
   *  it allocates the table of the required size when created.
   *
   *  Entries are added using the `insertEntry` method. This method checks whether the element
   *  exists and updates the size map. It returns false if the key was already in the table,
   *  and true if the key was successfully inserted. It does not update the number of elements
   *  in the table.
   */
  private[ParHashMapCombiner] class AddingHashTable(numelems: Int, lf: Int, _seedvalue: Int) extends HashTable[K, V, DefaultEntry[K, V]]
    with WithContents[K, V, DefaultEntry[K, V]] {

    import HashTable._
    _loadFactor = lf
    table = new Array[HashEntry[K, DefaultEntry[K, V]]](capacity(sizeForThreshold(_loadFactor, numelems)))
    tableSize = 0
    this.seedvalue = _seedvalue
    threshold = newThreshold(_loadFactor, table.length)
    sizeMapInit(table.length)
    def setSize(sz: Int) = tableSize = sz
    def insertEntry(/*block: Int, */e: DefaultEntry[K, V]): Boolean = {
      var h = index(elemHashCode(e.key))
      val olde = table(h).asInstanceOf[DefaultEntry[K, V]]

      // check if key already exists
      var ce = olde
      while (ce ne null) {
        if (ce.key == e.key) {
          h = -1
          ce = null
        } else ce = ce.next
      }

      // if key does not already exist
      if (h != -1) {
        e.next = olde
        table(h) = e
        nnSizeMapAdd(h)
        true
      } else false
    }
    def createNewEntry(key: K, x: V) = ???
  }

  /* tasks */

  import UnrolledBuffer.Unrolled

  class FillBlocks(buckets: Array[Unrolled[DefaultEntry[K, V]]], table: AddingHashTable, offset: Int, howmany: Int)
  extends Task[Int, FillBlocks] {
    var result = Int.MinValue
    def leaf(prev: Option[Int]) = {
      var i = offset
      val until = offset + howmany
      result = 0
      while (i < until) {
        result += fillBlock(i, buckets(i))
        i += 1
      }
    }
    private def fillBlock(block: Int, elems: Unrolled[DefaultEntry[K, V]]) = {
      var insertcount = 0
      var unrolled = elems
      var i = 0
      val t = table
      while (unrolled ne null) {
        val chunkarr = unrolled.array
        val chunksz = unrolled.size
        while (i < chunksz) {
          val elem = chunkarr(i)
          if (t.insertEntry(elem)) insertcount += 1
          i += 1
        }
        i = 0
        unrolled = unrolled.next
      }
      insertcount
    }
    def split = {
      val fp = howmany / 2
      List(new FillBlocks(buckets, table, offset, fp), new FillBlocks(buckets, table, offset + fp, howmany - fp))
    }
    override def merge(that: FillBlocks): Unit = {
      this.result += that.result
    }
    def shouldSplitFurther = howmany > scala.collection.parallel.thresholdFromSize(ParHashMapCombiner.numblocks, combinerTaskSupport.parallelismLevel)
  }
}

private[parallel] object ParHashMapCombiner {
  private[mutable] val discriminantbits = 5
  private[mutable] val numblocks = 1 << discriminantbits
  private[mutable] val discriminantmask = ((1 << discriminantbits) - 1)
  private[mutable] val nonmasklength = 32 - discriminantbits

  def apply[K, V] = new ParHashMapCombiner[K, V](HashTable.defaultLoadFactor) {} // was: with EnvironmentPassingCombiner[(K, V), ParHashMap[K, V]]
}
