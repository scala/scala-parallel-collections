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

package scala.collection.parallel

import scala.collection.immutable.{OldHashMap, OldHashSet}
import scala.language.implicitConversions
import scala.{collection => sc}
import scala.collection.{immutable => sci, mutable => scm, concurrent => scc}

/** Extension methods for `.par` on sequential collections. */
object CollectionConverters {

  // TODO Use IsSeqLike, IsIterableLike, etc.
  // Iterable

  implicit class IterableIsParallelizable[A](private val coll: sc.Iterable[A]) extends AnyVal with sc.CustomParallelizable[A, ParIterable[A]] {
    def seq = coll
    override def par = coll match {
      case coll: sc.Set[A @unchecked] => new SetIsParallelizable(coll).par
      case coll: sc.Map[?, ?] => new MapIsParallelizable(coll).par.asInstanceOf[ParIterable[A]]
      case coll: sci.Iterable[A] => new ImmutableIterableIsParallelizable(coll).par
      case coll: scm.Iterable[A @unchecked] => new MutableIterableIsParallelizable(coll).par
      case _ => ParIterable.newCombiner[A].fromSequential(seq) // builds ParArray, same as for scm.Iterable
    }
  }

  implicit class MutableIterableIsParallelizable[A](private val coll: scm.Iterable[A]) extends AnyVal with sc.CustomParallelizable[A, mutable.ParIterable[A]] {
    def seq = coll
    override def par = coll match {
      case coll: scm.Seq[A] => new MutableSeqIsParallelizable(coll).par
      case coll: scm.Set[A] => new MutableSetIsParallelizable(coll).par
      case coll: scm.Map[?, ?] => new MutableMapIsParallelizable(coll).par.asInstanceOf[mutable.ParIterable[A]]
      case _ => mutable.ParIterable.newCombiner[A].fromSequential(seq) // builds ParArray
    }
  }

  implicit class ImmutableIterableIsParallelizable[A](private val coll: sci.Iterable[A]) extends AnyVal with sc.CustomParallelizable[A, immutable.ParIterable[A]] {
    def seq = coll
    override def par = coll match {
      case coll: sci.Seq[A] => new ImmutableSeqIsParallelizable(coll).par
      case coll: sci.Set[A @unchecked] => new ImmutableSetIsParallelizable(coll).par
      case coll: sci.Map[?, ?] => new ImmutableMapIsParallelizable(coll).par.asInstanceOf[immutable.ParIterable[A]]
      case _ => immutable.ParIterable.newCombiner[A].fromSequential(seq) // builds ParVector
    }
  }

  // Seq
  implicit def seqIsParallelizable[A](coll: sc.Seq[A]): sc.Parallelizable[A, ParSeq[A]] = coll match {
    case it: scm.Seq[A @unchecked] => new MutableSeqIsParallelizable(it)
    case it: sci.Seq[A] => new ImmutableSeqIsParallelizable(it)
    case _ => throw new IllegalArgumentException("Unexpected type "+coll.getClass.getName+" - every scala.collection.Seq must be a scala.collection.mutable.Seq or scala.collection.immutable.Seq")
  }

  implicit class MutableSeqIsParallelizable[A](private val coll: scm.Seq[A]) extends AnyVal with sc.CustomParallelizable[A, mutable.ParSeq[A]] {
    def seq = coll
    override def par = coll match {
      case coll: scm.ArraySeq[A] => new MutableArraySeqIsParallelizable(coll).par
      case coll: scm.ArrayBuffer[A] => new MutableArrayBufferIsParallelizable(coll).par
      case _ => mutable.ParSeq.newCombiner[A].fromSequential(seq)
    }
  }

  implicit class MutableArraySeqIsParallelizable[T](private val coll: scm.ArraySeq[T]) extends AnyVal with sc.CustomParallelizable[T, mutable.ParArray[T]] {
    def seq = coll
    override def par = mutable.ParArray.handoff(coll.array.asInstanceOf[Array[T]], coll.length)
  }

  implicit class MutableArrayBufferIsParallelizable[T](private val coll: scm.ArrayBuffer[T]) extends AnyVal with sc.CustomParallelizable[T, mutable.ParArray[T]] {
    def seq = coll
    override def par = mutable.ParArray.handoff[T](coll.array.asInstanceOf[Array[T]], coll.size)
  }

  // immutable.Seq

  implicit class ImmutableSeqIsParallelizable[A](private val coll: sci.Seq[A]) extends AnyVal with sc.CustomParallelizable[A, immutable.ParSeq[A]] {
    def seq = coll
    override def par = coll match {
      case coll: sci.Vector[?] => new VectorIsParallelizable(coll.asInstanceOf[sci.Vector[A]]).par
      case coll: sci.Range => new RangeIsParallelizable(coll).par.asInstanceOf[immutable.ParSeq[A]]
      case _ => immutable.ParSeq.newCombiner[A].fromSequential(seq)
    }
  }

  implicit class RangeIsParallelizable(private val coll: sci.Range) extends AnyVal with sc.CustomParallelizable[Int, immutable.ParRange] {
    def seq = coll
    override def par = new immutable.ParRange(coll)
  }

  implicit class VectorIsParallelizable[T](private val coll: sci.Vector[T]) extends AnyVal with sc.CustomParallelizable[T, immutable.ParVector[T]] {
    def seq = coll
    override def par = new immutable.ParVector(coll)
  }

  // Set

  implicit class SetIsParallelizable[A](private val coll: sc.Set[A]) extends AnyVal with sc.CustomParallelizable[A, ParSet[A]] {
    def seq = coll
    override def par = coll match {
      case coll: sci.Set[A] => new ImmutableSetIsParallelizable(coll).par
      case coll: scm.Set[A] => new MutableSetIsParallelizable(coll).par
      case _ => ParSet.newCombiner[A].fromSequential(seq)
    }
  }

  implicit class ImmutableSetIsParallelizable[A](private val coll: sci.Set[A]) extends AnyVal with sc.CustomParallelizable[A, immutable.ParSet[A]] {
    def seq = coll
    override def par = coll match {
      case coll: sci.HashSet[A] => new ImmutableHashSetIsParallelizable(coll).par
      case _ => immutable.ParSet.newCombiner[A].fromSequential(seq)
    }
  }

  implicit class MutableSetIsParallelizable[A](private val coll: scm.Set[A]) extends AnyVal with sc.CustomParallelizable[A, mutable.ParSet[A]] {
    def seq = coll
    override def par = coll match {
      case coll: scm.HashSet[A] => new MutableHashSetIsParallelizable(coll).par
      case _ => mutable.ParSet.newCombiner[A].fromSequential(seq)
    }
  }

  implicit class MutableHashSetIsParallelizable[T](private val coll: scm.HashSet[T]) extends AnyVal with sc.CustomParallelizable[T, mutable.ParHashSet[T]] {
    def seq = coll
    override def par = coll.to(mutable.ParHashSet)
  }

  implicit class ImmutableHashSetIsParallelizable[T](private val coll: sci.HashSet[T]) extends AnyVal with sc.CustomParallelizable[T, immutable.ParHashSet[T]] {
    def seq = coll
    override def par = immutable.ParHashSet.fromTrie(coll.to(OldHashSet)) // TODO Redesign immutable.ParHashSet so that conversion from sequential sci.HashSet takes constant time
  }

  // Map

  implicit class MapIsParallelizable[K, V](private val coll: sc.Map[K, V]) extends AnyVal with sc.CustomParallelizable[(K, V), ParMap[K, V]] {
    def seq = coll
    override def par = coll match {
      case coll: sci.Map[K, V] => new ImmutableMapIsParallelizable(coll).par
      case coll: scm.Map[K @unchecked, V @unchecked] => new MutableMapIsParallelizable(coll).par
      case _ => ParMap.newCombiner[K, V].fromSequential(seq)
    }
  }

  implicit class ImmutableMapIsParallelizable[K, V](private val coll: sci.Map[K, V]) extends AnyVal with sc.CustomParallelizable[(K, V), immutable.ParMap[K, V]] {
    def seq = coll
    override def par = coll match {
      case coll: sci.HashMap[K, V] => new ImmutableHashMapIsParallelizable(coll).par
      case _ => immutable.ParMap.newCombiner[K, V].fromSequential(seq)
    }
  }

  implicit class MutableMapIsParallelizable[K, V](private val coll: scm.Map[K, V]) extends AnyVal with sc.CustomParallelizable[(K, V), mutable.ParMap[K, V]] {
    def seq = coll
    override def par = coll match {
      case coll: scm.HashMap[K, V] => new MutableHashMapIsParallelizable(coll).par
      case coll: scc.TrieMap[K, V] => new ConcurrentTrieMapIsParallelizable(coll).par
      case _ => mutable.ParMap.newCombiner[K, V].fromSequential(seq)
    }
  }

  implicit class ImmutableHashMapIsParallelizable[K, V](private val coll: sci.HashMap[K, V]) extends AnyVal with sc.CustomParallelizable[(K, V), immutable.ParHashMap[K, V]] {
    def seq = coll
    override def par = immutable.ParHashMap.fromTrie(coll.to(OldHashMap)) // TODO Redesign immutable.ParHashMap so that conversion from sequential sci.HashMap takes constant time
  }

  implicit class MutableHashMapIsParallelizable[K, V](private val coll: scm.HashMap[K, V]) extends AnyVal with sc.CustomParallelizable[(K, V), mutable.ParHashMap[K, V]] {
    def seq = coll
    override def par = coll.to(mutable.ParHashMap) // TODO Redesign mutable.ParHashMap so that conversion from sequential scm.HashMap takes constant time
  }

  implicit class ConcurrentTrieMapIsParallelizable[K, V](private val coll: scc.TrieMap[K, V]) extends AnyVal with sc.CustomParallelizable[(K, V), mutable.ParTrieMap[K, V]] {
    def seq = coll
    override def par = new mutable.ParTrieMap(coll)
  }

  // Other

  implicit class ArrayIsParallelizable[T](private val a: Array[T]) extends AnyVal with sc.CustomParallelizable[T, mutable.ParArray[T]] {
    def seq = a // via ArrayOps
    override def par = mutable.ParArray.handoff(a)
  }
}
