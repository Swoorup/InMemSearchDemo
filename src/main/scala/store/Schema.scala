package inmemdb.store

trait Encoder[From, To]: 
  def encode(value: From): To

trait Decoder[From, To]: 
  def decode(value: To): Either[String, From]

trait Codec[From, To] extends Encoder[From, To] with Decoder[From, To]

type IndexEncoder[T] = Encoder[T, IndexableValue]
type InputDecoder[T] = Decoder[T, String]

enum PrimitiveValue:
  case Str(content: String)
  case Bool(content: Boolean)
  case Num(content: Long)

enum CompositeValue:
  case Opt(content: Option[IndexableValue])
  case Arr(content: List[IndexableValue])

type IndexableValue = PrimitiveValue | CompositeValue

trait DocumentSchema[T, K]:

  case class Field[I](
    name: String, 
    select: T => I,
    shouldIndex: Boolean = false
  ) (using InputDecoder[I], IndexEncoder[I]) {
    def stringDecoder: InputDecoder[I] = summon[InputDecoder[I]]
    def indexEncoder: IndexEncoder[I] = summon[IndexEncoder[I]]
  }

  def name: String
  def primary: Field[K]
  def fields: List[Field[?]]
