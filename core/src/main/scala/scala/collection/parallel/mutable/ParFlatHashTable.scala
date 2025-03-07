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
package parallel.mutable

import scala.collection.parallel.IterableSplitter

/** Parallel flat hash table.
 *
 *  @tparam T      type of the elements in the $coll.
 *  @define coll   table
 *  @define Coll   `ParFlatHashTable`
 *
 */
trait ParFlatHashTable[T] extends scala.collection.mutable.FlatHashTable[T] {

  override def alwaysInitSizeMap = true

  abstract class ParFlatHashTableIterator(var idx: Int, val until: Int, val totalsize: Int)
  extends IterableSplitter[T] with SizeMapUtils {

    private[this] var traversed = 0
    private[this] val itertable = table

    if (hasNext) scan()

    private[this] def scan(): Unit = {
      while (itertable(idx) eq null) {
        idx += 1
      }
    }

    def newIterator(index: Int, until: Int, totalsize: Int): IterableSplitter[T]

    def remaining = totalsize - traversed
    def hasNext = traversed < totalsize
    def next() = if (hasNext) {
      val r = entryToElem(itertable(idx))
      traversed += 1
      idx += 1
      if (hasNext) scan()
      r
    } else Iterator.empty.next()
    def dup = newIterator(idx, until, totalsize)
    def split = if (remaining > 1) {
      val divpt = (until + idx) / 2

      val fstidx = idx
      val fstuntil = divpt
      val fsttotal = calcNumElems(idx, divpt, itertable.length, sizeMapBucketSize)
      val fstit = newIterator(fstidx, fstuntil, fsttotal)

      val sndidx = divpt
      val snduntil = until
      val sndtotal = remaining - fsttotal
      val sndit = newIterator(sndidx, snduntil, sndtotal)

      scala.Seq(fstit, sndit)
    } else scala.Seq(this)

    import DebugUtils._
    override def debugInformation = buildString {
      append =>
      append("Parallel flat hash table iterator")
      append("---------------------------------")
      append("Traversed/total: " + traversed + " / " + totalsize)
      append("Table idx/until: " + idx + " / " + until)
      append("Table length: " + itertable.length)
      append("Table: ")
      append(arrayString(itertable, 0, itertable.length))
      append("Sizemap: ")
      append(arrayString(sizemap, 0, sizemap.length))
    }

    protected def countElems(from: Int, until: Int) = {
      var count = 0
      var i = from
      while (i < until) {
        if (itertable(i) ne null) count += 1
        i += 1
      }
      count
    }

    protected def countBucketSizes(frombucket: Int, untilbucket: Int) = {
      var count = 0
      var i = frombucket
      while (i < untilbucket) {
        count += sizemap(i)
        i += 1
      }
      count
    }
  }
}
