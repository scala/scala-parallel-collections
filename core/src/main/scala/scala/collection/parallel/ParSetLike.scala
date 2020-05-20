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

import scala.collection.{Set, SetOps}

/** A template trait for parallel sets. This trait is mixed in with concrete
 *  parallel sets to override the representation type.
 *
 *  $sideeffects
 *
 *  @tparam T    the element type of the set
 *  @define Coll `ParSet`
 *  @define coll parallel set
 */
trait ParSetLike[T,
                 +CC[X] <: ParIterable[X],
                 +Repr <: ParSet[T],
                 +Sequential <: Set[T] with SetOps[T, Set, Sequential]]
extends ParIterableLike[T, CC, Repr, Sequential]
  with (T => Boolean)
  with Equals
{ self =>

  // --- Members previously inherited from GenSetLike
  def contains(elem: T): Boolean
  final def apply(elem: T): Boolean = contains(elem)
  def +(elem: T): Repr
  def -(elem: T): Repr

  /** Computes the intersection between this set and another set.
    *
    *  @param   that  the set to intersect with.
    *  @return  a new set consisting of all elements that are both in this
    *  set and in the given set `that`.
    */
  def intersect(that: ParSet[T]): Repr = this filter that
  def intersect(that: Set[T]): Repr = this filter that

  /** Computes the intersection between this set and another set.
    *
    *  '''Note:'''  Same as `intersect`.
    *  @param   that  the set to intersect with.
    *  @return  a new set consisting of all elements that are both in this
    *  set and in the given set `that`.
    */
  def &(that: ParSet[T]): Repr = this intersect that
  def &(that: Set[T]): Repr = this intersect that

  /** Computes the union between this set and another set.
    *
    *  '''Note:'''  Same as `union`.
    *  @param   that  the set to form the union with.
    *  @return  a new set consisting of all elements that are in this
    *  set or in the given set `that`.
    */
  def | (that: ParSet[T]): Repr = this union that
  def | (that: Set[T]): Repr = this union that

  /** The difference of this set and another set.
    *
    *  '''Note:'''  Same as `diff`.
    *  @param that the set of elements to exclude.
    *  @return     a set containing those elements of this
    *              set that are not also contained in the given set `that`.
    */
  def &~(that: ParSet[T]): Repr = this diff that
  def &~(that: Set[T]): Repr = this diff that

  /** Tests whether this set is a subset of another set.
    *
    *  @param that  the set to test.
    *  @return     `true` if this set is a subset of `that`, i.e. if
    *              every element of this set is also an element of `that`.
    */
  def subsetOf(that: ParSet[T]): Boolean = this.forall(that)

  /** Compares this set with another object for equality.
    *
    *  '''Note:''' This operation contains an unchecked cast: if `that`
    *        is a set, it will assume with an unchecked cast
    *        that it has the same element type as this set.
    *        Any subsequent ClassCastException is treated as a `false` result.
    *  @param that the other object
    *  @return     `true` if `that` is a set which contains the same elements
    *              as this set.
    */
  override def equals(that: Any): Boolean = that match {
    case that: ParSet[_] =>
      (this eq that) ||
        (that canEqual this) &&
          (this.size == that.size) &&
          (try this subsetOf that.asInstanceOf[ParSet[T]]
          catch { case ex: ClassCastException => false })
    case _ =>
      false
  }

  // Careful! Don't write a Set's hashCode like:
  //    override def hashCode() = this map (_.hashCode) sum
  // Calling map on a set drops duplicates: any hashcode collisions would
  // then be dropped before they can be added.
  // Hash should be symmetric in set entries, but without trivial collisions.
  override def hashCode()= scala.util.hashing.MurmurHash3.unorderedHash(seq, "ParSet".hashCode)

  def canEqual(other: Any): Boolean = true
  // ---

  def empty: Repr

  // note: should not override toSet (could be mutable)

  def union(that: Set[T]): Repr = sequentially {
    _ union that
  }

  def union(that: ParSet[T]): Repr = sequentially {
    _ union that.seq
  }

  def diff(that: Set[T]): Repr = sequentially {
    _ diff that
  }

  def diff(that: ParSet[T]): Repr = sequentially {
    _ diff that.seq
  }
}
