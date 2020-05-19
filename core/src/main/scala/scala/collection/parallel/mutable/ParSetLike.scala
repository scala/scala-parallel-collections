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
package collection
package parallel.mutable

import scala.collection.mutable.Cloneable
import scala.collection.mutable.Growable
import scala.collection.mutable.Shrinkable

/** A template trait for mutable parallel sets. This trait is mixed in with concrete
 *  parallel sets to override the representation type.
 *
 *  $sideeffects
 *
 *  @tparam T    the element type of the set
 *  @define Coll `mutable.ParSet`
 *  @define coll mutable parallel set
 */
trait ParSetLike[T,
                 +CC[X] <: ParIterable[X],
                 +Repr <: ParSetLike[T, CC, Repr, Sequential] with ParSet[T],
                 +Sequential <: mutable.Set[T] with mutable.SetOps[T, mutable.Set, Sequential]]
extends scala.collection.parallel.ParIterableLike[T, CC, Repr, Sequential]
   with scala.collection.parallel.ParSetLike[T, CC, Repr, Sequential]
   with Growable[T]
   with Shrinkable[T]
   with Cloneable[Repr]
{
self =>
  override def knownSize: Int = -1

  override def empty: Repr

  def addOne(elem: T): this.type

  def subtractOne(elem: T): this.type

  def +(elem: T) = this.clone() += elem

  def -(elem: T) = this.clone() -= elem

  override def clone(): Repr = empty ++= this
  // note: should not override toSet
}
