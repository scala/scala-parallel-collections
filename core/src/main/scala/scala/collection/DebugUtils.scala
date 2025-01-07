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

package scala.collection

private[collection] object DebugUtils {

  def buildString(closure: (Any => Unit) => Unit): String = {
    val output = new collection.mutable.StringBuilder
    closure { any =>
      output ++= any.toString
      output += '\n'
    }

    output.result()
  }

  def arrayString[T](array: Array[T], from: Int, until: Int): String = {
    array.slice(from, until) map ({
      case null => "n/a"
      case x    => "" + x
    }: scala.PartialFunction[T, String]) mkString " | "
  }
}