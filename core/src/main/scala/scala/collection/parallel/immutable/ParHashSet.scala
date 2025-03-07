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
package collection.parallel.immutable



import scala.collection.Hashing
import scala.collection.parallel.ParSetLike
import scala.collection.parallel.Combiner
import scala.collection.parallel.IterableSplitter
import scala.collection.mutable.UnrolledBuffer.Unrolled
import scala.collection.mutable.UnrolledBuffer
import scala.collection.generic.{CanCombineFrom, GenericParCompanion, GenericParTemplate, ParSetFactory}
import scala.collection.immutable.{OldHashSet, TrieIterator}
import scala.collection.parallel.Task



/** Immutable parallel hash set, based on hash tries.
 *
 *  $paralleliterableinfo
 *
 *  $sideeffects
 *
 *  @tparam T    the element type of the set
 *
 *  @see  [[http://docs.scala-lang.org/overviews/parallel-collections/concrete-parallel-collections.html#parallel_hash_tries Scala's Parallel Collections Library overview]]
 *  section on Parallel Hash Tries for more information.
 *
 *  @define Coll `immutable.ParHashSet`
 *  @define coll immutable parallel hash set
 */
@SerialVersionUID(1L)
class ParHashSet[T] private[immutable] (private[this] val trie: OldHashSet[T])
extends ParSet[T]
   with GenericParTemplate[T, ParHashSet]
   with ParSetLike[T, ParHashSet, ParHashSet[T], OldHashSet[T]]
   with Serializable
{
self =>

  def this() = this(OldHashSet.empty[T])

  override def companion: GenericParCompanion[ParHashSet] = ParHashSet

  override def empty: ParHashSet[T] = new ParHashSet[T]

  def splitter: IterableSplitter[T] = new ParHashSetIterator(trie.iterator, trie.size)

  override def seq = trie

  def -(e: T) = new ParHashSet(trie - e)

  def +(e: T) = new ParHashSet(trie + e)

  def contains(e: T): Boolean = trie.contains(e)

  def size = trie.size
  override def knownSize = trie.size

  protected override def reuse[S, That](oldc: Option[Combiner[S, That]], newc: Combiner[S, That]) = oldc match {
    case Some(old) => old
    case None => newc
  }

  class ParHashSetIterator(var triter: Iterator[T], val sz: Int)
  extends IterableSplitter[T] {
    var i = 0
    def dup = triter match {
      case t: TrieIterator[T] =>
        dupFromIterator(t.dupIterator)
      case _ =>
        val buff = triter.toBuffer
        triter = buff.iterator
        dupFromIterator(buff.iterator)
    }
    private def dupFromIterator(it: Iterator[T]) = {
      val phit = new ParHashSetIterator(it, sz)
      phit.i = i
      phit
    }
    def split: Seq[IterableSplitter[T]] = if (remaining < 2) Seq(this) else triter match {
      case t: TrieIterator[T] =>
        val previousRemaining = remaining
        val ((fst, fstlength), snd) = t.split
        val sndlength = previousRemaining - fstlength
        Seq(
          new ParHashSetIterator(fst, fstlength),
          new ParHashSetIterator(snd, sndlength)
        )
      case _ =>
        // iterator of the collision map case
        val buff = triter.toBuffer
        val (fp, sp) = buff.splitAt(buff.length / 2)
        Seq(fp, sp) map { b => new ParHashSetIterator(b.iterator, b.length) }
    }
    def next(): T = {
      i += 1
      triter.next()
    }
    def hasNext: Boolean = {
      i < sz
    }
    def remaining = sz - i
  }

}


/** $factoryInfo
 *  @define Coll `immutable.ParHashSet`
 *  @define coll immutable parallel hash set
 */
object ParHashSet extends ParSetFactory[ParHashSet] {
  def newCombiner[T]: Combiner[T, ParHashSet[T]] = HashSetCombiner[T]

  implicit def canBuildFrom[S, T]: CanCombineFrom[ParHashSet[S], T, ParHashSet[T]] =
    new GenericCanCombineFrom[S, T]

  def fromTrie[T](t: OldHashSet[T]) = new ParHashSet(t)
}


private[immutable] abstract class HashSetCombiner[T]
extends scala.collection.parallel.BucketCombiner[T, ParHashSet[T], Any, HashSetCombiner[T]](HashSetCombiner.rootsize) {
//self: EnvironmentPassingCombiner[T, ParHashSet[T]] =>
  import HashSetCombiner._
  val emptyTrie = OldHashSet.empty[T]

  def addOne(elem: T) = {
    sz += 1
    val hc = Hashing.computeHash(elem)
    val pos = hc & 0x1f
    if (buckets(pos) eq null) {
      // initialize bucket
      buckets(pos) = new UnrolledBuffer[Any]
    }
    // add to bucket
    buckets(pos) += elem
    this
  }

  def result() = {
    val bucks = buckets.filter(_ != null).map(_.headPtr)
    val root = new Array[OldHashSet[T]](bucks.length)

    combinerTaskSupport.executeAndWaitResult(new CreateTrie(bucks, root, 0, bucks.length))

    var bitmap = 0
    var i = 0
    while (i < rootsize) {
      if (buckets(i) ne null) bitmap |= 1 << i
      i += 1
    }
    val sz = root.foldLeft(0)(_ + _.size)

    if (sz == 0) new ParHashSet[T]
    else if (sz == 1) new ParHashSet[T](root(0))
    else {
      val trie = new OldHashSet.HashTrieSet(bitmap, root, sz)
      new ParHashSet[T](trie)
    }
  }

  /* tasks */

  class CreateTrie(bucks: Array[Unrolled[Any]], root: Array[OldHashSet[T]], offset: Int, howmany: Int)
  extends Task[Unit, CreateTrie] {
    var result = ()
    def leaf(prev: Option[Unit]) = {
      var i = offset
      val until = offset + howmany
      while (i < until) {
        root(i) = createTrie(bucks(i))
        i += 1
      }
    }
    private def createTrie(elems: Unrolled[Any]): OldHashSet[T] = {
      var trie = OldHashSet.empty[T]

      var unrolled = elems
      var i = 0
      while (unrolled ne null) {
        val chunkarr = unrolled.array
        val chunksz = unrolled.size
        while (i < chunksz) {
          val v = chunkarr(i).asInstanceOf[T]
          val hc = Hashing.computeHash(v)
          trie = trie.updated0(v, hc, rootbits)  // internal API, private[collection]
          i += 1
        }
        i = 0
        unrolled = unrolled.next
      }

      trie
    }
    def split = {
      val fp = howmany / 2
      List(new CreateTrie(bucks, root, offset, fp), new CreateTrie(bucks, root, offset + fp, howmany - fp))
    }
    def shouldSplitFurther = howmany > scala.collection.parallel.thresholdFromSize(root.length, combinerTaskSupport.parallelismLevel)
  }
}


object HashSetCombiner {
  def apply[T] = new HashSetCombiner[T] {} // was: with EnvironmentPassingCombiner[T, ParHashSet[T]] {}

  private[immutable] val rootbits = 5
  private[immutable] val rootsize = 1 << 5
}
