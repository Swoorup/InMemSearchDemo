package inmemdb.db

import cats.Applicative
import cats.implicits.*
import cats.effect.{Async, Ref, Sync}
import java.time.OffsetDateTime

/** 
 * Possible search errors described in an ADT
 */
enum SearchError(val msg: String):
  case DataSchemaNotPresent(schema: String) extends SearchError(s"No data present for schema '$schema'")
  case FieldNotFound(schema: String, field: String) extends SearchError(s"Schema '$schema' does not contain field '$field'")
  case InputParseError(parseError: String) extends SearchError(parseError)

trait Database[F[_]]:
  def bulkInsert[T, K](using DocumentSchema[T, K])(objects: List[T]): F[Unit]
  def lookUp[T, K](using DocumentSchema[T, K])(key: K): F[Option[T]]
  def searchByField[T, K](using DocumentSchema[T, K])(field: String, value: String): F[Either[SearchError, List[T]]]

object Database:
  def apply[F[_]: Async]: F[Database[F]] = {
    for {
      documentsRef <- Ref.of[F, Map[DocumentSchema[?, ?], Documents[?, ?]]](Map.empty)
      db           <- Sync[F].delay(new DatabaseImpl[F](documentsRef))
    } yield db
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

  def searchByField[T, K](using schema: DocumentSchema[T, K])(field: String, value: String): F[Either[SearchError, List[T]]] = {
    for {
      documentsSchemaMap <- documentsSchemaMapRef.get
      documentsOpt       <- Sync[F].delay(documentsSchemaMap.get(schema))
    } yield {
      for {
        documents <- Either.fromOption(documentsOpt, SearchError.DataSchemaNotPresent(schema.name))
        primaryKeys <-
          if field == schema.primary.name then
            // search using primary indexes
            schema.primary.decodeInput(value).map(Set(_)).leftMap(SearchError.InputParseError(_))
          else
            // search using non primary indexes
            for {
              indexData     <- Either.fromOption(documents.indexData.get(field), SearchError.FieldNotFound(schema.name, field))
              indexToSearch <- indexData.field.getIndexPrimitive(value).leftMap(SearchError.InputParseError(_))
            } yield indexData.indexToPrimary.get(indexToSearch).getOrElse(Set.empty).asInstanceOf[Set[K]]
      } yield {
        primaryKeys.toList
          .map(key => documents.all.asInstanceOf[Map[K, T]].get(key))
          .flattenOption
      }
    }
  }
}
