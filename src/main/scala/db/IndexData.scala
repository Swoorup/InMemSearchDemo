package inmemdb.db

import inmemdb.common.*
import inmemdb.db.DocumentSchema

/** Container for a single index field data for the document
  */
case class IndexData[T, K, I](
    indexToPrimary: Map[IndexPrimitiveValue, Set[K]],
    field: DocumentSchema[T, K]#IndexField[I]
) {
  type IndexDataType = IndexData[T, K, I]

  def merge(other: IndexData[T, K, I]): IndexData[T, K, I] =
    this.copy(indexToPrimary = (this.indexToPrimary merge other.indexToPrimary) { _ ++ _ })
}

extension [T, K](indexData: IndexData[T, K, String])
  def partialMatch(searchString: String): Set[K] = {
    indexData.indexToPrimary.keySet
    .map { 
      case IndexPrimitiveValue.Str(strContent) => Some(strContent) 
      case _ => None
    }
    .flatten
    .filter(_.contains(searchString))
    .map(idx => indexData.indexToPrimary.get(IndexPrimitiveValue.Str(idx)))
    .flatten
    .flatten
  }

object IndexData:
  def fromDocumentField[T, K, I](using schema: DocumentSchema[T, K])(document: T, field: schema.IndexField[I]): IndexData[T, K, I] = {
    val primaryKey            = schema.primary.select(document)
    val encodedPrimitiveIndex = field.pickleFieldFromDocument(document)
    val nonUniqueIndexMap     = encodedPrimitiveIndex.map(_ -> Set(primaryKey)).toMap

    IndexData(nonUniqueIndexMap, field)
  }
