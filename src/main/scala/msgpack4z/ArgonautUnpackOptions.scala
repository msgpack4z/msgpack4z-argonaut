package msgpack4z

import msgpack4z.ArgonautUnpackOptions.NonStringKeyHandler
import argonaut.{JsonLong, Json}
import scalaz.{\/-, -\/}

final case class ArgonautUnpackOptions(
  extended: Unpacker[Json],
  binary: Unpacker[Json],
  positiveInf: UnpackResult[Json],
  negativeInf: UnpackResult[Json],
  nan: UnpackResult[Json],
  nonStringKey: NonStringKeyHandler
)

object ArgonautUnpackOptions {
  val binaryToNumberArray: Binary => Json = { bytes =>
    Json.jArray(bytes.value.map(byte => Json.jNumber(JsonLong(byte)))(collection.breakOut))
  }

  val binaryToNumberArrayUnpacker: Unpacker[Json] = { unpacker =>
    CodecInstances.binary.binaryCodec.unpack(unpacker).map(binaryToNumberArray)
  }

  type NonStringKeyHandler = (MsgType, MsgUnpacker) => Option[String]

  private[this] val jNullRight = \/-(Json.jNull)

  val default: ArgonautUnpackOptions = ArgonautUnpackOptions(
    _ => -\/(Err(new Exception("does not support extended type"))),
    binaryToNumberArrayUnpacker,
    jNullRight,
    jNullRight,
    jNullRight,
    {case (tpe, unpacker) =>
      PartialFunction.condOpt(tpe){
        case MsgType.NIL =>
          "null"
        case MsgType.BOOLEAN =>
          unpacker.unpackBoolean().toString
        case MsgType.INTEGER =>
          unpacker.unpackBigInteger().toString
        case MsgType.FLOAT =>
          unpacker.unpackDouble().toString
        case MsgType.STRING =>
          unpacker.unpackString()
      }
    }
  )

}
