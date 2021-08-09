import cats.effect.*
import cats.*
import cats.implicits.*
import cats.effect.unsafe.IORuntime
import io.circe.jawn.*
import java.time.OffsetDateTime
import java.util.UUID

import inmemdb.domain.{User, UserId, Ticket, TicketId}
import inmemdb.domain.DocumentSchema.given
import inmemdb.domain.JsonCodec.given
import inmemdb.db.Database

val user = User(
  UserId.fromLong(1231231L),
  "Catalina Simpson",
  OffsetDateTime.MIN,
  None
)

val ticket = Ticket(
  TicketId.fromUUID(UUID.randomUUID()),
  OffsetDateTime.MIN,
  Some("P1 incident"),
  "Unable to turn on computer",
  None,
  List("mac", "pc")
)

given IORuntime = IORuntime.global
val app = for {
  db            <- Database[IO]
  _             <- db.bulkInsert[User, UserId](List(user))
  userResult    <- db.searchByField[User, UserId]("_id", "1231231")
  ticketResult  <- db.searchByField[Ticket, TicketId]("tags", "pc")
} yield (userResult, ticketResult)

app.unsafeRunSync()