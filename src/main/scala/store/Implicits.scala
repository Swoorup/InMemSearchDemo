package inmemdb.store

object Implicits: 
  import java.time.OffsetDateTime
  import scala.util.Try

  given Codec[Long, String] with
    def encode(v: Long) = v.toString
    def decode(v: String) = v.toLongOption.toRight("Failed to parse number.")

  given Codec[Boolean, String] with
    def encode(v: Boolean) = v.toString
    def decode(v: String) = v.toBooleanOption.toRight("Failed to parse bool.")

  given Codec[String, String] with
    def encode(v: String) = v
    def decode(v: String) = Right(v)

  given Codec[OffsetDateTime, String] with
    def encode(v: OffsetDateTime) = v.toString
    def decode(v: String) = Try { OffsetDateTime.parse(v) }.toOption.toRight("Failed to parse date time offset.")
    

  given [From, To](using codec: Codec[From, To]): Encoder[From, To] = codec
  given [From, To](using codec: Codec[From, To]): Decoder[From, To] = codec

  given indexableValues: Encoder[IndexableValue, List[PrimitiveValue]] with
    def encode(value: IndexableValue) = value match {
      case t:PrimitiveValue => List(t)
      case CompositeValue.Opt(content) => content.toList.map(encode).flatten
      case CompositeValue.Arr(content) => content.map(encode).flatten
    }

  // decoders

  // encoders for pickling
  given Encoder[Long, IndexableValue] with
    def encode(v: Long) = PrimitiveValue.Num(v)
  given Encoder[String, IndexableValue] with
    def encode(v: String) = PrimitiveValue.Str(v)
  given Encoder[Boolean, IndexableValue] with
    def encode(v: Boolean) = PrimitiveValue.Bool(v)
  given Encoder[OffsetDateTime, IndexableValue] with
    def encode(v: OffsetDateTime) = PrimitiveValue.Str(v.toString)