package inmemdb.store

trait Encoder[From, To]: 
  def encode(value: From): To

trait Decoder[From, To]: 
  def decode(value: To): Either[String, From]

trait Codec[From, To] extends Encoder[From, To] with Decoder[From, To]

// type UniqueIndex

enum PrimitiveValue:
  case Str(content: String)
  case Bool(content: Boolean)
  case Num(content: Long)

enum CompositeValue:
  case Opt(content: Option[CompositeValue])
  case Arr(content: List[PrimitiveValue])

type IndexableValue = PrimitiveValue | CompositeValue

trait DocumentSchema[T, K]:

  case class Field[I](
    name: String, 
    select: T => I,
    shouldIndex: Boolean = false
  ) (using Decoder[I, String], Encoder[I, IndexableValue]) {
    def stringDecoder: Decoder[I, String] = stringDecoder
    def indexEncoder: Encoder[I, IndexableValue] = indexEncoder
  }

  // type DocumentType = T
  // type PrimaryKeyType = K

  def name: String
  def primary: Field[K]
  def fields: List[Field[?]]
