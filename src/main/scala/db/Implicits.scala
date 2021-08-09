package inmemdb.db

object Implicits:
  import java.time.OffsetDateTime
  import java.util.UUID
  import scala.util.Try

  // default index encoders
  given IndexEncoder[Long]           = IndexPrimitiveValue.Num(_)
  given IndexEncoder[String]         = IndexPrimitiveValue.Str(_)
  given IndexEncoder[Boolean]        = IndexPrimitiveValue.Bool(_)
  given IndexEncoder[OffsetDateTime] = v => IndexPrimitiveValue.Str(v.toString)
  given IndexEncoder[UUID]           = v => IndexPrimitiveValue.Str(v.toString)

  given [T](using inner: IndexEncoder[T]): IndexEncoder[Option[T]] with
    def apply(vOpt: Option[T]) = IndexCompositeValue.Opt(vOpt.map(inner(_)))

  given [T](using inner: IndexEncoder[T]): IndexEncoder[List[T]] with
    def apply(vList: List[T]) = IndexCompositeValue.Arr(vList.map(inner(_)))

  // default input decoders
  given InputDecoder[Long]           = _.toLongOption.toRight("Failed to parse number.")
  given InputDecoder[Boolean]        = _.toBooleanOption.toRight("Failed to parse bool.")
  given InputDecoder[String]         = Right(_)
  given InputDecoder[OffsetDateTime] = v => Try(OffsetDateTime.parse(v)).toOption.toRight("Failed to parse date time offset.")
  given InputDecoder[UUID]           = v => Try(UUID.fromString(v)).toOption.toRight("Failed to parse UUID.")

  given [T](using inner: InputDecoder[T]): InputDecoder[Option[T]] with
    def apply(v: String) = inner(v).map(Some(_))

  given [T](using inner: InputDecoder[T]): InputDecoder[List[T]] with
    def apply(v: String) = inner(v).map(List(_))
