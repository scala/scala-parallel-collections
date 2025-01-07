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

import javax.xml.bind.DatatypeConverter._
import java.nio.file.{ Path, Paths, Files }
import org.junit.Test

trait SerializationStabilityBase {

  val overwrite: Option[Path] =
    sys.props.get("overwrite.source")
      .map(s => Paths.get(s).toAbsolutePath)

  def serialize(o: AnyRef): String = {
    val bos = new java.io.ByteArrayOutputStream()
    val out = new java.io.ObjectOutputStream(bos)
    out.writeObject(o)
    out.flush()
    printBase64Binary(bos.toByteArray())
  }

  def amend(path: Path)(f: String => String): Unit = {
    val old = new String(java.nio.file.Files.readAllBytes(path))
    Files.write(path, f(old).getBytes)
  }

  def quote(s: String) = List("\"", s, "\"").mkString

  def patch(path: Path, line: Int, prevResult: String, result: String): Unit = {
    amend(path) {
      content =>
        content.linesIterator.toList.zipWithIndex.map {
          case (content, i) if i == line - 1 =>
            val newContent = content.replace(quote(prevResult), quote(result))
            if (newContent != content)
              println(s"- $content\n+ $newContent\n")
            newContent
          case (content, _) => content
        }.mkString("\n")
    }
  }

  def updateComment(path: Path): Unit = {
    val timestamp = {
      import java.text.SimpleDateFormat
      val sdf = new SimpleDateFormat("yyyyMMdd-HH:mm:ss")
      sdf.format(new java.util.Date)
    }
    val newComment = s"  // Generated on $timestamp with Scala ${scala.util.Properties.versionString})"
    amend(path) {
      content =>
        content.linesIterator.toList.map {
          f => f.replaceAll("""^ +// Generated on.*""", newComment)
        }.mkString("\n")
    }
  }

  def deserialize(string: String): AnyRef = {
    val bis = new java.io.ByteArrayInputStream(parseBase64Binary(string))
    val in = new java.io.ObjectInputStream(bis)
    in.readObject()
  }

  def checkRoundTrip[T <: AnyRef](instance: T)(f: T => AnyRef): Unit = {
    val result = serialize(instance)
    val reconstituted = deserialize(result).asInstanceOf[T]
    assert(f(instance) == f(reconstituted), (f(instance), f(reconstituted)))
  }

}
