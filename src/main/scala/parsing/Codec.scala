package parsing

import io.circe.Decoder
import io.circe.{Codec, HCursor, DecodingFailure}
import Domain.{*, given}
import io.circe.generic.semiauto.*

given Codec[User] = deriveCodec[User]
given Codec[Ticket] = deriveCodec[Ticket]

// given Codec[User] with
//   def apply(c: HCursor): Either[DecodingFailure, User] =

// type JsonRoot = JsonValue.JsonArray | JsonValue.JsonObject
enum JsonDocumentRelation:
  case None
  case OneToOne

enum JsonValue[T](content: T):
  case JsonBoolean(b: Boolean) extends JsonValue[Boolean](b)
  case JsonNumber(n: Long) extends JsonValue[Long](n)
  case JsonString(str: String) extends JsonValue[String](str)
  case JsonObject(fields: List[JsonField[T]]) extends JsonValue[List[JsonField[T]]](fields)
  case JsonArray(values: List[JsonValue[T]]) extends JsonValue[List[JsonValue[T]]](values)

case class JsonField[T](field: String, value: JsonValue[T])

enum JsonDataType:
  case JsonBoolean
  case JsonNumber
  case JsonString
  case JsonObject(fields: List[(String, JsonDataType)])
  case JsonArray(contentType: JsonDataType)
  case Optional(inner: JsonDataType)

type IndexDataType = 
    JsonDataType.JsonBoolean.type
  | JsonDataType.JsonNumber.type
  | JsonDataType.JsonString.type

case class JsonDocumentSchema[Key, V](
  schema: List[JsonProperty],
  primarySelector: V => Key,
  indexSelector: V => Key
  // indexes: 
)

// case class JsonStored[User()

trait JsonDocument[T]:

  case class JsonProperty(
    name: String,
    propertyType: JsonDataType,
    selector: T => JsonDataType,
    createIndex: Boolean,
  )

  def schema: List[JsonProperty]

given JsonDocument[User] with
  def schema: List[JsonProperty] = List(
    JsonProperty("_id", JsonDataType.JsonString, _._id, true),
    JsonProperty("name", JsonDataType.JsonString, true),
    JsonProperty("created_at", JsonDataType.JsonString, true),
    JsonProperty("verified", JsonDataType.JsonBoolean, true)
  )

given JsonDocument[Ticket] with
  def schema: List[JsonProperty] = List(
    JsonProperty("_id", JsonDataType.JsonString, true),
    JsonProperty("created_at", JsonDataType.JsonString, true),
    JsonProperty("type", JsonDataType.JsonString, true),
    JsonProperty("subject", JsonDataType.JsonBoolean, true),
    JsonProperty("assignee_id", JsonDataType.JsonString, true),
    JsonProperty(
      "tags", JsonDataType.JsonArray(JsonDataType.JsonString), true
    )
  )

trait DocumentStore:
  def addBulk[T](): Unit
