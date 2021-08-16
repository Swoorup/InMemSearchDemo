package inmemdb.db

import cats.effect.{IO, SyncIO, Resource}
import munit.CatsEffectSuite
import io.circe.jawn.*
import java.time.OffsetDateTime
import java.util.UUID

import inmemdb.domain.*
import inmemdb.domain.DocumentSchema.given
import inmemdb.domain.JsonCodec.given

class DatabaseIntegrationTestSuite extends CatsEffectSuite {

  val usersJson = """[ 
    {
      "_id": 74,
      "name": "Melissa Bishop",
      "created_at": "2016-02-17T10:35:02-11:00",
      "verified": false
    },
    {
      "_id": 75,
      "name": "Catalina Simpson",
      "created_at": "2016-06-07T09:18:00-10:00",
      "verified": true
    }
  ]
  """

  val ticketsJson = """[
    {
      "_id": "a0d5a779-dc8d-4191-9245-971ed57a8072",
      "created_at": "2016-04-20T09:40:14-10:00",
      "type": "incident",
      "subject": "A Catastrophe in Italy",
      "assignee_id": 74,
      "tags": [
        "Michigan",
        "Florida",
        "Idaho",
        "Georgia",
        "Tennessee"
      ]
    },
    {
      "_id": "25d9edca-7756-4d28-8fdd-f16f1532f6ab",
      "created_at": "2016-03-01T05:58:09-11:00",
      "type": "task",
      "subject": "A Problem in Cyprus",
      "assignee_id": 75,
      "tags": [
        "Puerto Rico",
        "Idaho",
        "Oklahoma",
        "Louisiana"
      ]
    }
  ]
  """

  val dbFixture = ResourceSuiteLocalFixture("db-fixture", Resource.make(Database[IO])(_ => IO.unit))

  override def munitFixtures = List(dbFixture)

  test("DBIntegrationTest: Users documents can be inserted") {
    assertIO(
      {
        for
          users  <- IO.fromEither(decode[List[User]](usersJson))
          result <- dbFixture().bulkInsert[User, UserId](users)
        yield result
      },
      Right(())
    )
  }

  test("DBIntegrationTest: Tickets documents can be inserted") {
    assertIO(
      {
        for
          tickets <- IO.fromEither(decode[List[Ticket]](ticketsJson))
          result  <- dbFixture().bulkInsert[Ticket, TicketId](tickets)
        yield result
      },
      Right(())
    )
  }

  test("DBIntegrationTest: Should be able to lookup users using primary key") {
    val userId = UserId.fromLong(74)
    dbFixture()
      .searchByPrimaryKey[User, UserId](userId)
      .map(_.map(_.id))
      .assertEquals(Some(userId))
  }

  test("DBIntegrationTest: Should be able to lookup tickets using primary key") {
    val ticketId = TicketId.fromUUID(UUID.fromString("25d9edca-7756-4d28-8fdd-f16f1532f6ab"))
    dbFixture()
      .searchByPrimaryKey[Ticket, TicketId](ticketId)
      .map(_.map(_.id))
      .assertEquals(Some(ticketId))
  }

  test("DBIntegrationTest: Should be able to search users using a field and a term") {
    val createdAt = OffsetDateTime.parse("2016-02-17T10:35:02-11:00")
    dbFixture()
      .searchByField[User, UserId]("created_at", createdAt.toString)
      .map(_.map(_.map(_.createdAt)))
      .assertEquals(Right(List(createdAt)))
  }

  test("DBIntegrationTest: Should be able to search tickets using tag field.") {
    val idaho = "Idaho"
    val expectedTickets = List("a0d5a779-dc8d-4191-9245-971ed57a8072", "25d9edca-7756-4d28-8fdd-f16f1532f6ab")
      .map(UUID.fromString)
      .map(TicketId.fromUUID)

    dbFixture()
      .searchByField[Ticket, TicketId]("tags", idaho)
      .map(_.map(_.map(_.id)))
      .assertEquals(Right(expectedTickets))
  }

  test("DBIntegrationTest: Should be able to search tickets using partial search string field.") {
    val expectedTickets = List("a0d5a779-dc8d-4191-9245-971ed57a8072")
      .map(UUID.fromString)
      .map(TicketId.fromUUID)

    dbFixture()
      .searchByField[Ticket, TicketId]("subject", "Italy")
      .map(_.map(_.map(_.id)))
      .assertEquals(Right(expectedTickets))
  }

}
