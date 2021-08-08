package inmemdb.store

object Implicits: 
  import java.time.OffsetDateTime
  import java.util.UUID
  import scala.util.Try

  // given [From, To](using codec: Codec[From, To]): Encoder[From, To] = codec
  // given [From, To](using codec: Codec[From, To]): Decoder[From, To] = codec

  // default index encoders
  given IndexEncoder[Long] with
    def encode(v: Long) = PrimitiveValue.Num(v)

  given IndexEncoder[String] with
    def encode(v: String) = PrimitiveValue.Str(v)

  given IndexEncoder[Boolean] with
    def encode(v: Boolean) = PrimitiveValue.Bool(v)

  given IndexEncoder[OffsetDateTime] with
    def encode(v: OffsetDateTime) = PrimitiveValue.Str(v.toString)

  given IndexEncoder[UUID] with
    def encode(v: UUID) = PrimitiveValue.Str(v.toString)

  given [T](using inner: IndexEncoder[T]): IndexEncoder[Option[T]] with
    def encode(vOpt: Option[T]) = CompositeValue.Opt(vOpt.map(inner.encode(_))) 

  given [T](using inner: IndexEncoder[T]): IndexEncoder[List[T]] with
    def encode(vList: List[T]) = CompositeValue.Arr(vList.map(inner.encode(_))) 

  given indexableValues: Encoder[IndexableValue, List[PrimitiveValue]] with
    def encode(value: IndexableValue) = value match {
      case t:PrimitiveValue => List(t)
      case CompositeValue.Opt(content) => content.toList.map(encode).flatten
      case CompositeValue.Arr(content) => content.map(encode).flatten
    }

  // default input decoders
  given InputDecoder[Long] with
    def decode(v: String) = v.toLongOption.toRight("Failed to parse number.")

  given InputDecoder[Boolean] with
    def decode(v: String) = v.toBooleanOption.toRight("Failed to parse bool.")

  given InputDecoder[String] with
    def decode(v: String) = Right(v)

  given InputDecoder[OffsetDateTime] with
    def decode(v: String) = Try { OffsetDateTime.parse(v) }.toOption.toRight("Failed to parse date time offset.")
  
  given InputDecoder[UUID] with
    def decode(v: String) = Try { UUID.fromString(v) }.toOption.toRight("Failed to parse UUID.")

  given [T](using inner: InputDecoder[T]):InputDecoder[Option[T]] with
    def decode(v: String) = inner.decode(v).map(Some(_))

  given [T](using inner: InputDecoder[T]):InputDecoder[List[T]] with
    def decode(v: String) = inner.decode(v).map(List(_))