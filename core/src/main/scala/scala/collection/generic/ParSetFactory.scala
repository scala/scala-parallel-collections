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
package generic

import scala.collection.parallel.Combiner
import scala.collection.parallel.ParSet
import scala.collection.parallel.ParSetLike

/**
 *  @define factoryInfo
 *    This object provides a set of operations needed to create `$Coll` values.
 */
abstract class ParSetFactory[CC[X] <: ParSet[X] with ParSetLike[X, CC, CC[X], ?] with GenericParTemplate[X, CC]]
  extends GenericParCompanion[CC] {
  def newBuilder[A]: Combiner[A, CC[A]] = newCombiner[A]

  def newCombiner[A]: Combiner[A, CC[A]]

  class GenericCanCombineFrom[B, A] extends CanCombineFrom[CC[B], A, CC[A]] {
    override def apply(from: CC[B]) = from.genericCombiner[A]
    override def apply() = newCombiner[A]
  }
}
