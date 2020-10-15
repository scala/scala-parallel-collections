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

package scala.collection.parallel

import org.scalacheck._
import org.scalacheck.Gen
import org.scalacheck.Gen._
import org.scalacheck.Prop._
import org.scalacheck.Properties

import scala.language.higherKinds
import scala.collection._
import scala.collection.parallel._

abstract class ParallelIterableCheck[T](collName: String) extends Properties(collName) with Operators[T] {
  type CollType <: ParIterable[T]

  def values: Seq[Gen[T]]
  def ofSize(vals: Seq[Gen[T]], sz: Int): Iterable[T]
  def fromIterable(t: Iterable[T]): CollType
  def hasStrictOrder: Boolean


  def instances(vals: Seq[Gen[T]]): Gen[Iterable[T]] = oneOf(
    sized(
      sz =>
      ofSize(vals, sz)
    ),
    for (sz <- choose(1000, 2000)) yield ofSize(vals, sz),
    for (sz <- choose(4000, 4001)) yield ofSize(vals, sz),
    for (sz <- choose(10000, 10001)) yield ofSize(vals, sz)
  )

  // used to check if constructed collection is valid
  def checkDataStructureInvariants(orig: Iterable[T], cf: AnyRef) = {
    // can be overridden in subclasses
    true
  }

  def printDataStructureDebugInfo(cf: AnyRef): Unit = {
    // can be overridden in subclasses
  }

  val rnd = new scala.util.Random

  def sample(gen: Gen[T]): T = {
    var s = gen.sample
    while (s == None) s = gen.sample
    s.get
  }

  def sampleValue: T = sample(values(rnd.nextInt(values.length)))

  def collectionPairs = for (inst <- instances(values)) yield (inst, fromIterable(inst))

  def collectionPairsWithLengths = for (inst <- instances(values); s <- choose(0, inst.size))
    yield (inst, fromIterable(inst), s)

  def collectionPairsWith2Indices = for (
      inst <- instances(values);
      f <- choose(0, inst.size);
      s <- choose(0, inst.size))
    yield (inst, fromIterable(inst), f, s)

  def collectionTriplets = for (inst <- instances(values);
      updStart <- choose(0, inst.size); howMany <- choose(0, inst.size)) yield {
    val modif = inst.toSeq.patch(updStart, inst.toSeq, howMany)
    (inst, fromIterable(inst), modif)
  }

  def areEqual(t1: Iterable[T], t2: ParIterable[T]) = if (hasStrictOrder) {
    t1.iterator.sameElements(t2) && t2.sameElements(t1)
  } else (t1, t2) match { // it is slightly delicate what `equal` means if the order is not strict
    case (m1: Map[_, _], m2: ParMap[_, _]) =>
      val am1: Map[Any, Any] = m1.asInstanceOf[Map[Any, Any]]
      val am2: ParMap[Any, Any] = m2.asInstanceOf[ParMap[Any, Any]]
      am1.forall { case (k, v) => am2.get(k).contains(v) } && am2.forall { case (k, v) => am1.get(k).contains(v) }
    case _ =>
      val s1 = t1.toSet
      val s2 = t2.toSet
      s1.forall(s2) && s2.forall(s1)
  }

  def printDebugInfo[A, CC[X] <: ParIterable[X], C <: ParIterable[A], S <: Iterable[A] with IterableOps[A, Iterable, S]](coll: ParIterableLike[A, CC, C, S]): Unit = {
    println("Collection debug info: ")
    coll.printDebugBuffer()
    println("Task debug info: ")
    println(coll.tasksupport.debugMessages.mkString("\n"))
  }

  def printComparison(t: Iterable[_], coll: ParIterable[_], tf: Iterable[_], cf: ParIterable[_], ind: Int): Unit = {
    printDebugInfo(coll)
    println("Operator: " + ind)
    println("sz: " + t.size)
    println(t)
    println()
    println("sz: " + coll.size)
    println(coll)
    println("transformed to:")
    println()
    println("size: " + tf.size)
    println(tf)
    println()
    println("size: " + cf.size)
    println(cf)
    println()
    println("tf sameElements cf - " + (tf.iterator sameElements  cf))
    println("cf sameElements tf - " + (cf.iterator sameElements tf))
  }

  property("reductions must be equal for assoc. operators") = forAllNoShrink(collectionPairs) { case (t, coll) =>
    if (t.size != 0) {
      val results = for ((op, ind) <- reduceOperators.zipWithIndex) yield {
        val tr = t.reduceLeft(op)
        val cr = coll.reduce(op)
        if (tr != cr) {
          println("from: " + t)
          println("and: " + coll)
          println("reducing with " + ind)
          println(tr)
          println(cr)
        }
        ("op index: " + ind) |: tr == cr
      }
      results.reduceLeft(_ && _)
    } else "has size 0" |: true
  }

  property("counts must be equal") = forAllNoShrink(collectionPairs) { case (t, coll) =>
    val results = for ((pred, ind) <- countPredicates.zipWithIndex) yield {
      val tc = t.count(pred)
      val cc = coll.count(pred)
      if (tc != cc) {
        println("from: " + t + " - size: " + t.size)
        println("and: " + coll + " - size: " + coll.toList.size)
        println(tc)
        println(cc)
        printDebugInfo(coll)
      }
      ("op index: " + ind) |: tc == cc
    }
    results.reduceLeft(_ && _)
  }

  property("forall must be equal") = forAllNoShrink(collectionPairs) { case (t, coll) =>
    val results = for ((pred, ind) <- forallPredicates.zipWithIndex)
      yield ("op index: " + ind) |: t.forall(pred) == coll.forall(pred)
    results.reduceLeft(_ && _)
  }

  property("exists must be equal") = forAllNoShrink(collectionPairs) { case (t, coll) =>
    val results = for ((pred, ind) <- existsPredicates.zipWithIndex)
      yield ("op index: " + ind) |: t.exists(pred) == coll.exists(pred)
    results.reduceLeft(_ && _)
  }

  property("both must find or not find an element") = forAllNoShrink(collectionPairs) { case (t, coll) =>
    val results = for ((pred, ind) <- findPredicates.zipWithIndex) yield {
      val ft = t.find(pred)
      val fcoll = coll.find(pred)
      ("op index: " + ind) |: ((ft == None && fcoll == None) || (ft != None && fcoll != None))
    }
    results.reduceLeft(_ && _)
  }

  property("mappings must be equal") = forAllNoShrink(collectionPairs) { case (t, coll) =>
    val results = for ((f, ind) <- mapFunctions.zipWithIndex) yield {
      val ms = t.map(f)
      val mp = coll.map(f)
      val invs = checkDataStructureInvariants(ms, mp)
      if (!areEqual(ms, mp) || !invs) {
        println(t)
        println(coll)
        println("mapped to: ")
        println(ms)
        println(mp)
        println("sizes: ")
        println(ms.size)
        println(mp.size)
        println("valid: " + invs)
      }
      ("op index: " + ind) |: (areEqual(ms, mp) && invs)
    }
    results.reduceLeft(_ && _)
  }

  property("collects must be equal") = forAllNoShrink(collectionPairs) { case (t, coll) =>
    val results = for ((f, ind) <- partialMapFunctions.zipWithIndex) yield {
      val ps = t.collect(f)
      val pp = coll.collect(f)
      if (!areEqual(ps, pp)) {
        println(t)
        println(coll)
        println("collected to: ")
        println(ps)
        println(pp)
      }
      ("op index: " + ind) |: areEqual(ps, pp)
    }
    results.reduceLeft(_ && _)
  }

  property("flatMaps must be equal") = forAllNoShrink(collectionPairs) { case (t, coll) =>
    (for ((f, ind) <- flatMapFunctions.zipWithIndex)
      yield ("op index: " + ind) |: areEqual(t.flatMap(f), coll.flatMap(f))).reduceLeft(_ && _)
  }

  property("filters must be equal") = forAllNoShrink(collectionPairs) { case (t, coll) =>
    (for ((p, ind) <- filterPredicates.zipWithIndex) yield {
      val tf = t.filter(p)
      val cf = coll.filter(p)
      val invs = checkDataStructureInvariants(tf, cf)
      if (!areEqual(tf, cf) || !invs) {
        printDebugInfo(coll)
        println("Operator: " + ind)
        println("sz: " + t.size)
        println(t)
        println()
        println("sz: " + coll.size)
        println(coll)
        println()
        println("filtered to:")
        println()
        println(cf)
        println()
        println(tf)
        println()
        println("areEqual(tf, cf) - " + areEqual(tf, cf))
        printDataStructureDebugInfo(cf)
        println("valid: " + invs)
      }
      ("op index: " + ind) |: (areEqual(tf, cf) && invs)
    }).reduceLeft(_ && _)
  }

  property("filterNots must be equal") = forAllNoShrink(collectionPairs) { case (t, coll) =>
    (for ((p, ind) <- filterNotPredicates.zipWithIndex) yield {
      val tf = t.filterNot(p)
      val cf = coll.filterNot(p)
      if (!areEqual(tf, cf)) printComparison(t, coll, tf, cf, ind)
      ("op index: " + ind) |: areEqual(tf, cf)
    }).reduceLeft(_ && _)
  }

  property("partitions must be equal") = forAllNoShrink(collectionPairs) { case (t, coll) =>
    (for ((p, ind) <- partitionPredicates.zipWithIndex) yield {
      val tpart @ (tpart1, tpart2) = t.partition(p)
      val cpart @ (cpart1, cpart2) = coll.partition(p)
      if (!areEqual(tpart1, cpart1) || !areEqual(tpart2, cpart2)) {
        println("from: " + t)
        println("and: " + coll)
        println(cpart)
        println(tpart)
      }
      ("op index: " + ind) |: (areEqual(tpart1, cpart1) && areEqual(tpart2, cpart2))
    }).reduceLeft(_ && _)
  }

  if (hasStrictOrder) property("takes must be equal") = forAllNoShrink(collectionPairsWithLengths) { case (t, coll, n) =>
    ("take " + n + " elements") |: t.take(n).iterator.sameElements(coll.take(n))
  }

  if (hasStrictOrder) property("drops must be equal") = forAllNoShrink(collectionPairsWithLengths) { case (t, coll, n) =>
    ("drop " + n + " elements") |: t.drop(n).iterator.sameElements(coll.drop(n))
  }

  if (hasStrictOrder) property("slices must be equal") = forAllNoShrink(collectionPairsWith2Indices) { case (t, coll, fr, slicelength) =>
    val from = if (fr < 0) 0 else fr
    val until = if (from + slicelength > t.size) t.size else from + slicelength
    val tsl = t.slice(from, until)
    val collsl = coll.slice(from, until)
    if (!tsl.iterator.sameElements(collsl)) {
      println("---------------------- " + from + ", " + until)
      println("from: " + t)
      println("and: " + coll)
      println(tsl)
      println(collsl)
      println("as list: " + collsl.toList)
      println(collsl.iterator.hasNext)
      println(collsl.iterator.next())
      println(collsl.iterator.hasNext)
      println(collsl.iterator.next())
      println(collsl.iterator.hasNext)
      println(collsl.iterator.next())
      println(collsl.iterator.hasNext)
    }
    ("slice from " + from + " until " + until) |: tsl.iterator.sameElements(collsl)
  }

  if (hasStrictOrder) property("splits must be equal") = forAllNoShrink(collectionPairsWithLengths) { case (t, coll, n) =>
    val tspl @ (tspl1, tspl2) = t.splitAt(n)
    val cspl @ (cspl1, cspl2) = coll.splitAt(n)
    if (!tspl1.iterator.sameElements(cspl1) || !tspl2.iterator.sameElements(cspl2)) {
      println("at: " + n)
      println("from: " + t)
      println("and: " + coll)
      println(tspl)
      println(cspl)
    }
    ("splitAt " + n) |: (tspl1.iterator.sameElements(cspl1) && tspl2.iterator.sameElements(cspl2))
  }

  if (hasStrictOrder) property("takeWhiles must be equal") = forAllNoShrink(collectionPairs) { case (t, coll) =>
    (for ((pred, ind) <- takeWhilePredicates.zipWithIndex) yield {
      val tt = t.takeWhile(pred)
      val ct = coll.takeWhile(pred)
      if (!tt.iterator.sameElements(ct)) {
        println("from: " + t)
        println("and: " + coll)
        println("taking while...")
        println(tt)
        println(ct)
      }
      ("operator " + ind) |: tt.iterator.sameElements(ct)
    }).reduceLeft(_ && _)
  }

  if (hasStrictOrder) property("spans must be equal") = forAllNoShrink(collectionPairs) { case (t, coll) =>
    (for ((pred, ind) <- spanPredicates.zipWithIndex) yield {
      val tsp @ (tsp1, tsp2) = t.span(pred)
      val csp @ (csp1, csp2) = coll.span(pred)
      if (!tsp1.iterator.sameElements(csp1) || !tsp2.iterator.sameElements(csp2)) {
        println("from: " + t)
        println("and: " + coll)
        println("span with predicate " + ind)
        println(tsp)
        println(csp)
        println("---------------------------------")
        println(coll.span(pred))
        println("---------------------------------")
      }
      ("operator " + ind) |: (tsp1.iterator.sameElements(csp1) && tsp2.iterator.sameElements(csp2))
    }).reduceLeft(_ && _)
  }

  if (hasStrictOrder) property("dropWhiles must be equal") = forAllNoShrink(collectionPairs) { case (t, coll) =>
    (for ((pred, ind) <- dropWhilePredicates.zipWithIndex) yield {
      ("operator " + ind) |: t.dropWhile(pred).iterator.sameElements(coll.dropWhile(pred))
    }).reduceLeft(_ && _)
  }

  property("folds must be equal for assoc. operators") = forAllNoShrink(collectionPairs) { case (t, coll) =>
    (for (((first, op), ind) <- foldArguments.zipWithIndex) yield {
      val tres = t.foldLeft(first)(op)
      val cres = coll.fold(first)(op)
      if (cres != tres) {
        println("from: " + t)
        println("and: " + coll)
        println("folds are: ")
        println(tres)
        println(cres)
      }
      ("operator " + ind) |: tres == cres
    }).reduceLeft(_ && _)
  }

  property("++s must be equal") = forAll(collectionTriplets) { case (t, coll, colltoadd) =>
    try {
      val toadd = colltoadd
      val tr = t ++ toadd.iterator
      val cr = coll ++ toadd.iterator
      if (!areEqual(tr, cr)) {
        println("from: " + t)
        println("and: " + coll.iterator.toList)
        println("adding: " + toadd)
        println(tr.toList)
        println(cr.iterator.toList)
      }
      ("adding " |: areEqual(tr, cr)) &&
      (for ((trav, ind) <- addAllIterables.zipWithIndex) yield {
        val tadded = t ++ trav
        val cadded = coll ++ trav
        if (!areEqual(tadded, cadded)) {
          println("----------------------")
          println("from: " + t)
          println("and: " + coll)
          println("adding: " + trav)
          println(tadded)
          println(cadded)
        }
        ("traversable " + ind) |: areEqual(tadded, cadded)
      }).reduceLeft(_ && _)
    } catch {
      case e: java.lang.Exception =>
        throw e
    }
  }

  if (hasStrictOrder) property("copies to array must be equal") = forAllNoShrink(collectionPairs) { case (t, coll) =>
    val tarr = newArray(t.size)
    val collarr = newArray(coll.size)
    t.copyToArray(tarr, 0, t.size)
    coll.copyToArray(collarr, 0, coll.size)
    if (tarr.toSeq != collarr.toSeq) {
      println("from: " + t)
      println("and: " + coll)
      println(tarr.toSeq)
      println(collarr.toSeq)
    }
    tarr.toSeq == collarr.toSeq
  }

  if (hasStrictOrder) property("scans must be equal") = forAllNoShrink(collectionPairs) {
    case (t, coll) =>
      (for (((first, op), ind) <- foldArguments.zipWithIndex) yield {
        val tscan = t.scanLeft(first)(op)
        val cscan = coll.scan(first)(op)
        if (!tscan.iterator.sameElements(cscan) || !cscan.sameElements(tscan)) {
          println("from: " + t)
          println("and: " + coll)
          println("scans are: ")
          println(tscan)
          println(cscan)
        }
        ("operator " + ind) |: tscan.iterator.sameElements(cscan) && cscan.sameElements(tscan)
      }).reduceLeft(_ && _)
  }

  property("groupBy must be equal") = forAllNoShrink(collectionPairs) {
    case (t, coll) =>
      (for ((f, ind) <- groupByFunctions.zipWithIndex) yield {
        val tgroup: scala.collection.Map[T, Iterable[T]] = t.groupBy(f)
        val cgroup: scala.collection.parallel.ParMap[T, ParIterable[T]] = coll.groupBy(f)
        val cgroupseq: scala.collection.parallel.ParMap[T, Iterable[T]] = cgroup.map { case (k, xs) => (k, xs.seq) }
        val areMapsEqual =
          tgroup.forall { case (k, v) => cgroupseq.get(k).contains(v) } &&
            cgroupseq.forall { case (k, v) => tgroup.get(k).contains(v) }
        if (!areMapsEqual) {
          println("from: " + t)
          println("and: " + coll)
          println("groups are: ")
          println(tgroup)
          println(cgroupseq)
        }
        ("operator " + ind) |: areMapsEqual
      }).reduceLeft(_ && _)
  }

}
