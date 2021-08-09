package inmemdb.db

/** Trait that describes the schema of the document. `T` is the type of the document, whereas `K` is the primary key of the document
  */
trait DocumentSchema[T, K] {

  /** Describes individual index field of the document
    */
  case class IndexField[I](name: String, select: T => I)(using
      inputDecoder: InputDecoder[I],
      indexEncoder: IndexEncoder[I]
  ) {
    def decodeInput(str: String): Either[String, I] =
      inputDecoder(str)

    def pickleFieldFromDocument(document: T): List[IndexPrimitiveValue] =
      val encoded = indexEncoder(this.select(document))
      IndexValue.decomposeToPrimitive(encoded)

    def getIndexPrimitive(s: String): Either[String, IndexPrimitiveValue] = {
      inputDecoder(s)
        .map(indexEncoder.apply)
        .map(IndexValue.decomposeToPrimitive)
        .map(_.head)
    }
  }

  def name: String
  def primary: IndexField[K]
  def nonPrimary: List[IndexField[?]]
  def allFields: List[IndexField[?]] = primary :: nonPrimary
}
