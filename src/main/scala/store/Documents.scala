package inmemdb.store

import inmemdb.common.*
import inmemdb.store.DocumentSchema
import cats.implicits.*

case class IndexData[I, K](
    indexToPrimary: Map[IndexPrimitiveValue, Set[K]],
    stringDecoder: InputDecoder[I],
    indexEncoder: IndexEncoder[I]
) {
  type IndexDataType = IndexData[I, K]

  def merge(other: IndexData[I, K]): IndexData[I, K] =
    this.copy(indexToPrimary = (this.indexToPrimary merge other.indexToPrimary) { _ ++ _ })

  def tryParseToIndexPrimitive(s: String): Either[String, IndexPrimitiveValue] =
    stringDecoder
      .decode(s)
      .map(indexEncoder.encode)
      .map(Implicits.indexableValues.encode)
      .map(_.head)
}

case class Documents[T, K](
    all: Map[K, T],
    indexData: Map[String, IndexData[?, K]]
) {
  def merge(other: Documents[T, K]): Documents[T, K] = {
    val newIndexes = (this.indexData merge other.indexData) {
      // asInstanceOf is a safe operation as both the fields should have the same type
      (idx1, idx2) => idx1.merge(idx2.asInstanceOf[idx1.IndexDataType])
    }

    this.copy(this.all ++ other.all, newIndexes)
  }

  def addDocument(using DocumentSchema[T, K])(document: T): Documents[T, K] =
    this.merge(Documents.fromSingle(document))
}

object Documents:
  def empty[T, K]: Documents[T, K] = Documents(Map.empty, Map.empty)

  def fromSingle[T, K](using schema: DocumentSchema[T, K])(
      document: T
  ): Documents[T, K] =
    val currentIndexes = schema.nonPrimary.map { field =>
      val primaryKey        = schema.primary.select(document)
      val index             = field.indexEncoder.encode(field.select(document))
      val primitiveIndex    = Implicits.indexableValues.encode(index)
      val nonUniqueIndexMap = primitiveIndex.map(_ -> Set(primaryKey)).toMap
      field.name -> IndexData(
        nonUniqueIndexMap,
        field.inputDecoder,
        field.indexEncoder
      )
    }.toMap

    Documents(
      all = Map(schema.primary.select(document) -> document),
      indexData = currentIndexes
    )

  def apply[T, K](using schema: DocumentSchema[T, K])(
      documents: List[T]
  ): Documents[T, K] =
    documents.map(fromSingle).fold(empty)(_.merge(_))
