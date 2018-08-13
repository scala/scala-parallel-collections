package scala

import javax.xml.bind.DatatypeConverter._
import scala.reflect.io.File
import org.junit.Test

// This test is self-modifying when run as follows:
//
//    junit/testOnly scala.SerializationStabilityTest -- -Doverwrite.source=junit/src/test/scala/scala/SerializationStabilityTest.scala
//
// Use this to re-establish a baseline for serialization compatibility.

// based on run/t8549.scala partest
object SerializationStability extends App {
  val overwrite: Option[File] = sys.props.get("overwrite.source").map(s => new File(new java.io.File(s)))

  def serialize(o: AnyRef): String = {
    val bos = new java.io.ByteArrayOutputStream()
    val out = new java.io.ObjectOutputStream(bos)
    out.writeObject(o)
    out.flush()
    printBase64Binary(bos.toByteArray())
  }

  def amend(file: File)(f: String => String): Unit = {
    file.writeAll(f(file.slurp))
  }
  def quote(s: String) = List("\"", s, "\"").mkString

  def patch(file: File, line: Int, prevResult: String, result: String): Unit = {
    amend(file) {
      content =>
        content.linesIterator.toList.zipWithIndex.map {
          case (content, i) if i == line - 1 =>
            val newContent = content.replaceAllLiterally(quote(prevResult), quote(result))
            if (newContent != content)
              println(s"- $content\n+ $newContent\n")
            newContent
          case (content, _) => content
        }.mkString("\n")
    }
  }

  def updateComment(file: File): Unit = {
    val timestamp = {
      import java.text.SimpleDateFormat
      val sdf = new SimpleDateFormat("yyyyMMdd-HH:mm:ss")
      sdf.format(new java.util.Date)
    }
    val newComment = s"  // Generated on $timestamp with Scala ${scala.util.Properties.versionString})"
    amend(file) {
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

  def check[T <: AnyRef](instance: => T)(prevResult: String, f: T => AnyRef = (x: T) => x): Unit = {
    val result = serialize(instance)
    overwrite match {
      case Some(f) =>
        val lineNumberOfLiteralString = Thread.currentThread.getStackTrace.apply(2).getLineNumber
        patch(f, lineNumberOfLiteralString, prevResult, result)
      case None =>
        checkRoundTrip(instance)(f)
        assert(f(deserialize(prevResult).asInstanceOf[T]) == f(instance), s"$instance != f(deserialize(prevResult))")
        assert(prevResult == result, s"instance = $instance : ${instance.getClass}\n serialization unstable: ${prevResult}\n   found: ${result}")
    }
  }

  // Generated on 20190112-18:04:29 with Scala version 2.13.0-20190109-081947-e69ecf1)
  overwrite.foreach(updateComment)

  // check(new collection.concurrent.TrieMap[Any, Any]())( "rO0ABXNyACNzY2FsYS5jb2xsZWN0aW9uLmNvbmN1cnJlbnQuVHJpZU1hcKckxpgOIYHPAwAETAALZXF1YWxpdHlvYmp0ABJMc2NhbGEvbWF0aC9FcXVpdjtMAApoYXNoaW5nb2JqdAAcTHNjYWxhL3V0aWwvaGFzaGluZy9IYXNoaW5nO0wABHJvb3R0ABJMamF2YS9sYW5nL09iamVjdDtMAAtyb290dXBkYXRlcnQAOUxqYXZhL3V0aWwvY29uY3VycmVudC9hdG9taWMvQXRvbWljUmVmZXJlbmNlRmllbGRVcGRhdGVyO3hwc3IAMnNjYWxhLmNvbGxlY3Rpb24uY29uY3VycmVudC5UcmllTWFwJE1hbmdsZWRIYXNoaW5nhTBoJQ/mgb0CAAB4cHNyABhzY2FsYS5tYXRoLkVxdWl2JCRhbm9uJDLBbyx4dy/qGwIAAHhwc3IANHNjYWxhLmNvbGxlY3Rpb24uY29uY3VycmVudC5UcmllTWFwU2VyaWFsaXphdGlvbkVuZCSbjdgbbGCt2gIAAHhweA==")
  // not sure why this one needs stable serialization.

  import collection.parallel
  check(parallel.immutable.ParHashMap(1 -> 2))( "rO0ABXNyAC5zY2FsYS5jb2xsZWN0aW9uLnBhcmFsbGVsLmltbXV0YWJsZS5QYXJIYXNoTWFwAAAAAAAAAAMCAANMAA9TY2FuTGVhZiRtb2R1bGV0ADVMc2NhbGEvY29sbGVjdGlvbi9wYXJhbGxlbC9QYXJJdGVyYWJsZUxpa2UkU2NhbkxlYWYkO0wAD1NjYW5Ob2RlJG1vZHVsZXQANUxzY2FsYS9jb2xsZWN0aW9uL3BhcmFsbGVsL1Bhckl0ZXJhYmxlTGlrZSRTY2FuTm9kZSQ7TAAEdHJpZXQAJ0xzY2FsYS9jb2xsZWN0aW9uL2ltbXV0YWJsZS9PbGRIYXNoTWFwO3hwcHBzcgAyc2NhbGEuY29sbGVjdGlvbi5nZW5lcmljLkRlZmF1bHRTZXJpYWxpemF0aW9uUHJveHkAAAAAAAAAAwMAAUwAB2ZhY3Rvcnl0ABpMc2NhbGEvY29sbGVjdGlvbi9GYWN0b3J5O3hwc3IAJXNjYWxhLmNvbGxlY3Rpb24uTWFwRmFjdG9yeSRUb0ZhY3RvcnkAAAAAAAAAAwIAAUwAB2ZhY3Rvcnl0AB1Mc2NhbGEvY29sbGVjdGlvbi9NYXBGYWN0b3J5O3hwc3IAJnNjYWxhLnJ1bnRpbWUuTW9kdWxlU2VyaWFsaXphdGlvblByb3h5AAAAAAAAAAECAAFMAAttb2R1bGVDbGFzc3QAEUxqYXZhL2xhbmcvQ2xhc3M7eHB2cgAmc2NhbGEuY29sbGVjdGlvbi5pbW11dGFibGUuT2xkSGFzaE1hcCQAAAAAAAAAAwMAAHhwdwQAAAABc3IADHNjYWxhLlR1cGxlMgH73c0i5zR6AgACTAACXzF0ABJMamF2YS9sYW5nL09iamVjdDtMAAJfMnEAfgAReHBzcgARamF2YS5sYW5nLkludGVnZXIS4qCk94GHOAIAAUkABXZhbHVleHIAEGphdmEubGFuZy5OdW1iZXKGrJUdC5TgiwIAAHhwAAAAAXNxAH4AEwAAAAJ4")
  check(parallel.immutable.ParHashSet(1, 2, 3))( "rO0ABXNyAC5zY2FsYS5jb2xsZWN0aW9uLnBhcmFsbGVsLmltbXV0YWJsZS5QYXJIYXNoU2V0AAAAAAAAAAECAANMAA9TY2FuTGVhZiRtb2R1bGV0ADVMc2NhbGEvY29sbGVjdGlvbi9wYXJhbGxlbC9QYXJJdGVyYWJsZUxpa2UkU2NhbkxlYWYkO0wAD1NjYW5Ob2RlJG1vZHVsZXQANUxzY2FsYS9jb2xsZWN0aW9uL3BhcmFsbGVsL1Bhckl0ZXJhYmxlTGlrZSRTY2FuTm9kZSQ7TAAEdHJpZXQAJ0xzY2FsYS9jb2xsZWN0aW9uL2ltbXV0YWJsZS9PbGRIYXNoU2V0O3hwcHBzcgAyc2NhbGEuY29sbGVjdGlvbi5nZW5lcmljLkRlZmF1bHRTZXJpYWxpemF0aW9uUHJveHkAAAAAAAAAAwMAAUwAB2ZhY3Rvcnl0ABpMc2NhbGEvY29sbGVjdGlvbi9GYWN0b3J5O3hwc3IAKnNjYWxhLmNvbGxlY3Rpb24uSXRlcmFibGVGYWN0b3J5JFRvRmFjdG9yeQAAAAAAAAADAgABTAAHZmFjdG9yeXQAIkxzY2FsYS9jb2xsZWN0aW9uL0l0ZXJhYmxlRmFjdG9yeTt4cHNyACZzY2FsYS5ydW50aW1lLk1vZHVsZVNlcmlhbGl6YXRpb25Qcm94eQAAAAAAAAABAgABTAALbW9kdWxlQ2xhc3N0ABFMamF2YS9sYW5nL0NsYXNzO3hwdnIAJnNjYWxhLmNvbGxlY3Rpb24uaW1tdXRhYmxlLk9sZEhhc2hTZXQkAAAAAAAAAAMDAAB4cHcEAAAAA3NyABFqYXZhLmxhbmcuSW50ZWdlchLioKT3gYc4AgABSQAFdmFsdWV4cgAQamF2YS5sYW5nLk51bWJlcoaslR0LlOCLAgAAeHAAAAABc3EAfgAQAAAAAnNxAH4AEAAAAAN4")
  // TODO SI-8576 Uninitialized field under -Xcheckinit
  // check(new parallel.immutable.ParRange(new Range(0, 1, 2)))( "rO0ABXNyACxzY2FsYS5jb2xsZWN0aW9uLnBhcmFsbGVsLmltbXV0YWJsZS5QYXJSYW5nZQAAAAAAAAABAgAETAAXUGFyUmFuZ2VJdGVyYXRvciRtb2R1bGV0AEBMc2NhbGEvY29sbGVjdGlvbi9wYXJhbGxlbC9pbW11dGFibGUvUGFyUmFuZ2UkUGFyUmFuZ2VJdGVyYXRvciQ7TAAPU2NhbkxlYWYkbW9kdWxldAA1THNjYWxhL2NvbGxlY3Rpb24vcGFyYWxsZWwvUGFySXRlcmFibGVMaWtlJFNjYW5MZWFmJDtMAA9TY2FuTm9kZSRtb2R1bGV0ADVMc2NhbGEvY29sbGVjdGlvbi9wYXJhbGxlbC9QYXJJdGVyYWJsZUxpa2UkU2Nhbk5vZGUkO0wABXJhbmdldAAiTHNjYWxhL2NvbGxlY3Rpb24vaW1tdXRhYmxlL1JhbmdlO3hwcHBwc3IAIHNjYWxhLmNvbGxlY3Rpb24uaW1tdXRhYmxlLlJhbmdlabujVKsVMg0CAAdJAANlbmRaAAdpc0VtcHR5SQALbGFzdEVsZW1lbnRJABBudW1SYW5nZUVsZW1lbnRzSQAFc3RhcnRJAARzdGVwSQAPdGVybWluYWxFbGVtZW50eHAAAAABAAAAAAAAAAABAAAAAAAAAAIAAAAC")
  // TODO SI-8576 unstable under -Xcheckinit
  // check(parallel.mutable.ParArray(1, 2, 3))( "rO0ABXNyACpzY2FsYS5jb2xsZWN0aW9uLnBhcmFsbGVsLm11dGFibGUuUGFyQXJyYXkAAAAAAAAAAQMABEwAF1BhckFycmF5SXRlcmF0b3IkbW9kdWxldAA+THNjYWxhL2NvbGxlY3Rpb24vcGFyYWxsZWwvbXV0YWJsZS9QYXJBcnJheSRQYXJBcnJheUl0ZXJhdG9yJDtMAA9TY2FuTGVhZiRtb2R1bGV0ADVMc2NhbGEvY29sbGVjdGlvbi9wYXJhbGxlbC9QYXJJdGVyYWJsZUxpa2UkU2NhbkxlYWYkO0wAD1NjYW5Ob2RlJG1vZHVsZXQANUxzY2FsYS9jb2xsZWN0aW9uL3BhcmFsbGVsL1Bhckl0ZXJhYmxlTGlrZSRTY2FuTm9kZSQ7TAAIYXJyYXlzZXF0ACNMc2NhbGEvY29sbGVjdGlvbi9tdXRhYmxlL0FycmF5U2VxO3hwcHBwc3IAMXNjYWxhLmNvbGxlY3Rpb24ucGFyYWxsZWwubXV0YWJsZS5FeHBvc2VkQXJyYXlTZXGx2OTefAodSQIAAkkABmxlbmd0aFsABWFycmF5dAATW0xqYXZhL2xhbmcvT2JqZWN0O3hyACFzY2FsYS5jb2xsZWN0aW9uLm11dGFibGUuQXJyYXlTZXEVPD3SKEkOcwIAAkkABmxlbmd0aFsABWFycmF5cQB+AAd4cAAAAAN1cgATW0xqYXZhLmxhbmcuT2JqZWN0O5DOWJ8QcylsAgAAeHAAAAADcHBwAAAAA3VxAH4ACgAAABBzcgARamF2YS5sYW5nLkludGVnZXIS4qCk94GHOAIAAUkABXZhbHVleHIAEGphdmEubGFuZy5OdW1iZXKGrJUdC5TgiwIAAHhwAAAAAXNxAH4ADQAAAAJzcQB+AA0AAAADcHBwcHBwcHBwcHBwcHg=")
  check(parallel.mutable.ParHashMap(1 -> 2))( "rO0ABXNyACxzY2FsYS5jb2xsZWN0aW9uLnBhcmFsbGVsLm11dGFibGUuUGFySGFzaE1hcAAAAAAAAAADAwAISQALX2xvYWRGYWN0b3JJAAlzZWVkdmFsdWVJAAl0YWJsZVNpemVJAAl0aHJlc2hvbGRMAA9TY2FuTGVhZiRtb2R1bGV0ADVMc2NhbGEvY29sbGVjdGlvbi9wYXJhbGxlbC9QYXJJdGVyYWJsZUxpa2UkU2NhbkxlYWYkO0wAD1NjYW5Ob2RlJG1vZHVsZXQANUxzY2FsYS9jb2xsZWN0aW9uL3BhcmFsbGVsL1Bhckl0ZXJhYmxlTGlrZSRTY2FuTm9kZSQ7WwAHc2l6ZW1hcHQAAltJWwAFdGFibGV0ACVbTHNjYWxhL2NvbGxlY3Rpb24vbXV0YWJsZS9IYXNoRW50cnk7eHB3DQAAAu4AAAABAAAABAFzcgARamF2YS5sYW5nLkludGVnZXIS4qCk94GHOAIAAUkABXZhbHVleHIAEGphdmEubGFuZy5OdW1iZXKGrJUdC5TgiwIAAHhwAAAAAXNxAH4ABgAAAAJ4")
  check(parallel.mutable.ParHashSet(1, 2, 3))( "rO0ABXNyACxzY2FsYS5jb2xsZWN0aW9uLnBhcmFsbGVsLm11dGFibGUuUGFySGFzaFNldAAAAAAAAAABAwAISQALX2xvYWRGYWN0b3JJAAlzZWVkdmFsdWVJAAl0YWJsZVNpemVJAAl0aHJlc2hvbGRMAA9TY2FuTGVhZiRtb2R1bGV0ADVMc2NhbGEvY29sbGVjdGlvbi9wYXJhbGxlbC9QYXJJdGVyYWJsZUxpa2UkU2NhbkxlYWYkO0wAD1NjYW5Ob2RlJG1vZHVsZXQANUxzY2FsYS9jb2xsZWN0aW9uL3BhcmFsbGVsL1Bhckl0ZXJhYmxlTGlrZSRTY2FuTm9kZSQ7WwAHc2l6ZW1hcHQAAltJWwAFdGFibGV0ABNbTGphdmEvbGFuZy9PYmplY3Q7eHB3DQAAAcIAAAADAAAAGwFzcgARamF2YS5sYW5nLkludGVnZXIS4qCk94GHOAIAAUkABXZhbHVleHIAEGphdmEubGFuZy5OdW1iZXKGrJUdC5TgiwIAAHhwAAAAAXNxAH4ABgAAAAJzcQB+AAYAAAADeA==")
}

class SerializationStabilityTest {
  @Test
  def testAll: Unit = SerializationStability.main(new Array[String](0))
}
