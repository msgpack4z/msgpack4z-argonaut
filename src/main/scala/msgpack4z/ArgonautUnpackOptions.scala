package msgpack4z

import msgpack4z.ArgonautUnpackOptions.NonStringKeyHandler
import argonaut.{JsonLong, Json}
import scalaz.\/-

final case class ArgonautUnpackOptions(
  extension: Unpacker[Json],
  binary: Unpacker[Json],
  positiveInf: UnpackResult[Json],
  negativeInf: UnpackResult[Json],
  nan: UnpackResult[Json],
  nonStringKey: NonStringKeyHandler
)

object ArgonautUnpackOptions {
  val binaryToNumberArray: Binary => Json = { bytes =>
    Json.jArray(bytes.value.map(byte => Json.jNumber(JsonLong(byte))).toList)
  }

  val binaryToNumberArrayUnpacker: Unpacker[Json] = { unpacker =>
    CodecInstances.binary.binaryCodec.unpack(unpacker).map(binaryToNumberArray)
  }

  val extUnpacker: Unpacker[Json] = { unpacker =>
    val header = unpacker.unpackExtTypeHeader
    val data = unpacker.readPayload(header.getLength)
    val dataArray = Json.jArray(data.map(byte => Json.jNumber(JsonLong(byte))).toList)
    val result = Json.obj(
      ("type", Json.jNumber(JsonLong(header.getType))),
      ("data", dataArray)
    )
    \/-(result)
  }

  type NonStringKeyHandler = (MsgType, MsgUnpacker) => Option[String]

  private[this] val jNullRight: UnpackResult[Json] = \/-(Json.jNull)

  val default: ArgonautUnpackOptions = ArgonautUnpackOptions(
    extUnpacker,
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
