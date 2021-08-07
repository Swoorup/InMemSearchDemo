import scala.deriving.Mirror
// package parsing

import io.circe.Decoder
import io.circe.{Codec, HCursor, DecodingFailure}
import Domain.{*, given}
import io.circe.generic.semiauto.*
import java.time.OffsetDateTime

given Codec[User] = deriveCodec[User]
given Codec[Ticket] = deriveCodec[Ticket]

// given Codec[User] with
//   def apply(c: HCursor): Either[DecodingFailure, User] =

// type JsonRoot = JsonValue.JsonArray | JsonValue.JsonObject
// enum JsonDocumentRelation:
//   case None
//   case OneToOne

enum PropertyValue:
  case Bool(b: Boolean) 
  case Num(n: Long) 
  case Str(str: String) 
  case Obj(fields: List[PropertyField]) 
  case Arr(values: List[PropertyValue]) 

case class PropertyField(field: String, value: PropertyValue)

// enum PropertyValue[T](content: T):
//   case Bool(b: Boolean) extends PropertyValue[Boolean](b)
//   case Num(n: Long) extends PropertyValue[Long](n)
//   case Str(str: String) extends PropertyValue[String](str)
//   case Obj(fields: List[PropertyField[T]]) extends PropertyValue[List[PropertyField[T]]](fields)
//   case Arr(values: List[PropertyValue[T]]) extends PropertyValue[List[PropertyValue[T]]](values)

// case class PropertyField[T](field: String, value: PropertyValue[T])

// enum JsonDataType:
//   case JsonBoolean
//   case JsonNumber
//   case JsonString
//   case JsonObject(fields: List[(String, JsonDataType)])
//   case JsonArray(contentType: JsonDataType)
//   case Optional(inner: JsonDataType)

// type IndexDataType =
//     JsonDataType.JsonBoolean.type
//   | JsonDataType.JsonNumber.type
//   | JsonDataType.JsonString.type

// case class JsonDocumentSchema[Key, V](
//   schema: List[JsonProperty],
//   primarySelector: V => Key,
//   indexSelector: V => Key
// )

// case class JsonStored[User()

// trait FieldSelector[T]:
//   def selector(value: T): String

// given FieldSelector[String] with
//   def selector(value: String): String = value

// given FieldSelector[Long] with
//   def selector(value: Long): String = value.toString

// given FieldSelector[Boolean] with
//   def selector(value: Boolean): String = value.toString

// given [T]:FieldSelector[List[T]] with
//   def selector(value: List[T]): String = value.toString

case class Simple(name: String, age: Int) 
case class Field[H](name: String)

trait Document[T]:
  // def schema: Tuple.Map[Mirror.ProductOf[T], Field]
  def schema[A](using M: Mirror.ProductOf[T], ev: M.MirroredMonoType =:= A): A

// type UserSchema = UserId *: String *: OffsetDateTime *: Boolean
given Document[Simple] with
  // given Tuple.Map[UserId, Field] = using
  def schema = (
    // Field[String]("_id"),
    // Field[Int]("name"),
    "Sas", 121

    // Field[UserId]("_id"),
    // Field[String]("name"),
    // Field[OffsetDateTime]("created_at"),
    // Field[Boolean]("verified")
  )

type FieldDecode[T] = T match
  case Field[T] => String
  case Any => String
  case _ => String

// def name[T](value: T): FieldDecode[T] = value match
//   case field: Field[t] => field.name
//   case _: Any => ""


// inline def toConstructor[D: Document <: Product](d: D) = 
//   val s = summon[Document[D]].schema
//   s.map[[X] =>> String] { [T] => (t: T) => "" }
//   s