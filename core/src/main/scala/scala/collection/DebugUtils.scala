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