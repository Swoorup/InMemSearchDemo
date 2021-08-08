package inmemdb.store

// import Domain.{*, given}
import cats.Applicative
import cats.implicits.*
import cats.effect.{Async, Ref, Sync}
import java.time.OffsetDateTime

// given Encoder[String, PrimitiveValue] with { def encode(value: String) = PrimitiveValue.Str(value) }
// given Encoder[Long, PrimitiveValue] with { def encode(value: Long) = PrimitiveValue.Num(value) }
// given Encoder[Boolean, PrimitiveValue] with { def encode(value: Boolean) = PrimitiveValue.Bool(value) }
// given Decoder[String, IndexedValue.Bool] with 
//   def decode(value: String) = value.toBooleanOption.map

// trait Indexer[K, I]: 
//   def search(value: I): List[K]


// def test = 
//   val documents: Documents[User, UserId] = ???
//   // search "name" field using "Francisca Rasmussen"
//   val indexMap = documents.indexes("name")
//   val resultKeys = indexMap(PrimitiveValue.Str("Francisca Rasmussen"))
//   val result = resultKeys.map(userId => documents.all(userId))
//   ???

type DocumentStoreError = String

trait DocumentStore[F[_]]:
  def bulkInsert[T, K](using DocumentSchema[T, K])(objects: List[T]): F[Unit]
  def lookUp[T, K](using DocumentSchema[T, K])(key: K): F[Option[T]]
  def searchByField[T, K](using DocumentSchema[T, K])(field: String, value: String): F[Either[DocumentStoreError, List[T]]]

object DocumentStore:
  def apply[F[_]: Async]: F[DocumentStore[F]] = 
    for {
      documentsRef <- Ref.of[F, Map[DocumentSchema[?, ?], Documents[?, ?]]](Map.empty)
      store <- Sync[F].delay(new DocumentStoreImpl[F](documentsRef))
    } yield store

private class DocumentStoreImpl[F[_]: Async](
  documentsSchemaMapRef: Ref[F, Map[DocumentSchema[?, ?], Documents[?, ?]]]
) extends DocumentStore[F] {
  import Implicits.*

  def bulkInsert[T, K](using schema: DocumentSchema[T, K])(objects: List[T]): F[Unit] =
    for {
      documentsSchemaMap    <- documentsSchemaMapRef.get
      documentsToInsert     <- Sync[F].delay(Documents(objects))
      documentsOpt          <- Sync[F].delay(documentsSchemaMap.get(schema))
      updatedDocuments      <- Sync[F].delay {documentsOpt match
                                case None => documentsToInsert
                                case Some(docs) => docs.asInstanceOf[Documents[T, K]].merge(documentsToInsert)
                              }
      _                     <- documentsSchemaMapRef.modify(ds => (ds.+(schema -> updatedDocuments), ds))
    } yield ()

  def lookUp[T, K](using schema: DocumentSchema[T, K])(key: K): F[Option[T]] = 
    for {
      documentsSchemaMap    <- documentsSchemaMapRef.get
      documentsOpt          <- Sync[F].delay(documentsSchemaMap.get(schema))
    } yield {
      for {
        documents <- documentsOpt
        document <- documents.all.asInstanceOf[Map[K, T]].get(key)
      } yield {
        document.asInstanceOf[T]
      }
    }


  def searchByField[T, K](using schema: DocumentSchema[T, K])(field: String, value: String): F[Either[DocumentStoreError, List[T]]] = 
    for {
      documentsSchemaMap    <- documentsSchemaMapRef.get
      documentsOpt          <- Sync[F].delay(documentsSchemaMap.get(schema))
    } yield {
      for {
        documents <- Either.fromOption(documentsOpt, s"No data present for schema ${schema.name}")
        primaryKeys <- 
          if field == schema.primary.name then 
            // search using primary indexes
            schema.primary.stringDecoder.decode(value).map(Set(_))
          else 
            // search using non primary indexes
            for { 
              indexData <- Either.fromOption(documents.indexData.get(field), s"Schema ${schema.name} does not have field $field")
              indexToSearch <- indexData.tryParseToIndexPrimitive(value)
            } yield indexData.map.get(indexToSearch).getOrElse(Set.empty).asInstanceOf[Set[K]]
      } yield {
        primaryKeys
          .toList
          .map(key => documents.all.asInstanceOf[Map[K, T]].get(key))
          .flattenOption
      }
    }
}