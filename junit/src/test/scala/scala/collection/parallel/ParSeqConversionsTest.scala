package scala.collection.parallel

import CollectionConverters._

import org.junit.Test
import org.junit.Assert._

// test conversions between collections
// based on run/pc-conversions.scala partest
class ParSeqConversionsTest {

  @Test
  def testConversions {
    // seq conversions
    assertSeq(scala.collection.parallel.mutable.ParArray(1, 2, 3))
    assertSeq(scala.collection.parallel.mutable.ParHashMap(1 -> 2, 2 -> 3))
    assertSeq(scala.collection.parallel.mutable.ParHashSet(1, 2, 3))
    assertSeq(scala.collection.parallel.immutable.ParRange(1, 50, 1, false))
    assertSeq(scala.collection.parallel.immutable.ParHashMap(1 -> 2, 2 -> 4))
    assertSeq(scala.collection.parallel.immutable.ParHashSet(1, 2, 3))

    // par conversions
    assertPar(Array(1, 2, 3))
    assertPar(scala.collection.mutable.ArrayBuffer(1, 2, 3))
    assertPar(scala.collection.mutable.ArraySeq(1, 2, 3))
    assertPar(scala.collection.mutable.WrappedArray.make[Int](Array(1, 2, 3)))
    assertPar(scala.collection.mutable.HashMap(1 -> 1, 2 -> 2))
    assertPar(scala.collection.mutable.HashSet(1, 2, 3))
    assertPar(scala.collection.immutable.Range(1, 50, 1))
    assertPar(scala.collection.immutable.HashMap(1 -> 1, 2 -> 2))
    assertPar(scala.collection.immutable.HashSet(1, 2, 3))

    // par.to* and to*.par tests
    assertToPar(List(1 -> 1, 2 -> 2, 3 -> 3))
    assertToPar(Stream(1 -> 1, 2 -> 2))
    assertToPar(Array(1 -> 1, 2 -> 2))
    assertToPar(scala.collection.mutable.PriorityQueue(1 -> 1, 2 -> 2, 3 -> 3))
    assertToPar(scala.collection.mutable.ArrayBuffer(1 -> 1, 2 -> 2))
    assertToPar(scala.collection.mutable.ArraySeq(1 -> 3))
    assertToPar(scala.collection.mutable.WrappedArray.make[(Int, Int)](Array(1 -> 3)))
    assertToPar(scala.collection.mutable.HashMap(1 -> 3))
    assertToPar(scala.collection.mutable.HashSet(1 -> 3))
    assertToPar(scala.collection.immutable.HashMap(1 -> 3))
    assertToPar(scala.collection.immutable.HashSet(1 -> 3))
    assertToPar(scala.collection.parallel.mutable.ParArray(1 -> 1, 2 -> 2, 3 -> 3))
    assertToPar(scala.collection.parallel.mutable.ParHashMap(1 -> 2))
    assertToPar(scala.collection.parallel.mutable.ParHashSet(1 -> 2))
    assertToPar(scala.collection.parallel.immutable.ParHashMap(1 -> 2))
    assertToPar(scala.collection.parallel.immutable.ParHashSet(1 -> 3))

    assertToParWoMap(scala.collection.immutable.Range(1, 10, 2))

    // seq and par again conversions)
    assertSeqPar(scala.collection.parallel.mutable.ParArray(1, 2, 3))
  }

  def assertSeqPar[T](pc: scala.collection.parallel.ParIterable[T]) = pc.seq.par == pc

  def assertSeq[T](pc: scala.collection.parallel.ParIterable[T]) = assertTrue(pc.seq == pc)

  def assertPar[T, P](xs: scala.collection.GenIterable[T]) = assertTrue(xs == xs.par)

  def assertToPar[K, V](xs: scala.collection.GenTraversable[(K, V)]) {
    xs match {
      case _: Seq[_] =>
        assertTrue(xs.toIterable.par == xs)
        assertTrue(xs.par.toIterable == xs)
      case _ =>
    }

    assertTrue(xs.toSeq.par == xs.toSeq)
    assertTrue(xs.par.toSeq == xs.toSeq)

    assertTrue(xs.toSet.par == xs.toSet)
    assertTrue(xs.par.toSet == xs.toSet)

    assertTrue(xs.toMap.par == xs.toMap)
    assertTrue(xs.par.toMap == xs.toMap)
  }

  def assertToParWoMap[T](xs: scala.collection.GenSeq[T]) {
    assertTrue(xs.toIterable.par == xs.toIterable)
    assertTrue(xs.par.toIterable == xs.toIterable)

    assertTrue(xs.toSeq.par == xs.toSeq)
    assertTrue(xs.par.toSeq == xs.toSeq)

    assertTrue(xs.toSet.par == xs.toSet)
    assertTrue(xs.par.toSet == xs.toSet)
  }

}
