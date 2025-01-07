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

class DumbHash(val i: Int) {
  override def equals(other: Any) = other match {
    case that: DumbHash => that.i == this.i
    case _ => false
  }
  override def hashCode = i % 5
  override def toString = "DH(%s)".format(i)
}
