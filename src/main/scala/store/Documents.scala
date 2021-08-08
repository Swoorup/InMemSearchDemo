package inmemdb.store

import inmemdb.common.*

case class Documents[T, K](
  all: Map[K, T],
  indexes: Map[String, Map[PrimitiveValue, Set[K]]]
) { 
  def merge(other: Documents[T, K]): Documents[T, K] = {
    val newIndexes = (this.indexes merge other.indexes) { 
      case (indexMap1, indexMap2) => (indexMap1 merge indexMap2) {_ ++ _}
    }

    this.copy(this.all ++ other.all, newIndexes)
  }

  def addDocument(using S: DocumentSchema[T, K])(document: T): Documents[T, K] = 
    this.merge(Documents.fromSingle(document))
}

object Documents:
  def empty[T, K]: Documents[T, K] = Documents(Map.empty, Map.empty)

  def fromSingle[T, K](using S: DocumentSchema[T, K])(document: T): Documents[T, K] = 
    val currentIndexes = S.fields.map{ field => 
      val primaryKey = S.primary.select(document)
      val index = field.indexEncoder.encode(field.select(document))
      val primitiveIndex = Implicits.indexableValues.encode(index)
      val nonUniqueIndexMap = primitiveIndex.map(_ -> Set(primaryKey)).toMap
      field.name -> nonUniqueIndexMap
    }.toMap

    Documents(all = Map(S.primary.select(document) -> document), indexes = currentIndexes)

  def apply[T, K](using S: DocumentSchema[T, K])(documents: List[T]): Documents[T, K] = 
    documents.map(fromSingle).fold(empty)(_.merge(_))
