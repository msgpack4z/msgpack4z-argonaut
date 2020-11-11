package msgpack4z

import argonaut._
import argonaut.Json.JsonArray
import scalaz.{-\/, \/, \/-}

object ArgonautMsgpack {

  def jsonCodec(options: ArgonautUnpackOptions): MsgpackCodec[Json] =
    new CodecArgonautJson(options)

  def jsonArrayCodec(options: ArgonautUnpackOptions): MsgpackCodec[JsonArray] =
    new CodecArgonautJsonArray(options)

  def jsonObjectCodec(options: ArgonautUnpackOptions): MsgpackCodec[JsonObject] =
    new CodecArgonautJsonObject(options)

  def allCodec(options: ArgonautUnpackOptions): (MsgpackCodec[Json], MsgpackCodec[JsonArray], MsgpackCodec[JsonObject]) = (
    jsonCodec(options),
    jsonArrayCodec(options),
    jsonObjectCodec(options)
  )

  def jsonObject2msgpack(packer: MsgPacker, obj: JsonObject): Unit = {
    val fields = obj.toList
    packer.packMapHeader(fields.size)
    fields.foreach { field =>
      packer.packString(field._1)
      json2msgpack(packer, field._2)
    }
    packer.mapEnd()
  }

  def jsonArray2msgpack(packer: MsgPacker, array: JsonArray): Unit = {
    packer.packArrayHeader(array.size)
    array.foreach { x =>
      json2msgpack(packer, x)
    }
    packer.arrayEnd()
  }

  def json2msgpack(packer: MsgPacker, json: Json): Unit = {
    json.fold(
      jsonNull = {
        packer.packNil()
      },
      jsonBool = value => {
        packer.packBoolean(value)
      },
      jsonNumber = {
        case JsonDecimal(value) =>
          packer.packDouble(java.lang.Double.parseDouble(value))
        case JsonBigDecimal(value) =>
          packer.packDouble(value.toDouble)
        case JsonLong(value) =>
          packer.packLong(value)
      },
      jsonString = string => {
        packer.packString(string)
      },
      jsonArray = array => {
        jsonArray2msgpack(packer, array)
      },
      jsonObject = obj => {
        jsonObject2msgpack(packer, obj)
      }
    )
  }

  def msgpack2json(unpacker: MsgUnpacker, unpackOptions: ArgonautUnpackOptions): UnpackResult[Json] = {
    val result = Result.empty[Json]
    if (msgpack2json0(unpacker, result, unpackOptions)) {
      \/-(result.value)
    } else {
      -\/(result.error)
    }
  }

  def msgpack2jsonObject(unpacker: MsgUnpacker, unpackOptions: ArgonautUnpackOptions): UnpackResult[JsonObject] = {
    val result = Result.empty[JsonObject]
    if (msgpack2jsObj0(unpacker, result, unpackOptions)) {
      \/-(result.value)
    } else {
      -\/(result.error)
    }
  }

  def msgpack2jsonArray(unpacker: MsgUnpacker, unpackOptions: ArgonautUnpackOptions): UnpackResult[JsonArray] = {
    val result = Result.empty[JsonArray]
    if (msgpack2jsArray0(unpacker, result, unpackOptions)) {
      \/-(result.value)
    } else {
      -\/(result.error)
    }
  }

  private[this] final case class Result[A](
    var value: A, var error: UnpackError
  )
  private[this] object Result {
    def fromEither[A](e: UnpackError \/ A, result: Result[A]): Boolean = e match{
      case \/-(r) =>
        result.value = r
        true
      case -\/(l) =>
        result.error = l
        false
    }

    def empty[A >: Null]: Result[A] = Result[A](null, null)
  }

  private[this] def msgpack2jsObj0(unpacker: MsgUnpacker, result: Result[JsonObject], unpackOptions: ArgonautUnpackOptions): Boolean = {
    val size = unpacker.unpackMapHeader()
    var obj = JsonObject.empty
    var i = 0
    val mapElem = Result.empty[Json]
    var success = true

    def process(key: String): Unit = {
     if (msgpack2json0(unpacker, mapElem, unpackOptions)) {
       obj.+=(key, mapElem.value)
       i += 1
     } else {
       result.error = mapElem.error
       success = false
     }
    }

    while (i < size && success) {
      val tpe = unpacker.nextType()
      if(tpe == MsgType.STRING) {
        process(unpacker.unpackString())
      }else{
        unpackOptions.nonStringKey(tpe, unpacker) match {
          case Some(key) =>
            process(key)
          case None =>
            success = false
            result.error = Other("not string key")
        }
      }
    }
    unpacker.mapEnd()
    if (success) {
      result.value = obj
    }
    success
  }

  private[this] def msgpack2jsArray0(unpacker: MsgUnpacker, result: Result[JsonArray], unpackOptions: ArgonautUnpackOptions): Boolean = {
    val size = unpacker.unpackArrayHeader()
    val array = new Array[Json](size)
    var i = 0
    val arrayElem = Result[Json](null, null)
    var success = true
    while (i < size && success) {
      if (msgpack2json0(unpacker, arrayElem, unpackOptions)) {
        array(i) = arrayElem.value
        i += 1
      } else {
        result.error = arrayElem.error
        success = false
      }
    }
    unpacker.arrayEnd()
    if (success) {
      result.value = array.toList
    }
    success
  }

  private[this] val BigIntegerLongMax = java.math.BigInteger.valueOf(Long.MaxValue)
  private[this] val BigIntegerLongMin = java.math.BigInteger.valueOf(Long.MinValue)

  private def isValidLong(value: java.math.BigInteger): Boolean =
    (BigIntegerLongMin.compareTo(value) <= 0) && (value.compareTo(BigIntegerLongMax) <= 0)

  private def msgpack2json0(unpacker: MsgUnpacker, result: Result[Json], unpackOptions: ArgonautUnpackOptions): Boolean = {
    unpacker.nextType() match {
      case MsgType.NIL =>
        unpacker.unpackNil()
        result.value = Json.jNull
        true
      case MsgType.BOOLEAN =>
        if (unpacker.unpackBoolean()) {
          result.value = Json.jTrue
        } else {
          result.value = Json.jFalse
        }
        true
      case MsgType.INTEGER =>
        val value = unpacker.unpackBigInteger()
        if(isValidLong(value)){
          result.value = Json.jNumber(JsonLong(value.longValue()))
        }else{
          result.value = Json.jNumber(JsonBigDecimal(BigDecimal(value)))
        }
        true
      case MsgType.FLOAT =>
        val f = unpacker.unpackDouble()
        if(f.isPosInfinity){
          Result.fromEither(unpackOptions.positiveInf, result)
        }else if(f.isNegInfinity){
          Result.fromEither(unpackOptions.negativeInf, result)
        }else if(java.lang.Double.isNaN(f)) {
          Result.fromEither(unpackOptions.nan, result)
        }else{
          result.value = Json.jNumber(f)
        }
        true
      case MsgType.STRING =>
        result.value = Json.jString(unpacker.unpackString())
        true
      case MsgType.ARRAY =>
        val result0 = Result.empty[JsonArray]
        val r = msgpack2jsArray0(unpacker, result0, unpackOptions)
        result.error = result0.error
        result.value = Json.jArray(result0.value)
        r
      case MsgType.MAP =>
        val result0 = Result.empty[JsonObject]
        val r = msgpack2jsObj0(unpacker, result0, unpackOptions)
        result.error = result0.error
        result.value = Json.jObject(result0.value)
        r
      case MsgType.BINARY =>
        Result.fromEither(unpackOptions.binary(unpacker), result)
      case MsgType.EXTENSION =>
        Result.fromEither(unpackOptions.extension(unpacker), result)
    }
  }
}


private final class CodecArgonautJsonArray(unpackOptions: ArgonautUnpackOptions) extends MsgpackCodecConstant[JsonArray](
  ArgonautMsgpack.jsonArray2msgpack,
  unpacker => ArgonautMsgpack.msgpack2jsonArray(unpacker, unpackOptions)
)

private final class CodecArgonautJson(unpackOptions: ArgonautUnpackOptions) extends MsgpackCodecConstant[Json](
  ArgonautMsgpack.json2msgpack,
  unpacker => ArgonautMsgpack.msgpack2json(unpacker, unpackOptions)
)

private final class CodecArgonautJsonObject(unpackOptions: ArgonautUnpackOptions) extends MsgpackCodecConstant[JsonObject](
  ArgonautMsgpack.jsonObject2msgpack,
  unpacker => ArgonautMsgpack.msgpack2jsonObject(unpacker, unpackOptions)
)
