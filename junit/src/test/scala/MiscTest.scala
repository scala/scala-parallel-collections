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

import collection._
import scala.collection.parallel.CollectionConverters._
import org.junit.Test
import org.junit.Assert._

import scala.collection.parallel.ParSeq

class MiscTest {
  @Test
  def si4459: Unit = {
    for (i <- 0 until 2000) {
      foo((0 until 10000).toSeq.par)
    }
  }

  def foo(arg: ParSeq[?]): String = arg.map(x => x).mkString(",")

  @Test
  def si4608: Unit = {
    ((1 to 100) sliding 10).toList.par.map{_.map{i => i * i}}.flatten
  }

  @Test
  def si4761: Unit = {
    val gs = for (x <- (1 to 5)) yield { if (x % 2 == 0) List(1) else List(1).par }
    assertEquals("Vector(1, 1, 1, 1, 1)", gs.flatten.toString)
    // Commented because `transpose` require its argument to be convertible to an `Iterable` whereas
    // we only have an `IterableOnce`
    // assertEquals("Vector(Vector(1, 1, 1, 1, 1))", gs.transpose.toString)

    val s = LazyList(Vector(1).par, Vector(2).par)
    assertEquals("List(1, 2)", s.flatten.toList.toString)
//    assertEquals("List(List(1, 2))", s.transpose.map(_.toList).toList.toString)
  }

  @Test
  def si4894: Unit = {
    val phs = parallel.mutable.ParHashSet[Int]()
    phs ++= 1 to 10
    for (i <- 1 to 10) assertTrue(phs(i))
    phs --= 1 to 10
    assertTrue(phs.isEmpty)

    val phm = parallel.mutable.ParHashMap[Int, Int]()
    phm ++= ((1 to 10) zip (1 to 10))
    for (i <- 1 to 10) assertTrue(phm(i) == i)
    phm --= 1 to 10
    assertTrue(phm.isEmpty)
  }

  @Test
  def si4895: Unit = {
    def checkPar(sz: Int): Unit = {
      import collection._
      val hs = mutable.HashSet[Int]() ++ (1 to sz)
      assertEquals((2 to (sz + 1)), hs.par.map(_ + 1).seq.toSeq.sorted)
    }

    for (i <- 0 until 100) checkPar(i)
    for (i <- 100 until 1000 by 50) checkPar(i)
    for (i <- 1000 until 10000 by 500) checkPar(i)
    for (i <- 10000 until 100000 by 5000) checkPar(i)
  }

  @Test
  def si5375: Unit = {
    val foos = (1 to 1000).toSeq
    try {
      foos.par.map(i => if (i % 37 == 0) throw new MultipleOf37Exception(i) else i)
      assert(false)
    } catch {
      case ex: MultipleOf37Exception =>
        assert(ex.getSuppressed.size > 0)
        assert(ex.getSuppressed.forall(_.isInstanceOf[MultipleOf37Exception]))
        assert(ex.i == 37)
        assert(ex.getSuppressed.map(_.asInstanceOf[MultipleOf37Exception].i).forall(_ % 37 == 0))
      case _: Throwable =>
        assert(false)
    }
    class MultipleOf37Exception(val i: Int) extends RuntimeException
  }

  @Test
  def si6052: Unit = {
    def seqarr(i: Int) = Array[Int]() ++ (0 until i)
    def pararr(i: Int) = seqarr(i).par

    def check[T](i: Int, f: Int => T): Unit = {
      val gseq = seqarr(i).toSeq.groupBy(f)
      val gpar = pararr(i).groupBy(f)
      // Note: sequential and parallel collections can not be compared with ''==''
      assertTrue(gseq.forall { case (k, vs) => gpar.get(k).exists(_.sameElements(vs)) })
      assertTrue(gpar.forall { case (k, vs) => gseq.get(k).exists(_.sameElements(vs)) })
    }

    for (i <- 0 until 20) check(i, _ > 0)
    for (i <- 0 until 20) check(i, _ % 2)
    for (i <- 0 until 20) check(i, _ % 4)
  }

  @Test
  def si6510: Unit = {
    val x = collection.parallel.mutable.ParArray.range(1,10) groupBy { _ % 2 } mapValues { _.size }
    assertTrue(x.isInstanceOf[parallel.ParMap[?, ?]])
    val y = collection.parallel.immutable.ParVector.range(1,10) groupBy { _ % 2 } mapValues { _.size }
    assertTrue(y.isInstanceOf[parallel.ParMap[?, ?]])
  }

  @Test
  def si6467: Unit = {
    assertEquals(List(1, 2, 3, 4).foldLeft(new java.lang.StringBuffer)(_ append _).toString, "1234")
    assertEquals(List(1, 2, 3, 4).par.aggregate(new java.lang.StringBuffer)(_ append _, _ append _).toString, "1234")
    assertEquals(Seq(0 until 100: _*).foldLeft(new java.lang.StringBuffer)(_ append _).toString, (0 until 100).mkString)
    assertEquals(Seq(0 until 100: _*).par.aggregate(new java.lang.StringBuffer)(_ append _, _ append _).toString, (0 until 100).mkString)
  }

  @Test
  def si6908: Unit = {
    val set = collection.mutable.Set("1", null, "3").par
    assert( set exists (_ eq null) )
  }

  @Test
  def si7498: Unit = {
    class Collision(val idx: Int) {
      override def hashCode = idx % 10
    }
    val tm = scala.collection.concurrent.TrieMap[Collision, Unit]()
    for (i <- 0 until 1000) tm(new Collision(i)) = ()
    tm.par.foreach(kv => ())
  }

  @Test
  def si8955: Unit = {
    def test(): Unit =
      scala.collection.parallel.immutable.ParSet[Int]((1 to 10000): _*) foreach (x => ()) // hangs non deterministically
    for (i <- 1 to 2000) test()
  }

  @Test
  def si8072: Unit = {
    testutil.ShouldNotTypecheck(
      """
        import collection.parallel._
        val x = List(1,2)
        val y = x.ifParSeq[Int](throw new Exception).otherwise(0)  // Shouldn't compile
        val z = x.toParArray
      """, "value ifParSeq is not a member of List\\[Int\\]")
  }
}
