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
package parallel.immutable

import scala.collection.generic.{GenericParTemplate, CanCombineFrom, ParFactory}
import scala.collection.parallel.ParSeqLike
import scala.collection.parallel.Combiner
import scala.collection.parallel.SeqSplitter
import mutable.ArrayBuffer
import immutable.Vector
import immutable.VectorBuilder
import immutable.VectorIterator

/** Immutable parallel vectors, based on vectors.
 *
 *  $paralleliterableinfo
 *
 *  $sideeffects
 *
 *  @tparam T    the element type of the vector
 *
 *  @see  [[http://docs.scala-lang.org/overviews/parallel-collections/concrete-parallel-collections.html#parallel_vector Scala's Parallel Collections Library overview]]
 *  section on `ParVector` for more information.
 *
 *  @define Coll `immutable.ParVector`
 *  @define coll immutable parallel vector
 */
class ParVector[+T](private[this] val vector: Vector[T])
extends ParSeq[T]
   with GenericParTemplate[T, ParVector]
   with ParSeqLike[T, ParVector, ParVector[T], Vector[T]]
   with Serializable
{
  override def companion = ParVector

  def this() = this(Vector())

  def apply(idx: Int) = vector.apply(idx)

  def length = vector.length
  override def knownSize = vector.knownSize

  def splitter: SeqSplitter[T] = {
    val pit = new ParVectorIterator(vector.startIndex, vector.endIndex)
    vector.initIterator(pit)
    pit
  }

  override def seq: Vector[T] = vector

  override def toVector: Vector[T] = vector

  // TODO Implement ParVectorIterator without extending VectorIterator, which will eventually
  // become private final. Inlining the contents of the current VectorIterator is not as easy
  // as it seems because it relies a lot on Vector internals.
  // Duplicating the whole Vector data structure seems to be the safest way, but we will loose
  // interoperability with the standard Vector.
  class ParVectorIterator(_start: Int, _end: Int) extends VectorIterator[T](_start, _end) with SeqSplitter[T] {
    def remaining: Int = remainingElementCount
    def dup: SeqSplitter[T] = (new ParVector(remainingVector)).splitter
    def split: scala.collection.immutable.Seq[ParVectorIterator] = {
      val rem = remaining
      if (rem >= 2) psplit(rem / 2, rem - rem / 2)
      else scala.collection.immutable.Seq(this)
    }
    def psplit(sizes: Int*): scala.Seq[ParVectorIterator] = {
      var remvector = remainingVector
      val splitted = List.newBuilder[Vector[T]]
      for (sz <- sizes) {
        splitted += remvector.take(sz)
        remvector = remvector.drop(sz)
      }
      splitted.result().map(v => new ParVector(v).splitter.asInstanceOf[ParVectorIterator])
    }
  }
}

/** $factoryInfo
 *  @define Coll `immutable.ParVector`
 *  @define coll immutable parallel vector
 */
object ParVector extends ParFactory[ParVector] {
  implicit def canBuildFrom[S, T]: CanCombineFrom[ParVector[S], T, ParVector[T]] =
    new GenericCanCombineFrom[S, T]

  def newBuilder[T]: Combiner[T, ParVector[T]] = newCombiner[T]

  def newCombiner[T]: Combiner[T, ParVector[T]] = new LazyParVectorCombiner[T] // was: with EPC[T, ParVector[T]]
}

private[immutable] class LazyParVectorCombiner[T] extends Combiner[T, ParVector[T]] {
//self: EnvironmentPassingCombiner[T, ParVector[T]] =>
  var sz = 0
  val vectors = new ArrayBuffer[VectorBuilder[T]] += new VectorBuilder[T]

  def size: Int = sz

  def addOne(elem: T): this.type = {
    vectors.last += elem
    sz += 1
    this
  }

  def clear() = {
    vectors.clear()
    vectors += new VectorBuilder[T]
    sz = 0
  }

  def result(): ParVector[T] = {
    val rvb = new VectorBuilder[T]
    for (vb <- vectors) {
      rvb ++= vb.result()
    }
    new ParVector(rvb.result())
  }

  def combine[U <: T, NewTo >: ParVector[T]](other: Combiner[U, NewTo]) = if (other eq this) this else {
    val that = other.asInstanceOf[LazyParVectorCombiner[T]]
    sz += that.sz
    vectors ++= that.vectors
    this
  }
}
