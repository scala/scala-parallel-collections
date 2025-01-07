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

package scala.collection.concurrent.ctries_old

import scala.reflect.{ClassTag, classTag}

trait Spec {

  implicit class Str2ops(s: String) {
    def in[U](body: =>U): Unit = {
      // just execute body
      body
    }
  }

  implicit class Any2ops(a: Any) {
    def shouldEqual(other: Any) = assert(a == other)
  }

  trait HasShouldProduce[U] { def shouldProduce[T <: Throwable: ClassTag](): Unit }

  def evaluating[U](body: =>U): HasShouldProduce[U] = new HasShouldProduce[U] {
    override def shouldProduce[T <: Throwable: ClassTag]() = {
      var produced = false
      try body
      catch {
        case e: Throwable => if (e.getClass == implicitly[ClassTag[T]].runtimeClass) produced = true
      } finally {
        assert(produced, "Did not produce exception of type: " + implicitly[ClassTag[T]])
      }
    }
  }

}
