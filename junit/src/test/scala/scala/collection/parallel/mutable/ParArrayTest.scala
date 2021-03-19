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

package scala.collection.parallel.mutable

import org.junit.Test
import org.junit.Assert._

class ParArrayTest extends scala.collection.concurrent.ctries_old.Spec {

  @Test
  def `create new parallel array with a bad initial capacity`: Unit = {
    evaluating { new ParArray(-5) }.shouldProduce[IllegalArgumentException]()
    /**
     * this currently passes, but do we want it to?
     * does it have meaning to have an empty parallel array?
     */
    new ParArray(0)
    ()
  }

  @Test
  def `compare identical ParArrays`: Unit = {
    assert(new ParArray(5) == new ParArray(5))
    assert(ParArray(1,2,3,4,5) == ParArray(1,2,3,4,5))
  }

  /**
   * this test needs attention. how is equality defined on ParArrays?
   * Well, the same way it is for normal collections, I guess. For normal arrays its reference equality.
   * I do not think it should be that way in the case of ParArray-s. I'll check this with Martin.
   */
  @Test
  def `compare non-identical ParArrays`: Unit = {
    assert(ParArray(1,2,3,4,5) != ParArray(1,2,3,4),
      "compared PA's that I expect to not be identical, but they were!")
  }

  @Test
  def `"creation via PA object [String]`: Unit = {
    val paFromApply: ParArray[String] = ParArray("x", "1", "true", "etrijwejiorwer")
    val paFromHandoff: ParArray[String] = ParArray.handoff(Array("x", "1", "true", "etrijwejiorwer"))
    val paFromCopy: ParArray[String] = ParArray.createFromCopy(Array("x", "1", "true", "etrijwejiorwer"))
    assert( paFromApply == paFromCopy )
    assert( paFromApply == paFromCopy )
  }

//  // handoffs dont work for primitive types...
//  test("creation via PA object [Boolean]"){
//    val paFromApply: ParArray[Boolean] = ParArray(true, false, true, false)
//    val paFromCopy: ParArray[Boolean] = ParArray.createFromCopy(Array(true, false, true, false))
//    assert( paFromApply == paFromCopy )
//  }
//
//  // handoffs dont work for primitive types...
//  test("creation via PA object [Int]"){
//    val paFromApply: ParArray[Int] = ParArray(1, 2, 4, 3)
//    val paFromCopy: ParArray[Int] = ParArray.createFromCopy(Array(1, 2, 4, 3))
//    assert( paFromApply == paFromCopy )
//  }

  /**
   * This fails because handoff is really doing a copy.
   * TODO: look at handoff
   */
  @Test
  def `Handoff Is Really A Handoff`: Unit = {
    val arrayToHandOff = Array("a", "x", "y", "z")
    val paFromHandoff: ParArray[String] = ParArray.handoff(arrayToHandOff)
    arrayToHandOff(0) = "w"
    assert(paFromHandoff(0) == "w")
  }

  @Test
  def `simple reduce`: Unit = {
    assert( ParArray(1,2,3,4,5).reduce(_+_) == 15 )
  }

  @Test
  def `empty reduce`: Unit = {
    evaluating { ParArray.empty[Int].reduce(_+_) }.shouldProduce[UnsupportedOperationException]()
  }

  @Test
  def `simple count`: Unit = {
    assert( ParArray[Int]().count(_ > 7) == 0 )
    assert( ParArray(1,2,3).count(_ > 7) == 0 )
    assert( ParArray(1,2,3).count(_ <= 3) == 3 )
    assert( ParArray(1,2,3,4,5,6,7,8,9,10).count(_ > 7 ) == 3 )
  }

  @Test
  def `simple forall`: Unit = {
    assert( ParArray[Int]().forall(_ > 7) == true )
    assert( ParArray(1,2,3).forall(_ > 3) == false )
    assert( ParArray(1,2,3).forall(_ <= 3) == true )
    assert( ParArray(1,2,3,4,5,6,7,8,9,10).forall(_ > 0) == true )
    assert( ParArray(1,2,3,4,5,6,7,8,9,10).forall(_ < 5) == false )
  }

  /**
   */
  @Test
  def `simple foreach`: Unit = {
    val buf = new java.util.concurrent.ArrayBlockingQueue[Int](10000)
    ParArray((1 to 10000):_*).foreach(buf add _)
    (1 to 10000).foreach(i => assert( buf contains i, "buf should have contained:" + i ))
  }

  @Test
  def `simple exists`: Unit = {
    assert( ParArray[Int]().exists(_ => true) == false )
    assert( ParArray(1,2,3).forall(_ > 3) == false )
    assert( ParArray(1,2,3,4,5,6,7,8,9,10).exists(_ > 7) == true )
  }

  @Test
  def `simple filter`: Unit = {
    assert(ParArray(1,2,3,4,5).filter( _ < 4 ) == ParArray(1,2,3))
  }

  @Test
  def `simple map test`: Unit = {
    assert(ParArray(1,2,3,4,5).map( (_:Int) * 10 ) == ParArray(10,20,30,40,50))
  }

  @Test
  def `empty min`: Unit = {
    evaluating { ParArray.empty[Int].min }.shouldProduce[UnsupportedOperationException]()
  }

  @Test
  def `empty max`: Unit = {
    evaluating { ParArray.empty[Int].max }.shouldProduce[UnsupportedOperationException]()
  }

  @Test
  def `empty minBy`: Unit = {
    evaluating { ParArray.empty[String].minBy(_.length) }.shouldProduce[UnsupportedOperationException]()
  }

}
