// package parsing

import io.circe.Decoder
import io.circe.{Codec, HCursor, DecodingFailure}
import Domain.{*, given}
import io.circe.generic.semiauto.*

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

// enum FieldValue[T](content: T):
//   case Bool(b: Boolean) extends FieldValue[Boolean](b)
//   case Num(n: Long) extends FieldValue[Long](n)
//   case Str(str: String) extends FieldValue[String](str)
//   case Obj(fields: List[FieldField[T]]) extends FieldValue[List[FieldField[T]]](fields)
//   case Arr(values: List[FieldValue[T]]) extends FieldValue[List[FieldValue[T]]](values)

  // case Arr(values: List[FieldSelection[T]]) extends FieldValue[List[FieldValue[T]]](values)


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


trait DocumentSchema[T]:
  // case class Field(name: String, selector: FieldSelection)

  // enum FieldSelection:
  //   case Bool(s: T => Boolean) 
  //   case Num(s: T => Long) 
  //   case Str(s: T => String) 
  //   case Obj(s: T => List[Field])
  //   case Arr(s: T => List[FieldSelection])


  // case class Field(name: String, selector: FieldSelection)
  enum Field(name: String):
    case Bool(name: String, selector: T => Boolean) extends Field(name)
    case Num(name: String, selector: T => Long) extends Field(name)
    case Str(name: String, selector: T => String) extends Field(name)
    case Arr(name: String, selector: T => List[Field]) extends Field(name)
    case Obj(name: String, fields: List[Field]) extends Field(name)

  case class Property(
    name: String,
    // propertyType: JsonDataType,
    selector: T => PropertyValue,
    createIndex: Boolean
  )

  type PropertySelector = T => PropertyValue
  def StringProperSelector(s: T => String): PropertySelector = v => PropertyValue.Str (s(v))
    def schema: List[Property]

given DocumentSchema[User] with
  def schema = List(
    Field.Num("_id", StringProperSelector(_._id), true),
    Field.("name", x => PropertyValue.Str(x.name), true),
    Field.("created_at", _.createdAt, true),
    Field.("verified", _.verified, true)
  )

given DocumentSchema[Ticket] with
  def schema = (
    Property("_id", true),
    Property("created_at", true),
    Property("type", true),
    Property("subject", true),
    Property("assignee_id", true),
    Property("tags", true)
  )

trait Key { type Value }
trait Document { type Key }

trait DocumentStore:
  def bulkInsert(objects: List[Document]): Unit
