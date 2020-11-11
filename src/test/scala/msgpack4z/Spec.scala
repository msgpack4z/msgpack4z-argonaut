package msgpack4z

import argonaut._
import argonaut.JsonObjectScalaz._
import argonaut.JsonScalaz._
import argonaut.Json.JsonArray
import scalaprops._
import scalaz.{-\/, Equal, \/-}
import scalaz.std.list._

abstract class SpecBase extends Scalaprops {

  private[this] implicit val scalaDoubleGen: Gen[Double] =
    Gen[Long].map { n =>
      java.lang.Double.longBitsToDouble(n) match {
        case x if x.isNaN => n
        case x => x
      }
    }

  private[this] implicit val bigDecimalGen: Gen[BigDecimal] =
    Gen[Double].map(BigDecimal(_))

  private[this] implicit val stringGen = Gen.alphaNumString

  private[this] val jsonNumberGen: Gen[JsonNumber] =
    Gen.oneOf(
      bigDecimalGen.map(JsonBigDecimal),
      Gen[Long].map(JsonLong),
      bigDecimalGen.map(a => JsonNumber.fromString(a.toString).get)
    )

  private[this] val jsValuePrimitivesArb: Gen[Json] =
    Gen.oneOf(
      Gen.value(Json.jNull),
      Gen.value(Json.jTrue),
      Gen.value(Json.jFalse),
      jsonNumberGen.map(Json.jNumber(_)),
      Gen[String].map(Json.jString)
    )

  private[this] val jsObjectArb1: Gen[JsonObject] =
    Gen.listOfN(
      6,
      Gen.tuple2(
        Gen[String], jsValuePrimitivesArb
      )
    ).map(JsonObject.fromIterable(_))

  private[this] val jsArrayArb1: Gen[JsonArray] =
    Gen.listOfN(6, jsValuePrimitivesArb)

  implicit val jsValueArb: Gen[Json] =
    Gen.oneOf(
      jsValuePrimitivesArb,
      jsObjectArb1.map(Json.jObject),
      jsArrayArb1.map(Json.jArray)
    )

  implicit val jsObjectArb: Gen[JsonObject] =
    Gen.listOfN(
      6,
      Gen.tuple2(Gen[String], jsValueArb)
    ).map(JsonObject.fromIterable(_))

  implicit val jsArrayArb: Gen[JsonArray] =
    Gen.listOfN(6, jsValueArb)

  protected[this] def packer(): MsgPacker
  protected[this] def unpacker(bytes: Array[Byte]): MsgUnpacker

  private[this] def checkRoundTripBytes[A](implicit A: MsgpackCodec[A], G: Gen[A], E: Equal[A]) =
    Property.forAll { a: A =>
      A.roundtripz(a, packer(), unpacker _) match {
        case None =>
          true
        case Some(\/-(b)) =>
          println("fail roundtrip bytes " + a + " " + b)
          false
        case Some(-\/(e)) =>
          println(e)
          false
      }
    }

  val testJson = {
    implicit val instance = ArgonautMsgpack.jsonCodec(
      ArgonautUnpackOptions.default
    )
    checkRoundTripBytes[Json]
  }

  val testJsonObject = {
    implicit val instance = ArgonautMsgpack.jsonObjectCodec(
      ArgonautUnpackOptions.default
    )
    checkRoundTripBytes[JsonObject]
  }

  val testJsonArray = {
    implicit val instance = ArgonautMsgpack.jsonArrayCodec(
      ArgonautUnpackOptions.default
    )
    checkRoundTripBytes[JsonArray]
  }
}

object NativeSpec extends SpecBase {
  override protected[this] def packer() = MsgOutBuffer.create()
  override protected[this] def unpacker(bytes: Array[Byte]) = MsgInBuffer(bytes)
}
