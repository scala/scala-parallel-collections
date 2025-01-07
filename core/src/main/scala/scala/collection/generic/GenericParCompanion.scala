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
package generic

import scala.collection.parallel.Combiner
import scala.collection.parallel.ParIterable
import scala.collection.parallel.ParMap
import scala.language.implicitConversions

/** A template class for companion objects of parallel collection classes.
 *  They should be mixed in together with `GenericCompanion` type.
 *
 *  @define Coll `ParIterable`
 *  @tparam CC   the type constructor representing the collection class
 *  @since 2.8
 */
trait GenericParCompanion[+CC[X] <: ParIterable[X]] {

  // `empty` and `apply` were previously inherited from `GenericCompanion` but this class
  // has been removed in 2.13. I’ve copied their old implementation here.

  /** An empty collection of type `$Coll[A]`
    *  @tparam A      the type of the ${coll}'s elements
    */
  def empty[A]: CC[A] = newBuilder[A].result()

  /** Creates a $coll with the specified elements.
    *  @tparam A      the type of the ${coll}'s elements
    *  @param elems  the elements of the created $coll
    *  @return a new $coll with elements `elems`
    */
  def apply[A](elems: A*): CC[A] = {
    if (elems.isEmpty) empty[A]
    else {
      val b = newBuilder[A]
      b ++= elems
      b.result()
    }
  }

  /** The default builder for $Coll objects.
   */
  def newBuilder[A]: Combiner[A, CC[A]]

  /** The parallel builder for $Coll objects.
   */
  def newCombiner[A]: Combiner[A, CC[A]]

  implicit def toFactory[A]: Factory[A, CC[A]] = GenericParCompanion.toFactory(this)

}


// TODO Specialize `Factory` with parallel collection creation methods so that the `xs.to(ParArray)` syntax
// does build the resulting `ParArray` in parallel
object GenericParCompanion {
  /**
    * Implicit conversion for converting any `ParFactory` into a sequential `Factory`.
    * This provides supports for the `to` conversion method (eg, `xs.to(ParArray)`).
    */
  implicit def toFactory[A, CC[X] <: ParIterable[X]](parFactory: GenericParCompanion[CC]): Factory[A, CC[A]] =
    new ToFactory(parFactory)

  @SerialVersionUID(3L)
  private class ToFactory[A, CC[X] <: ParIterable[X]](parFactory: GenericParCompanion[CC])
    extends Factory[A, CC[A]] with Serializable{
      def fromSpecific(it: IterableOnce[A]): CC[A] = (parFactory.newBuilder[A] ++= it).result()
      def newBuilder: mutable.Builder[A, CC[A]] = parFactory.newBuilder
  }

}

trait GenericParMapCompanion[+CC[P, Q] <: ParMap[P, Q]] {

  def newCombiner[P, Q]: Combiner[(P, Q), CC[P, Q]]

  implicit def toFactory[K, V]: Factory[(K, V), CC[K, V]] = GenericParMapCompanion.toFactory(this)

}

object GenericParMapCompanion {
  /**
    * Implicit conversion for converting any `ParFactory` into a sequential `Factory`.
    * This provides supports for the `to` conversion method (eg, `xs.to(ParMap)`).
    */
  implicit def toFactory[K, V, CC[X, Y] <: ParMap[X, Y]](
    parFactory: GenericParMapCompanion[CC]
  ): Factory[(K, V), CC[K, V]] =
    new ToFactory[K, V, CC](parFactory)

  @SerialVersionUID(3L)
  private class ToFactory[K, V, CC[X, Y] <: ParMap[X, Y]](
    parFactory: GenericParMapCompanion[CC]
  ) extends Factory[(K, V), CC[K, V]] with Serializable {
    def fromSpecific(it: IterableOnce[(K, V)]): CC[K, V] = (parFactory.newCombiner[K, V] ++= it).result()
    def newBuilder: mutable.Builder[(K, V), CC[K, V]] = parFactory.newCombiner
  }

}
