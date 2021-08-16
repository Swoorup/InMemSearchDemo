package inmemdb.db

trait Encoder[From, To]:
  def apply(value: From): To

trait Decoder[From, To]:
  def apply(value: To): Either[String, From]

/** Describes encoding the field type to the index primitives
*/
type IndexEncoder[T] = Encoder[T, IndexValue]

/** Describes decoding string to the field of the object
*/
type InputDecoder[T] = Decoder[T, String]

/** The primitive value in which all the fields of the document are represented in the database.
  */
enum IndexPrimitiveValue:
  case Str(content: String)
  case Bool(content: Boolean)
  case Num(content: Long)
  case Empty

/** The composite index allows supports index support to compound types like Option of Arrays, Arrays, etc
  */
enum IndexCompositeValue:
  case Opt(content: Option[IndexValue])
  case Arr(content: List[IndexValue])

type IndexValue = IndexPrimitiveValue | IndexCompositeValue

object IndexValue:
  def decomposeToPrimitive(value: IndexValue): List[IndexPrimitiveValue] = value match {
    case t: IndexPrimitiveValue           => List(t)
    case IndexCompositeValue.Opt(content) => content.map(decomposeToPrimitive) getOrElse List(IndexPrimitiveValue.Empty)
    case IndexCompositeValue.Arr(content) => content.map(decomposeToPrimitive).flatten
  }
