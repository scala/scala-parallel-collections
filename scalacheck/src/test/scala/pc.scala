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

import org.scalacheck._
import scala.collection.parallel._

// package here to be able access the package-private implementation and shutdown the pool
package scala {

  abstract class ParCollProperties extends Properties("Parallel collections") {

    // Included tests have to be abstract classes, otherwise sbt tries to instantiate them on its own and fails
    def includeAllTestsWith(support: TaskSupport): Unit = {
      // parallel arrays with default task support
      include(new mutable.IntParallelArrayCheck(support) {})
    
      // parallel ranges
      include(new immutable.ParallelRangeCheck(support) {})
    
      // parallel immutable hash maps (tries)
      include(new immutable.IntIntParallelHashMapCheck(support) {})
    
      // parallel immutable hash sets (tries)
      include(new immutable.IntParallelHashSetCheck(support) {})
    
      // parallel mutable hash maps (tables)
      include(new mutable.IntIntParallelHashMapCheck(support) {})
    
      // parallel ctrie
      include(new mutable.IntIntParallelConcurrentTrieMapCheck(support) {})
    
      // parallel mutable hash sets (tables)
      include(new mutable.IntParallelHashSetCheck(support) {})
    
      // parallel vectors
      include(new immutable.IntParallelVectorCheck(support) {})
    }
  
    includeAllTestsWith(defaultTaskSupport)
  
    val ec = scala.concurrent.ExecutionContext.fromExecutorService(java.util.concurrent.Executors.newFixedThreadPool(5))
    val ectasks = new collection.parallel.ExecutionContextTaskSupport(ec)
    includeAllTestsWith(ectasks)

    // no post test hooks in scalacheck, so cannot do:
    // ec.shutdown()
  
  }

}

object Test extends scala.ParCollProperties {
  /*
  def main(args: Array[String]) {
    val pc = new ParCollProperties
    org.scalacheck.Test.checkProperties(
      org.scalacheck.Test.Params(
        rng = new java.util.Random(5134L),
        testCallback = new ConsoleReporter(0),
        workers = 1,
        minSize = 0,
        maxSize = 4000,
        minSuccessfulTests = 5
      ),
      pc
    )
  }
  */
}
