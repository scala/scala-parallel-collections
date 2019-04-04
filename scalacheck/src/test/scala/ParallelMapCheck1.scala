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

import scala.collection._
import scala.collection.parallel._

abstract class ParallelMapCheck[K, V](collname: String) extends ParallelIterableCheck[(K, V)](collname) {
  type CollType <: ParMap[K, V]

  property("gets iterated keys") = forAllNoShrink(collectionPairs) {
    case (t, coll) =>
    val containsT = for ((k, v) <- t) yield (coll.get(k) == Some(v))
    val containsSelf = coll.map { case (k, v) => coll.get(k) == Some(v) }
    ("Par contains elements of seq map" |: containsT.forall(_ == true)) &&
    ("Par contains elements of itself" |: containsSelf.forall(_ == true))
  }

  override def collectionPairs: Gen[(Map[K, V], CollType)] =
    super.collectionPairs.map { case (iterable, parmap) =>
      (iterable.to(Map), parmap)
    }

  override def collectionTriplets: Gen[(Map[K, V], CollType, scala.Seq[(K, V)])] =
    super.collectionTriplets.map { case (iterable, parmap, seq) =>
      (iterable.to(Map), parmap, seq)
    }

  // The following tests have been copied from `ParIterableCheck`, and adapted to test
  // overloads of the methods that return Map and ParMap collections
  // They are disabled for now because this behavior is unspecified and the tests fail.
//  property("mappings returning maps must be equal") = forAll/*NoShrink*/(collectionPairs) { case (t, coll) =>
//    val results = for ((f, ind) <- mapFunctions.zipWithIndex.take(5)) yield {
//      val ms: Map[K, V] = t.map(f)
//      val mp: ParMap[K, V] = coll.map(f)
//      val invs = checkDataStructureInvariants(ms, mp)
//      if (!areEqual(ms, mp) || !invs) {
//        println(t)
//        println(coll)
//        println("mapped to: ")
//        println(ms)
//        println(mp)
//        println("sizes: ")
//        println(ms.size)
//        println(mp.size)
//        println("valid: " + invs)
//      }
//      ("op index: " + ind) |: (areEqual(ms, mp) && invs)
//    }
//    results.reduceLeft(_ && _)
//  }
//
//  property("collects returning maps must be equal") = forAllNoShrink(collectionPairs) { case (t, coll) =>
//    val results = for ((f, ind) <- partialMapFunctions.zipWithIndex) yield {
//      val ps: Map[K, V] = t.collect(f)
//      val pp: ParMap[K, V] = coll.collect(f)
//      if (!areEqual(ps, pp)) {
//        println(t)
//        println(coll)
//        println("collected to: ")
//        println(ps)
//        println(pp)
//      }
//      ("op index: " + ind) |: areEqual(ps, pp)
//    }
//    results.reduceLeft(_ && _)
//  }
//
//  property("flatMaps returning maps must be equal") = forAllNoShrink(collectionPairs) { case (t, coll) =>
//    (for ((f, ind) <- flatMapFunctions.zipWithIndex)
//      yield ("op index: " + ind) |: {
//        val tf: Map[K, V] = t.flatMap(f)
//        val collf: ParMap[K, V] = coll.flatMap(f)
//        if (!areEqual(tf, collf)) {
//          println("----------------------")
//          println(s"t = $t")
//          println(s"coll = $coll")
//          println(s"tf = $tf")
//          println(s"collf = $collf")
//        }
//        areEqual(t.flatMap(f), coll.flatMap(f))
//      }).reduceLeft(_ && _)
//  }
//
//  property("++s returning maps must be equal") = forAll(collectionTriplets) { case (t, coll, colltoadd) =>
//    try {
//      val toadd = colltoadd
//      val tr: Map[K, V] = t ++ toadd.iterator
//      val cr: ParMap[K, V] = coll ++ toadd.iterator
//      if (!areEqual(tr, cr)) {
//        println("from: " + t)
//        println("and: " + coll.iterator.toList)
//        println("adding: " + toadd)
//        println(tr.toList)
//        println(cr.iterator.toList)
//      }
//      (s"adding " |: areEqual(tr, cr)) &&
//      (for ((trav, ind) <- addAllIterables.zipWithIndex) yield {
//        val tadded: Map[K, V] = t ++ trav
//        val cadded: ParMap[K, V] = coll ++ trav
//        if (!areEqual(tadded, cadded)) {
//          println("----------------------")
//          println("from: " + t)
//          println("and: " + coll)
//          println("adding: " + trav)
//          println(tadded)
//          println(cadded)
//        }
//        ("traversable " + ind) |: areEqual(tadded, cadded)
//      }).reduceLeft(_ && _)
//    } catch {
//      case e: java.lang.Exception =>
//        throw e
//    }
//  }

}
