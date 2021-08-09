package inmemdb.db

import cats.effect.{IO, SyncIO, Resource}
import munit.CatsEffectSuite
import inmemdb.db.*
import inmemdb.db.Implicits.given

class DatabaseTestSuite extends CatsEffectSuite {
  case class Book(title: String)
  case class Person(id: Long, name: String, booksRead: List[Book] = List.empty, verified: Option[Boolean] = None)

  val janeDoe    = Person(1, "Jane Doe")
  val swoorup    = Person(2, "Swoorup", List(Book("Fountain Head")), Some(true))
  val david      = Person(3, "David", List(Book("Fountain Head"), Book("Adam Smith")), Some(false))
  val testPerson = List(janeDoe, swoorup, david)

  given validSchema: DocumentSchema[Person, Long] with {
    def name    = "Person"
    def primary = IndexField("id", _.id)
    def nonPrimary = List(
      IndexField("name", _.name),
      IndexField("books_read.title", _.booksRead.map(_.title)),
      IndexField("verified", _.verified)
    )
  }

  val sampleDb = for {
    db <- Database[IO]
    _  <- db.bulkInsert[Person, Long](testPerson)
  } yield db

  test("BulkInsert returns SchemaError upon inserting documents which has invalid schema") {
    /// name field occurs twice
    val testInvalidPersonSchema = new DocumentSchema[Person, Long] {
      def name    = "Person"
      def primary = IndexField("id", _.id)
      def nonPrimary = List(
        IndexField("name", _.name),
        IndexField("name", _.booksRead.map(_.title)),
        IndexField("verified", _.verified)
      )
    }

    assertIO(
      {
        for {
          db     <- Database[IO]
          result <- db.bulkInsert[Person, Long](using testInvalidPersonSchema)(testPerson)
        } yield result
      },
      Left(DatabaseError.SchemaError("Person contains duplicate fields"))
    )
  }

  test("BulkImport import documents successfully with valid schema") {
    assertIO(
      {
        for {
          db     <- Database[IO]
          result <- db.bulkInsert[Person, Long](testPerson)
        } yield result
      },
      Right(())
    )
  }

  test("SearchByPrimaryKey returns none if no key is found") {
    sampleDb
      .flatMap(_.searchByPrimaryKey[Person, Long](99))
      .assertEquals(None)
  }

  test("SearchByPrimaryKey returns some if key is found") {
    sampleDb
      .flatMap(_.searchByPrimaryKey[Person, Long](1))
      .assertEquals(Some(Person(1, "Jane Doe")))
  }

  test("SearchByField returns FieldNotFound error if field specifed does not exist") {
    sampleDb
      .flatMap(_.searchByField[Person, Long]("age", "35"))
      .assertEquals(Left(DatabaseError.FieldNotFound("Person", "age")))
  }

  test("SearchByField returns InputParseError if search value is not of valid field type") {
    sampleDb
      .flatMap(_.searchByField[Person, Long]("id", "Mary"))
      .assertEquals(Left(DatabaseError.InputParseError("Failed to parse number.")))
  }

  test("SearchByField returns empty list if search value is not found.") {
    sampleDb
      .flatMap(_.searchByField[Person, Long]("name", "Mary"))
      .assertEquals(Right(List()))
  }

  test("SearchByField returns results if search value is found.") {
    sampleDb
      .flatMap(_.searchByField[Person, Long]("books_read.title", "Fountain Head"))
      .assertEquals(Right(List(swoorup, david)))
  }
}
