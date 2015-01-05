package msgpack6z

import argonaut._
import argonaut.Json.JsonArray
import msgpack4z._
import org.scalacheck.{Gen, Arbitrary, Prop, Properties}
import scalaz.{-\/, Equal, \/-}
import scalaz.std.list._

abstract class SpecBase(name: String) extends Properties(name){

  private val bigDecimalGen: Gen[BigDecimal] =
    gen[Double].map(BigDecimal(_))

  private val jsonNumberGen: Gen[JsonNumber] =
    Gen.oneOf(
      bigDecimalGen.map(JsonBigDecimal),
      gen[Long].map(JsonLong),
      gen[Double].map(JsonDouble),
      bigDecimalGen.map(a => JsonNumber.fromString(a.toString).get)
    )

  private val jsValuePrimitivesArb: Arbitrary[Json] =
    Arbitrary(Gen.oneOf(
      Gen.const(Json.jNull),
      Gen.const(Json.jTrue),
      Gen.const(Json.jFalse),
      jsonNumberGen.map(Json.jNumber(_)),
      gen[String].map(Json.jString)
    ))

  private val jsObjectArb1: Arbitrary[JsonObject] =
    Arbitrary(Gen.choose(0, 6).flatMap(n =>
      Gen.listOfN(
        n,
        Arbitrary.arbTuple2(
          arb[String], jsValuePrimitivesArb
        ).arbitrary
      ).map(JsonObject.from(_))
    ))

  private val jsArrayArb1: Arbitrary[JsonArray] =
    Arbitrary(Gen.choose(0, 6).flatMap(n =>
      Gen.listOfN(n, jsValuePrimitivesArb.arbitrary)
    ))

  implicit val jsValueArb: Arbitrary[Json] =
    Arbitrary(Gen.oneOf(
      jsValuePrimitivesArb.arbitrary,
      jsObjectArb1.arbitrary.map(Json.jObject),
      jsArrayArb1.arbitrary.map(Json.jArray)
    ))

  implicit val jsObjectArb: Arbitrary[JsonObject] =
    Arbitrary(Gen.choose(0, 6).flatMap(n =>
      Gen.listOfN(
        n,
        Arbitrary.arbTuple2(arb[String], jsValueArb).arbitrary
      ).map(JsonObject.from(_))
    ))

  implicit val jsArrayArb: Arbitrary[JsonArray] =
    Arbitrary(Gen.choose(0, 6).flatMap(n =>
      Gen.listOfN(n, jsValueArb.arbitrary)
    ))

  final def gen[A: Arbitrary]: Gen[A] =
    implicitly[Arbitrary[A]].arbitrary

  final def arb[A: Arbitrary]: Arbitrary[A] =
    implicitly[Arbitrary[A]]

  protected[this] def packer(): MsgPacker
  protected[this] def unpacker(bytes: Array[Byte]): MsgUnpacker

  private def checkRoundTripBytes[A](implicit A: MsgpackCodec[A], G: Arbitrary[A], E: Equal[A]) =
    Prop.forAll { a: A =>
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

  property("Json") = {
    implicit val instance = ArgonautMsgpack.jsonCodec(
      ArgonautUnpackOptions.default
    )
    checkRoundTripBytes[Json]
  }

  property("JsonObject") = {
    implicit val instance = ArgonautMsgpack.jsonObjectCodec(
      ArgonautUnpackOptions.default
    )
    checkRoundTripBytes[JsonObject]
  }

  property("JsonArray") = {
    implicit val instance = ArgonautMsgpack.jsonArrayCodec(
      ArgonautUnpackOptions.default
    )
    checkRoundTripBytes[JsonArray]
  }
}

object Java06Spec extends SpecBase("java06"){
  override protected[this] def packer() = Msgpack06.defaultPacker()
  override protected[this] def unpacker(bytes: Array[Byte]) = Msgpack06.defaultUnpacker(bytes)
}

object Java07Spec extends SpecBase("java07"){
  override protected[this] def packer() = new Msgpack07Packer()
  override protected[this] def unpacker(bytes: Array[Byte]) = Msgpack07Unpacker.defaultUnpacker(bytes)
}

object NativeSpec extends SpecBase("native"){
  override protected[this] def packer() = MsgOutBuffer.create()
  override protected[this] def unpacker(bytes: Array[Byte]) = MsgInBuffer(bytes)
}
