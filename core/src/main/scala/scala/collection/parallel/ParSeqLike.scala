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

import scala.collection.{AnyConstr, BufferedIterator, Iterator, SeqOps}
import scala.collection.generic.DefaultSignalling
import scala.collection.generic.AtomicIndexFlag
import scala.collection.generic.VolatileAbort
import scala.collection.parallel.ParallelCollectionImplicits._

/** A template trait for sequences of type `ParSeq[T]`, representing
 *  parallel sequences with element type `T`.
 *
 *  $parallelseqinfo
 *
 *  @tparam T           the type of the elements contained in this collection
 *  @tparam Repr        the type of the actual collection containing the elements
 *  @tparam Sequential  the type of the sequential version of this parallel collection
 *
 *  @define parallelseqinfo
 *  Parallel sequences inherit the `Seq` trait. Their indexing and length computations
 *  are defined to be efficient. Like their sequential counterparts
 *  they always have a defined order of elements. This means they will produce resulting
 *  parallel sequences in the same way sequential sequences do. However, the order
 *  in which they perform bulk operations on elements to produce results is not defined and is generally
 *  nondeterministic. If the higher-order functions given to them produce no sideeffects,
 *  then this won't be noticeable.
 *
 *  @define mayNotTerminateInf
 *    Note: may not terminate for infinite-sized collections.
 *  @define willNotTerminateInf
 *    Note: will not terminate for infinite-sized collections.
 *
 *  This trait defines a new, more general `split` operation and reimplements the `split`
 *  operation of `ParallelIterable` trait using the new `split` operation.
 *
 *  @author Aleksandar Prokopec
 *  @since 2.9
 */
trait ParSeqLike[+T, +CC[X] <: ParSeq[X], +Repr <: ParSeq[T], +Sequential <: scala.collection.Seq[T] with SeqOps[T, AnyConstr, Sequential]]
extends ParIterableLike[T, CC, Repr, Sequential]
   with Equals {
self =>

  def length: Int
  def apply(index: Int): T

  override def hashCode() = scala.util.hashing.MurmurHash3.orderedHash(this, "ParSeq".hashCode)

  /** The equals method for arbitrary parallel sequences. Compares this
    * parallel sequence to some other object.
    *  @param    that  The object to compare the sequence to
    *  @return   `true` if `that` is a sequence that has the same elements as
    *            this sequence in the same order, `false` otherwise
    */
  override def equals(that: Any): Boolean = that match {
    case that: ParSeq[_] => (that eq this.asInstanceOf[AnyRef]) || (that canEqual this) && (this sameElements that)
    case _               => false
  }

  def canEqual(other: Any): Boolean = true

  protected[this] type SuperParIterator = IterableSplitter[T]

  /** A more refined version of the iterator found in the `ParallelIterable` trait,
   *  this iterator can be split into arbitrary subsets of iterators.
   *
   *  @return       an iterator that can be split into subsets of precise size
   */
  protected[parallel] def splitter: SeqSplitter[T]

  override def iterator: PreciseSplitter[T] = splitter

  final def size = length

  /** Used to iterate elements using indices */
  protected abstract class Elements(start: Int, val end: Int) extends SeqSplitter[T] with BufferedIterator[T] {
    private var i = start

    def hasNext = i < end

    def next(): T = if (i < end) {
      val x = self(i)
      i += 1
      x
    } else Iterator.empty.next()

    def head = self(i)

    final def remaining = end - i

    def dup = new Elements(i, end) {}

    def split = psplit(remaining / 2, remaining - remaining / 2)

    def psplit(sizes: Int*) = {
      val incr = sizes.scanLeft(0)(_ + _)
      for ((from, until) <- incr.init zip incr.tail) yield {
        new Elements(start + from, (start + until) min end) {}
      }
    }

    override def toString = "Elements(" + start + ", " + end + ")"
  }

  /** Tests whether this $coll contains given index.
    *
    *  The implementations of methods `apply` and `isDefinedAt` turn a `ParSeq[T]` into
    *  a `PartialFunction[Int, T]`.
    *
    * @param    idx     the index to test
    * @return   `true` if this $coll contains an element at position `idx`, `false` otherwise.
    */
  def isDefinedAt(idx: Int): Boolean = (idx >= 0) && (idx < length)

  /* ParallelSeq methods */

  /** Returns the length of the longest segment of elements starting at
   *  a given position satisfying some predicate.
   *
   *  $indexsignalling
   *
   *  The index flag is initially set to maximum integer value.
   *
   *  @param p     the predicate used to test the elements
   *  @param from  the starting offset for the search
   *  @return      the length of the longest segment of elements starting at `from` and
   *               satisfying the predicate
   */
  def segmentLength(p: T => Boolean, from: Int): Int = if (from >= length) 0 else {
    val realfrom = if (from < 0) 0 else from
    val ctx = new DefaultSignalling with AtomicIndexFlag
    ctx.setIndexFlag(Int.MaxValue)
    tasksupport.executeAndWaitResult(new SegmentLength(p, 0, splitter.psplitWithSignalling(realfrom, length - realfrom)(1) assign ctx))._1
  }

  /** Returns the length of the longest prefix whose elements all satisfy some predicate.
    *
    *  $mayNotTerminateInf
    *
    *  @param   p     the predicate used to test elements.
    *  @return  the length of the longest prefix of this $coll
    *           such that every element of the segment satisfies the predicate `p`.
    */
  def prefixLength(p: T => Boolean): Int = segmentLength(p, 0)

  /** Finds index of first occurrence of some value in this $coll.
    *
    *  @param   elem   the element value to search for.
    *  @tparam  B      the type of the element `elem`.
    *  @return  the index of the first element of this $coll that is equal (as determined by `==`)
    *           to `elem`, or `-1`, if none exists.
    */
  def indexOf[B >: T](elem: B): Int = indexOf(elem, 0)

  /** Finds index of first occurrence of some value in this $coll after or at some start index.
    *
    *  @param   elem   the element value to search for.
    *  @tparam  B      the type of the element `elem`.
    *  @param   from   the start index
    *  @return  the index `>= from` of the first element of this $coll that is equal (as determined by `==`)
    *           to `elem`, or `-1`, if none exists.
    */
  def indexOf[B >: T](elem: B, from: Int): Int = indexWhere(elem == _, from)


  /** Finds index of first element satisfying some predicate.
    *
    *  $mayNotTerminateInf
    *
    *  @param   p     the predicate used to test elements.
    *  @return  the index of the first element of this $coll that satisfies the predicate `p`,
    *           or `-1`, if none exists.
    */
  def indexWhere(p: T => Boolean): Int = indexWhere(p, 0)

  /** Finds the first element satisfying some predicate.
   *
   *  $indexsignalling
   *
   *  The index flag is initially set to maximum integer value.
   *
   *  @param p     the predicate used to test the elements
   *  @param from  the starting offset for the search
   *  @return      the index `>= from` of the first element of this $coll that satisfies the predicate `p`,
   *               or `-1`, if none exists
   */
  def indexWhere(p: T => Boolean, from: Int): Int = if (from >= length) -1 else {
    val realfrom = if (from < 0) 0 else from
    val ctx = new DefaultSignalling with AtomicIndexFlag
    ctx.setIndexFlag(Int.MaxValue)
    tasksupport.executeAndWaitResult(new IndexWhere(p, realfrom, splitter.psplitWithSignalling(realfrom, length - realfrom)(1) assign ctx))
  }


  /** Finds index of last occurrence of some value in this $coll.
    *
    *  $willNotTerminateInf
    *
    *  @param   elem   the element value to search for.
    *  @tparam  B      the type of the element `elem`.
    *  @return  the index of the last element of this $coll that is equal (as determined by `==`)
    *           to `elem`, or `-1`, if none exists.
    */
  def lastIndexOf[B >: T](elem: B): Int = lastIndexWhere(elem == _)

  /** Finds index of last occurrence of some value in this $coll before or at a given end index.
    *
    *  @param   elem   the element value to search for.
    *  @param   end    the end index.
    *  @tparam  B      the type of the element `elem`.
    *  @return  the index `<= end` of the last element of this $coll that is equal (as determined by `==`)
    *           to `elem`, or `-1`, if none exists.
    */
  def lastIndexOf[B >: T](elem: B, end: Int): Int = lastIndexWhere(elem == _, end)

  /** Finds index of last element satisfying some predicate.
    *
    *  $willNotTerminateInf
    *
    *  @param   p     the predicate used to test elements.
    *  @return  the index of the last element of this $coll that satisfies the predicate `p`,
    *           or `-1`, if none exists.
    */
  def lastIndexWhere(p: T => Boolean): Int = lastIndexWhere(p, length - 1)

  /** Finds the last element satisfying some predicate.
   *
   *  $indexsignalling
   *
   *  The index flag is initially set to minimum integer value.
   *
   *  @param p     the predicate used to test the elements
   *  @param end   the maximum offset for the search
   *  @return      the index `<= end` of the first element of this $coll that satisfies the predicate `p`,
   *               or `-1`, if none exists
   */
  def lastIndexWhere(p: T => Boolean, end: Int): Int = if (end < 0) -1 else {
    val until = if (end >= length) length else end + 1
    val ctx = new DefaultSignalling with AtomicIndexFlag
    ctx.setIndexFlag(Int.MinValue)
    tasksupport.executeAndWaitResult(new LastIndexWhere(p, 0, splitter.psplitWithSignalling(until, length - until)(0) assign ctx))
  }

  def reverse: Repr = {
    tasksupport.executeAndWaitResult(new Reverse(() => newCombiner, splitter) mapResult { _.resultWithTaskSupport })
  }

  def reverseMap[S](f: T => S): CC[S] = {
    tasksupport.executeAndWaitResult(
      new ReverseMap[S, CC[S]](f, () => companion.newCombiner[S], splitter) mapResult { _.resultWithTaskSupport }
    )
  }

  /** Tests whether this $coll contains the given sequence at a given index.
   *
   *  $abortsignalling
   *
   *  @tparam S      the element type of `that` parallel sequence
   *  @param that    the parallel sequence this sequence is being searched for
   *  @param offset  the starting offset for the search
   *  @return        `true` if there is a sequence `that` starting at `offset` in this sequence, `false` otherwise
   */
  def startsWith[S >: T](that: IterableOnce[S], offset: Int = 0): Boolean = that match {
    case pt: ParSeq[S] =>
      if (offset < 0 || offset >= length) offset == length && pt.isEmpty
      else if (pt.isEmpty) true
      else if (pt.length > length - offset) false
      else {
        val ctx = new DefaultSignalling with VolatileAbort
        tasksupport.executeAndWaitResult(
          new SameElements[S](splitter.psplitWithSignalling(offset, pt.length)(1) assign ctx, pt.splitter)
        )
      }
    case _ => seq.startsWith(that, offset)
  }

  override def sameElements[U >: T](that: IterableOnce[U]): Boolean = {
    that match {
      case pthat: ParSeq[U] =>
        val ctx = new DefaultSignalling with VolatileAbort
        length == pthat.length && tasksupport.executeAndWaitResult(new SameElements(splitter assign ctx, pthat.splitter))
      case _ => super.sameElements(that)
    }
  }

  /** Tests whether this $coll ends with the given parallel sequence.
   *
   *  $abortsignalling
   *
   *  @tparam S       the type of the elements of `that` sequence
   *  @param that     the sequence to test
   *  @return         `true` if this $coll has `that` as a suffix, `false` otherwise
   */
  def endsWith[S >: T](that: ParSeq[S]): Boolean = {
    if (that.length == 0) true
    else if (that.length > length) false
    else {
      val ctx = new DefaultSignalling with VolatileAbort
      val tlen = that.length
      tasksupport.executeAndWaitResult(new SameElements[S](splitter.psplitWithSignalling(length - tlen, tlen)(1) assign ctx, that.splitter))
    }
  }

  /** Tests whether this $coll ends with the given collection.
    *
    *  $abortsignalling
    *
    *  @tparam S       the type of the elements of `that` sequence
    *  @param that     the sequence to test
    *  @return         `true` if this $coll has `that` as a suffix, `false` otherwise
    */
  def endsWith[S >: T](that: Iterable[S]): Boolean = seq.endsWith(that)

  /** Overload of ''patch'' that takes a sequential collection as parameter */
  def patch[U >: T](from: Int, patch: scala.collection.Seq[U], replaced: Int): CC[U] = patch_sequential(from, patch, replaced)

  def patch[U >: T](from: Int, patch: ParSeq[U], replaced: Int): CC[U] = {
    val realreplaced = replaced min (length - from)
    if ((size - realreplaced + patch.size) > MIN_FOR_COPY) {
      val that = patch.asParSeq
      val pits = splitter.psplitWithSignalling(from, replaced, length - from - realreplaced)
      val cfactory = combinerFactory(() => companion.newCombiner[U])
      val copystart = new Copy[U, CC[U]](cfactory, pits(0))
      val copymiddle = wrap {
        val tsk = new that.Copy[U, CC[U]](cfactory, that.splitter)
        tasksupport.executeAndWaitResult(tsk)
      }
      val copyend = new Copy[U, CC[U]](cfactory, pits(2))
      tasksupport.executeAndWaitResult(((copystart parallel copymiddle) { _ combine _ } parallel copyend) { _ combine _ } mapResult {
        _.resultWithTaskSupport
      })
    } else patch_sequential(from, patch.seq, replaced)
  }

  private def patch_sequential[U >: T](fromarg: Int, patch: scala.collection.Seq[U], r: Int): CC[U] = {
    val from = 0 max fromarg
    val b = companion.newBuilder[U]
    val repl = (r min (length - from)) max 0
    val pits = splitter.psplitWithSignalling(from, repl, length - from - repl)
    b ++= pits(0)
    b ++= patch
    b ++= pits(2)
    setTaskSupport(b.result(), tasksupport)
  }

  def updated[U >: T](index: Int, elem: U): CC[U] = {
    tasksupport.executeAndWaitResult(
      new Updated(index, elem, combinerFactory(() => companion.newCombiner[U]), splitter) mapResult {
        _.resultWithTaskSupport
      }
    )
  }

  def +:[U >: T, That](elem: U): CC[U] = {
    patch(0, mutable.ParArray(elem), 0)
  }

  def :+[U >: T, That](elem: U): CC[U] = {
    patch(length, mutable.ParArray(elem), 0)
  }


  /** Produces a new sequence which contains all elements of this $coll and also all elements of
    *  a given sequence. `xs union ys`  is equivalent to `xs ++ ys`.
    *
    * Another way to express this
    * is that `xs union ys` computes the order-preserving multi-set union of `xs` and `ys`.
    * `union` is hence a counter-part of `diff` and `intersect` which also work on multi-sets.
    *
    * $willNotTerminateInf
    *
    *  @param that   the sequence to add.
    *  @tparam B     the element type of the returned $coll.
    *  @return       a new $coll which contains all elements of this $coll
    *                  followed by all elements of `that`.
    */
  def union[B >: T](that: ParSeq[B]): CC[B] = this ++ that

  /** Overload of ''union'' that takes a sequential collection as parameter */
  def union[B >: T](that: scala.collection.Seq[B]): CC[B] = this ++ that

  def padTo[U >: T](len: Int, elem: U): CC[U] = if (length < len) {
    patch(length, new immutable.Repetition(elem, len - length), 0)
  } else patch(length, ParSeq.newBuilder[U].result(), 0)

  override def zip[U >: T, S](that: ParIterable[S]): CC[(U, S)] = /*if (bf(repr).isCombiner && that.isParSeq)*/ {
    that match {
      case thatseq: ParSeq[S] =>
        tasksupport.executeAndWaitResult(
          new Zip(length min thatseq.length, combinerFactory(() => companion.newCombiner[(U, S)]), splitter, thatseq.splitter) mapResult {
            _.resultWithTaskSupport
          }
        )
      case _ => super.zip(that)
    }
  }

  /** Tests whether every element of this $coll relates to the
   *  corresponding element of another parallel sequence by satisfying a test predicate.
   *
   *  $abortsignalling
   *
   *  @param   that    the other parallel sequence
   *  @param   p       the test predicate, which relates elements from both sequences
   *  @tparam  S       the type of the elements of `that`
   *  @return          `true` if both parallel sequences have the same length and
   *                   `p(x, y)` is `true` for all corresponding elements `x` of this $coll
   *                   and `y` of `that`, otherwise `false`
   */
  def corresponds[S](that: ParSeq[S])(p: (T, S) => Boolean): Boolean = {
    val ctx = new DefaultSignalling with VolatileAbort
    length == that.length && tasksupport.executeAndWaitResult(new Corresponds(p, splitter assign ctx, that.splitter))
  }

  def diff[U >: T](that: ParSeq[U]): Repr = diff(that.seq)

  def diff[U >: T](that: scala.collection.Seq[U]): Repr = sequentially {
    _ diff that
  }

  /** Computes the multiset intersection between this $coll and another sequence.
   *
   *  @param that   the sequence of elements to intersect with.
   *  @tparam U     the element type of `that` parallel sequence
   *  @return       a new collection of type `That` which contains all elements of this $coll
   *                which also appear in `that`.
   *                If an element value `x` appears
   *                ''n'' times in `that`, then the first ''n'' occurrences of `x` will be retained
   *                in the result, but any following occurrences will be omitted.
   *
   *  @usecase def intersect(that: Seq[T]): $Coll[T]
   *    @inheritdoc
   *
   *    $mayNotTerminateInf
   *
   *    @return       a new $coll which contains all elements of this $coll
   *                  which also appear in `that`.
   *                  If an element value `x` appears
   *                  ''n'' times in `that`, then the first ''n'' occurrences of `x` will be retained
   *                  in the result, but any following occurrences will be omitted.
   */
  def intersect[U >: T](that: scala.collection.Seq[U]) = sequentially {
    _ intersect that
  }

  /** Builds a new $coll from this $coll without any duplicate elements.
   *  $willNotTerminateInf
   *
   *  @return  A new $coll which contains the first occurrence of every element of this $coll.
   */
  def distinct: Repr = sequentially {
    _.distinct
  }

  override def toString = seq.mkString(stringPrefix + "(", ", ", ")")

  override def toSeq = this.asInstanceOf[ParSeq[T]]

  /* tasks */

  protected[this] def down(p: IterableSplitter[_]) = p.asInstanceOf[SeqSplitter[T]]

  protected trait Accessor[R, Tp] extends super.Accessor[R, Tp] {
    protected[this] val pit: SeqSplitter[T]
  }

  protected trait Transformer[R, Tp] extends Accessor[R, Tp] with super.Transformer[R, Tp]

  protected[this] class SegmentLength(pred: T => Boolean, from: Int, protected[this] val pit: SeqSplitter[T])
  extends Accessor[(Int, Boolean), SegmentLength] {
    @volatile var result: (Int, Boolean) = null
    def leaf(prev: Option[(Int, Boolean)]) = if (from < pit.indexFlag) {
      val itsize = pit.remaining
      val seglen = pit.prefixLength(pred)
      result = (seglen, itsize == seglen)
      if (!result._2) pit.setIndexFlagIfLesser(from)
    } else result = (0, false)
    protected[this] def newSubtask(p: SuperParIterator) = throw new UnsupportedOperationException
    override def split = {
      val pits = pit.splitWithSignalling
      for ((p, untilp) <- pits zip pits.scanLeft(0)(_ + _.remaining)) yield new SegmentLength(pred, from + untilp, p)
    }
    override def merge(that: SegmentLength) = if (result._2) result = (result._1 + that.result._1, that.result._2)
    override def requiresStrictSplitters = true
  }

  protected[this] class IndexWhere(pred: T => Boolean, from: Int, protected[this] val pit: SeqSplitter[T])
  extends Accessor[Int, IndexWhere] {
    @volatile var result: Int = -1
    def leaf(prev: Option[Int]) = if (from < pit.indexFlag) {
      val r = pit.indexWhere(pred)
      if (r != -1) {
        result = from + r
        pit.setIndexFlagIfLesser(from)
      }
    }
    protected[this] def newSubtask(p: SuperParIterator) = throw new UnsupportedOperationException
    override def split = {
      val pits = pit.splitWithSignalling
      for ((p, untilp) <- pits zip pits.scanLeft(from)(_ + _.remaining)) yield new IndexWhere(pred, untilp, p)
    }
    override def merge(that: IndexWhere) = result = if (result == -1) that.result else {
      if (that.result != -1) result min that.result else result
    }
    override def requiresStrictSplitters = true
  }

  protected[this] class LastIndexWhere(pred: T => Boolean, pos: Int, protected[this] val pit: SeqSplitter[T])
  extends Accessor[Int, LastIndexWhere] {
    @volatile var result: Int = -1
    def leaf(prev: Option[Int]) = if (pos > pit.indexFlag) {
      val r = pit.lastIndexWhere(pred)
      if (r != -1) {
        result = pos + r
        pit.setIndexFlagIfGreater(pos)
      }
    }
    protected[this] def newSubtask(p: SuperParIterator) = throw new UnsupportedOperationException
    override def split = {
      val pits = pit.splitWithSignalling
      for ((p, untilp) <- pits zip pits.scanLeft(pos)(_ + _.remaining)) yield new LastIndexWhere(pred, untilp, p)
    }
    override def merge(that: LastIndexWhere) = result = if (result == -1) that.result else {
      if (that.result != -1) result max that.result else result
    }
    override def requiresStrictSplitters = true
  }

  protected[this] class Reverse[U >: T, This >: Repr](cbf: () => Combiner[U, This], protected[this] val pit: SeqSplitter[T])
  extends Transformer[Combiner[U, This], Reverse[U, This]] {
    @volatile var result: Combiner[U, This] = null
    def leaf(prev: Option[Combiner[U, This]]) = result = pit.reverse2combiner(reuse(prev, cbf()))
    protected[this] def newSubtask(p: SuperParIterator) = new Reverse(cbf, down(p))
    override def merge(that: Reverse[U, This]) = result = that.result combine result
  }

  protected[this] class ReverseMap[S, That](f: T => S, pbf: () => Combiner[S, That], protected[this] val pit: SeqSplitter[T])
  extends Transformer[Combiner[S, That], ReverseMap[S, That]] {
    @volatile var result: Combiner[S, That] = null
    def leaf(prev: Option[Combiner[S, That]]) = result = pit.reverseMap2combiner(f, pbf())
    protected[this] def newSubtask(p: SuperParIterator) = new ReverseMap(f, pbf, down(p))
    override def merge(that: ReverseMap[S, That]) = result = that.result combine result
  }

  protected[this] class SameElements[U >: T](protected[this] val pit: SeqSplitter[T], val otherpit: SeqSplitter[U])
  extends Accessor[Boolean, SameElements[U]] {
    @volatile var result: Boolean = true
    def leaf(prev: Option[Boolean]) = if (!pit.isAborted) {
      result = pit.sameElements(otherpit)
      if (!result) pit.abort()
    }
    protected[this] def newSubtask(p: SuperParIterator) = throw new UnsupportedOperationException
    override def split = {
      val fp = pit.remaining / 2
      val sp = pit.remaining - fp
      for ((p, op) <- pit.psplitWithSignalling(fp, sp) zip otherpit.psplitWithSignalling(fp, sp)) yield new SameElements(p, op)
    }
    override def merge(that: SameElements[U]) = result = result && that.result
    override def requiresStrictSplitters = true
  }

  protected[this] class Updated[U >: T, That](pos: Int, elem: U, pbf: CombinerFactory[U, That], protected[this] val pit: SeqSplitter[T])
  extends Transformer[Combiner[U, That], Updated[U, That]] {
    @volatile var result: Combiner[U, That] = null
    def leaf(prev: Option[Combiner[U, That]]) = result = pit.updated2combiner(pos, elem, pbf())
    protected[this] def newSubtask(p: SuperParIterator) = throw new UnsupportedOperationException
    override def split = {
      val pits = pit.splitWithSignalling
      for ((p, untilp) <- pits zip pits.scanLeft(0)(_ + _.remaining)) yield new Updated(pos - untilp, elem, pbf, p)
    }
    override def merge(that: Updated[U, That]) = result = result combine that.result
    override def requiresStrictSplitters = true
  }

  protected[this] class Zip[U >: T, S, That](len: Int, cf: CombinerFactory[(U, S), That], protected[this] val pit: SeqSplitter[T], val otherpit: SeqSplitter[S])
  extends Transformer[Combiner[(U, S), That], Zip[U, S, That]] {
    @volatile var result: Result = null
    def leaf(prev: Option[Result]) = result = pit.zip2combiner[U, S, That](otherpit, cf())
    protected[this] def newSubtask(p: SuperParIterator) = throw new UnsupportedOperationException
    override def split = {
      val fp = len / 2
      val sp = len - len / 2
      val pits = pit.psplitWithSignalling(fp, sp)
      val opits = otherpit.psplitWithSignalling(fp, sp)
      Seq(
        new Zip(fp, cf, pits(0), opits(0)),
        new Zip(sp, cf, pits(1), opits(1))
      )
    }
    override def merge(that: Zip[U, S, That]) = result = result combine that.result
  }

  protected[this] class Corresponds[S](corr: (T, S) => Boolean, protected[this] val pit: SeqSplitter[T], val otherpit: SeqSplitter[S])
  extends Accessor[Boolean, Corresponds[S]] {
    @volatile var result: Boolean = true
    def leaf(prev: Option[Boolean]) = if (!pit.isAborted) {
      result = pit.corresponds(corr)(otherpit)
      if (!result) pit.abort()
    }
    protected[this] def newSubtask(p: SuperParIterator) = throw new UnsupportedOperationException
    override def split = {
      val fp = pit.remaining / 2
      val sp = pit.remaining - fp
      for ((p, op) <- pit.psplitWithSignalling(fp, sp) zip otherpit.psplitWithSignalling(fp, sp)) yield new Corresponds(corr, p, op)
    }
    override def merge(that: Corresponds[S]) = result = result && that.result
    override def requiresStrictSplitters = true
  }
}
