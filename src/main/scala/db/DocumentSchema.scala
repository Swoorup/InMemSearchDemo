package inmemdb.db

trait Encoder[From, To]:
  def encode(value: From): To

trait Decoder[From, To]:
  def decode(value: To): Either[String, From]

type IndexEncoder[T] = Encoder[T, IndexValue]
type InputDecoder[T] = Decoder[T, String]

enum IndexPrimitiveValue:
  case Str(content: String)
  case Bool(content: Boolean)
  case Num(content: Long)

enum IndexCompositeValue:
  case Opt(content: Option[IndexValue])
  case Arr(content: List[IndexValue])

type IndexValue = IndexPrimitiveValue | IndexCompositeValue

trait DocumentSchema[T, K] {
  case class IndexField[I](name: String, select: T => I)(using
      InputDecoder[I],
      IndexEncoder[I]
  ) {
    def inputDecoder: InputDecoder[I] = summon[InputDecoder[I]]
    def indexEncoder: IndexEncoder[I] = summon[IndexEncoder[I]]
  }

  def name: String
  def primary: IndexField[K]
  def nonPrimary: List[IndexField[?]]
  def allFields: List[IndexField[?]] = primary :: nonPrimary
}
