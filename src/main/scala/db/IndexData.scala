package inmemdb.db

import inmemdb.common.*
import inmemdb.db.DocumentSchema
import cats.implicits.*

case class IndexData[I, K](
    indexToPrimary: Map[IndexPrimitiveValue, Set[K]],
    stringDecoder: InputDecoder[I],
    indexEncoder: IndexEncoder[I]
) {
  type IndexDataType = IndexData[I, K]

  def merge(other: IndexData[I, K]): IndexData[I, K] =
    this.copy(indexToPrimary = (this.indexToPrimary merge other.indexToPrimary) { _ ++ _ })

  def tryParseToIndexPrimitive(s: String): Either[String, IndexPrimitiveValue] = {
    stringDecoder
      .decode(s)
      .map(indexEncoder.encode)
      .map(Implicits.indexableValues.encode)
      .map(_.head)
  }
}

object IndexData:
  def apply[T, K, I](using schema: DocumentSchema[T, K])(document: T, field: schema.IndexField[I]): IndexData[I, K] = {
    val primaryKey            = schema.primary.select(document)
    val encodedIndex          = field.indexEncoder.encode(field.select(document))
    val encodedPrimitiveIndex = Implicits.indexableValues.encode(encodedIndex)
    val nonUniqueIndexMap     = encodedPrimitiveIndex.map(_ -> Set(primaryKey)).toMap

    IndexData(nonUniqueIndexMap, field.inputDecoder, field.indexEncoder)
  }
