package inmemdb.db

import inmemdb.common.*
import inmemdb.db.DocumentSchema

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

  def fromSingle[T, K](using schema: DocumentSchema[T, K])(document: T): Documents[T, K] = {
    Documents(
      all = Map(schema.primary.select(document) -> document),
      indexData = schema.nonPrimary.map(field => field.name -> IndexData(document, field)).toMap
    )
  }

  def apply[T, K](using schema: DocumentSchema[T, K])(documents: List[T]): Documents[T, K] =
    documents.map(fromSingle).fold(empty)(_.merge(_))
