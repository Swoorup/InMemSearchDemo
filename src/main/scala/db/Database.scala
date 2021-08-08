package inmemdb.db

import cats.Applicative
import cats.implicits.*
import cats.effect.{Async, Ref, Sync}
import java.time.OffsetDateTime

type DatabaseError = String

trait Database[F[_]]:
  def bulkInsert[T, K](using DocumentSchema[T, K])(objects: List[T]): F[Unit]
  def lookUp[T, K](using DocumentSchema[T, K])(key: K): F[Option[T]]
  def searchByField[T, K](using DocumentSchema[T, K])(field: String, value: String): F[Either[DatabaseError, List[T]]]

object Database:
  def apply[F[_]: Async]: F[Database[F]] = {
    for {
      documentsRef <- Ref.of[F, Map[DocumentSchema[?, ?], Documents[?, ?]]](Map.empty)
      store        <- Sync[F].delay(new DatabaseImpl[F](documentsRef))
    } yield store
  }

private class DatabaseImpl[F[_]: Async](
    documentsSchemaMapRef: Ref[F, Map[DocumentSchema[?, ?], Documents[?, ?]]]
) extends Database[F] {
  import Implicits.*

  def bulkInsert[T, K](using schema: DocumentSchema[T, K])(objects: List[T]): F[Unit] = {
    for {
      documentsSchemaMap <- documentsSchemaMapRef.get
      documentsToInsert  <- Sync[F].delay(Documents(objects))
      documentsOpt       <- Sync[F].delay(documentsSchemaMap.get(schema))
      updatedDocuments <- Sync[F].delay {
        documentsOpt match
          case None       => documentsToInsert
          case Some(docs) => docs.asInstanceOf[Documents[T, K]].merge(documentsToInsert)
      }
      _ <- documentsSchemaMapRef.modify(ds => (ds.+(schema -> updatedDocuments), ds))
    } yield ()
  }

  def lookUp[T, K](using schema: DocumentSchema[T, K])(key: K): F[Option[T]] = {
    for {
      documentsSchemaMap <- documentsSchemaMapRef.get
      documentsOpt       <- Sync[F].delay(documentsSchemaMap.get(schema))
    } yield {
      for {
        documents <- documentsOpt
        document  <- documents.all.asInstanceOf[Map[K, T]].get(key)
      } yield document.asInstanceOf[T]
    }
  }

  def searchByField[T, K](using schema: DocumentSchema[T, K])(field: String, value: String): F[Either[DatabaseError, List[T]]] = {
    for {
      documentsSchemaMap <- documentsSchemaMapRef.get
      documentsOpt       <- Sync[F].delay(documentsSchemaMap.get(schema))
    } yield {
      for {
        documents <- Either.fromOption(documentsOpt, s"No data present for schema ${schema.name}")
        primaryKeys <-
          if field == schema.primary.name then
            // search using primary indexes
            schema.primary.inputDecoder.decode(value).map(Set(_))
          else
            // search using non primary indexes
            for {
              indexData     <- Either.fromOption(documents.indexData.get(field), s"Schema ${schema.name} does not have field $field")
              indexToSearch <- indexData.tryParseToIndexPrimitive(value)
            } yield indexData.indexToPrimary.get(indexToSearch).getOrElse(Set.empty).asInstanceOf[Set[K]]
      } yield {
        primaryKeys.toList
          .map(key => documents.all.asInstanceOf[Map[K, T]].get(key))
          .flattenOption
      }
    }
  }
}
