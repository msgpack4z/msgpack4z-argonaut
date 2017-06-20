package msgpack4z

import java.io._

object Main {
  def main(args: Array[String]): Unit = {
    val input = args.headOption.getOrElse(sys.error("empty args"))
    val fileName = args.lift(1).getOrElse("out.bin")
    val buf = MsgOutBuffer.create()
    val j = argonaut.JsonParser.parse(input) match {
      case Right(json) => json
      case Left(e) => sys.error(e)
    }
    ArgonautMsgpack.json2msgpack(buf, j)
    val bytes = buf.result()

    val file = new File(fileName)
    use(new FileOutputStream(file)){ out =>
      if (!file.isFile) {
        file.createNewFile()
      }
      out.write(bytes)
      out.flush()
    }
  }

  def use[A <: AutoCloseable, B](a: A)(f: A => B): B = {
    try {
      f(a)
    } finally {
      a.close()
    }
  }
}
