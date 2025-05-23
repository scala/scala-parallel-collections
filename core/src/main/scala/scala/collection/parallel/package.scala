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

import scala.collection.parallel.mutable.ParArray
import scala.collection.mutable.UnrolledBuffer
import scala.language.implicitConversions

/** Package object for parallel collections.
 */
package object parallel {
  /* constants */
  val MIN_FOR_COPY = 512
  val CHECK_RATE = 512
  val SQRT2 = math.sqrt(2)
  val availableProcessors = java.lang.Runtime.getRuntime.availableProcessors

  /* functions */

  /** Computes threshold from the size of the collection and the parallelism level.
   */
  def thresholdFromSize(sz: Int, parallelismLevel: Int) = {
    val p = parallelismLevel
    if (p > 1) 1 + sz / (8 * p)
    else sz
  }

  val defaultTaskSupport: TaskSupport = new ExecutionContextTaskSupport

  def setTaskSupport[Coll](c: Coll, t: TaskSupport): Coll = {
    c match {
      case pc: ParIterableLike[?, ?, ?, ?] => pc.tasksupport = t
      case _ => // do nothing
    }
    c
  }

  /** Adds toParArray method to collection classes. */
  implicit class CollectionsHaveToParArray[C, T](c: C)(implicit asGto: C => scala.collection.IterableOnce[T]) {
    def toParArray = {
      val t = asGto(c)
      if (t.isInstanceOf[ParArray[?]]) t.asInstanceOf[ParArray[T]]
      else {
        val it = t.iterator
        val cb = mutable.ParArrayCombiner[T]()
        while (it.hasNext) cb += it.next()
        cb.result()
      }
    }
  }
}


package parallel {
  /** Implicit conversions used in the implementation of parallel collections. */
  private[collection] object ParallelCollectionImplicits {
    implicit def traversable2ops[T](t: scala.collection.IterableOnce[T]): TraversableOps[T] = new TraversableOps[T] {
      def isParallel = t.isInstanceOf[Parallel]
      def isParIterable = t.isInstanceOf[ParIterable[?]]
      def asParIterable = t.asInstanceOf[ParIterable[T]]
      def isParSeq = t.isInstanceOf[ParSeq[?]]
      def asParSeq = t.asInstanceOf[ParSeq[T]]
      def ifParSeq[R](isbody: ParSeq[T] => R) = new Otherwise[R] {
        def otherwise(notbody: => R) = if (isParallel) isbody(asParSeq) else notbody
      }
    }
  }

  trait TraversableOps[T] {
    trait Otherwise[R] {
      def otherwise(notbody: => R): R
    }

    def isParallel: Boolean
    def isParIterable: Boolean
    def asParIterable: ParIterable[T]
    def isParSeq: Boolean
    def asParSeq: ParSeq[T]
    def ifParSeq[R](isbody: ParSeq[T] => R): Otherwise[R]
  }

  /* classes */

  trait CombinerFactory[U, Repr] {
    /** Provides a combiner used to construct a collection. */
    def apply(): Combiner[U, Repr]
    /** The call to the `apply` method can create a new combiner each time.
     *  If it does, this method returns `false`.
     *  The same combiner factory may be used each time (typically, this is
     *  the case for concurrent collections, which are thread safe).
     *  If so, the method returns `true`.
     */
    def doesShareCombiners: Boolean
  }

  /** A helper iterator for iterating very small array buffers.
   *  Automatically forwards the signal delegate when splitting.
   */
  private[parallel] class BufferSplitter[T]
  (private val buffer: scala.collection.mutable.ArrayBuffer[T], private var index: Int, private val until: Int, _sigdel: scala.collection.generic.Signalling)
  extends IterableSplitter[T] {
    signalDelegate = _sigdel
    def hasNext = index < until
    def next() = {
      val r = buffer(index)
      index += 1
      r
    }
    def remaining = until - index
    def dup = new BufferSplitter(buffer, index, until, signalDelegate)
    def split: scala.Seq[IterableSplitter[T]] = if (remaining > 1) {
      val divsz = (until - index) / 2
      scala.Seq(
        new BufferSplitter(buffer, index, index + divsz, signalDelegate),
        new BufferSplitter(buffer, index + divsz, until, signalDelegate)
      )
    } else scala.Seq(this)
    private[parallel] override def debugInformation = {
      buildString {
        append =>
        append("---------------")
        append("Buffer iterator")
        append("buffer: " + buffer)
        append("index: " + index)
        append("until: " + until)
        append("---------------")
      }
    }
  }

  /** A helper combiner which contains an array of buckets. Buckets themselves
   *  are unrolled linked lists. Some parallel collections are constructed by
   *  sorting their result set according to some criteria.
   *
   *  A reference `buckets` to buckets is maintained. Total size of all buckets
   *  is kept in `sz` and maintained whenever 2 bucket combiners are combined.
   *
   *  Clients decide how to maintain these by implementing `+=` and `result`.
   *  Populating and using the buckets is up to the client. While populating them,
   *  the client should update `sz` accordingly. Note that a bucket is by default
   *  set to `null` to save space - the client should initialize it.
   *  Note that in general the type of the elements contained in the buckets `Buck`
   *  doesn't have to correspond to combiner element type `Elem`.
   *
   *  This class simply gives an efficient `combine` for free - it chains
   *  the buckets together. Since the `combine` contract states that the receiver (`this`)
   *  becomes invalidated, `combine` reuses the receiver and returns it.
   *
   *  Methods `beforeCombine` and `afterCombine` are called before and after
   *  combining the buckets, respectively, given that the argument to `combine`
   *  is not `this` (as required by the `combine` contract).
   *  They can be overridden in subclasses to provide custom behaviour by modifying
   *  the receiver (which will be the return value).
   */
  private[parallel] abstract class BucketCombiner[-Elem, +To, Buck, +CombinerType <: BucketCombiner[Elem, To, Buck, CombinerType]]
  (private val bucketnumber: Int)
  extends Combiner[Elem, To] {
  //self: EnvironmentPassingCombiner[Elem, To] =>
    protected var buckets: Array[UnrolledBuffer[Buck]] = new Array[UnrolledBuffer[Buck]](bucketnumber)
    protected var sz: Int = 0

    def size = sz

    def clear() = {
      buckets = new Array[UnrolledBuffer[Buck]](bucketnumber)
      sz = 0
    }

    def beforeCombine[N <: Elem, NewTo >: To](other: Combiner[N, NewTo]): Unit = {}

    def afterCombine[N <: Elem, NewTo >: To](other: Combiner[N, NewTo]): Unit = {}

    def combine[N <: Elem, NewTo >: To](other: Combiner[N, NewTo]): Combiner[N, NewTo] = {
      if (this eq other) this
      else other match {
        case _: BucketCombiner[?, ?, ?, ?] =>
          beforeCombine(other)
          val that = other.asInstanceOf[BucketCombiner[Elem, To, Buck, CombinerType]]

          var i = 0
          while (i < bucketnumber) {
            if (buckets(i) eq null)
              buckets(i) = that.buckets(i)
            else if (that.buckets(i) ne null)
              buckets(i) concat that.buckets(i)

            i += 1
          }
          sz = sz + that.size
          afterCombine(other)
          this
        case _ =>
          sys.error("Unexpected combiner type.")
      }
    }
  }
}
