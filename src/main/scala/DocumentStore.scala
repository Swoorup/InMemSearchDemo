package store

import Domain.{*, given}
import cats.Applicative
import cats.implicits.*
import cats.effect.{Async, Ref, Sync}
import scala.reflect.ClassTag

trait DocumentSchema[T: ClassTag, K]:
  case class Field(name: String, value: FieldValue)

  // case FieldPrimitiveValue

  enum FieldValue:
    case Bool(select: T => Boolean, shouldIndex: Boolean = false)
    case Num(select: T => Long, shouldIndex: Boolean = false)
    case Str(select: T => String, shouldIndex: Boolean = false)
    case Custom[K](select: T => K, shouldIndex: Boolean = false)
    case Opt(select: T => Option[FieldValue])
    case Arr(select: T => List[FieldValue])
    case Obj(fields: List[Field])

  type PrimaryKeyType = K
  type DocumentType = T

  def primary: Field
  def fields: List[Field]

given DocumentSchema[User, UserId] with
  // type PrimaryIndex = UserId

  def primary = Field("_id", FieldValue.Num(_.id.value))
  def fields = List(
    Field("name", FieldValue.Str(_.name, true)), 
    Field("created_at", FieldValue.Str(_.createdAt.toString, true)),
    Field("verified", FieldValue.Bool(_.verified, true))
  )

trait FieldIndexer[K]: 
  def search(value: String): List[K]

case class Document[T, K](
  all: Map[K, T],
  indexes: Map[String, FieldIndexer[K]]
) 
object Document:
  def apply[T, K](using S: DocumentSchema[K, T])(objects: List[T]): Document[T, K] = 
    val all = S.primary.va
    Document(

    )

trait DocumentStore[F[_]]:
  def bulkInsert[T, K](using DocumentSchema[K, T])(objects: List[T]): F[Unit]
  def searchByField[T, K](using DocumentSchema[K, T])(field: String, value: String): F[T]

object DocumentStore:
  def apply[F[_]: Async]: F[DocumentStore[F]] = 
    for {
      documentsRef <- Ref.of[F, Map[DocumentSchema[?, ?], Document[?, ?]]](Map.empty)
      store <- Sync[F].delay(new DocumentStoreImpl[F](documentsRef))
    } yield store

private class DocumentStoreImpl[F[_]: Applicative](
  documentsRef: Ref[F, Map[DocumentSchema[?, ?], Document[?, ?]]]
) extends DocumentStore[F] {

  def bulkInsert[T, K](using S: DocumentSchema[K, T])(objects: List[T]): F[Unit] =
    for {
      documents <- documentsRef.modify { docs => (docs.+(S -> ???), docs)}
    } yield ???

  def searchByField[T, K](using DocumentSchema[K, T])(field: String, value: String): F[T] = 
    ???
}