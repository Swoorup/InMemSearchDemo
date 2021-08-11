package inmemdb.db

import cats.Applicative
import cats.effect.{Async, Ref, Sync}
import cats.implicits.*
import java.time.OffsetDateTime

/** Possible search errors described in an ADT
  */
enum DatabaseError(val msg: String):
  case SchemaError(schemaErrorMsg: String) extends DatabaseError(schemaErrorMsg)
  case FieldNotFound(schema: String, field: String) extends DatabaseError(s"Schema '$schema' does not contain field '$field'")
  case InputParseError(parseError: String) extends DatabaseError(parseError)

/** Core Database trait which the app interacts to insert/search in memory objects/documents
  */
trait Database[F[_]]:
  def bulkInsert[T, K](using DocumentSchema[T, K])(objects: List[T]): F[Either[DatabaseError, Unit]]
  def searchByPrimaryKey[T, K](using DocumentSchema[T, K])(key: K): F[Option[T]]
  def searchByField[T, K](using DocumentSchema[T, K])(field: String, value: String): F[Either[DatabaseError, List[T]]]

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
  import DatabaseError.*

  private def isValidSchema[T, K](using schema: DocumentSchema[T, K]): Boolean = {
    val allFields = schema.allFields
    allFields.length == allFields.map(_.name).distinct.length
  }

  def bulkInsert[T, K](using schema: DocumentSchema[T, K])(objects: List[T]): F[Either[DatabaseError, Unit]] = {
    for {
      documentsSchemaMap <- documentsSchemaMapRef.get
      documentsOpt       <- Sync[F].delay(documentsSchemaMap.get(schema))
      docsUpdateResult <- Sync[F].delay {
        documentsOpt match
          case None       => Either.cond(isValidSchema, Documents(objects), SchemaError(s"${schema.name} contains duplicate fields"))
          case Some(docs) => Right(docs.asInstanceOf[Documents[T, K]].merge(Documents(objects)))
      }
      insertResult <- docsUpdateResult.traverse { updatedDocs =>
        documentsSchemaMapRef.modify(ds => (ds.+(schema -> updatedDocs), ds)).void
      }
    } yield insertResult
  }

  def searchByPrimaryKey[T, K](using schema: DocumentSchema[T, K])(key: K): F[Option[T]] = {
    for {
      documentsSchemaMap <- documentsSchemaMapRef.get
      documentsOpt       <- Sync[F].delay(documentsSchemaMap.get(schema))
    } yield {
      for {
        documents <- documentsOpt
        document  <- documents.all.asInstanceOf[Map[K, T]].get(key)
      } yield document
    }
  }

  def searchByField[T, K](using schema: DocumentSchema[T, K])(field: String, value: String): F[Either[DatabaseError, List[T]]] = {
    for {
      documentsSchemaMap <- documentsSchemaMapRef.get
      documentsOpt       <- Sync[F].delay(documentsSchemaMap.get(schema))
    } yield {
      documentsOpt.asInstanceOf[Option[Documents[T, K]]] match
        case None => Right(List.empty)
        case Some(documents) => {
          val primaryKeys =
            if field == schema.primary.name then
              // search using primary indexes
              schema.primary.decodeInput(value).map(Set(_)).leftMap(InputParseError(_))
            else
              // search using non primary indexes
              for {
                indexData     <- Either.fromOption(documents.indexData.get(field), FieldNotFound(schema.name, field))
                indexToSearch <- indexData.field.getIndexPrimitive(value).leftMap(InputParseError(_))
              } yield indexData.indexToPrimary.get(indexToSearch).getOrElse(Set.empty)

          primaryKeys.map {
            _.toList
              .map(documents.all.get)
              .flattenOption
          }
        }
    }
  }
}
