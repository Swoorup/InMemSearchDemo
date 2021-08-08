import inmemdb.domain.*
import inmemdb.domain.DocumentSchema.given
import inmemdb.domain.JsonCodec.given
import inmemdb.store.*

import cats.effect.*
import cats.*
import cats.implicits.*
import cats.effect.unsafe.IORuntime

import java.time.*

val user = User(
  UserId.fromLong(1231231L),
  "asdcasdcsad",
  OffsetDateTime.now,
  true
)

given IORuntime = IORuntime.global
val app = for {
  store   <- DocumentStore[IO]
  _       <- store.bulkInsert[User, UserId](List(user))
  result  <- store.searchByField[User, UserId]("_id", "131231")
} yield { result }

app.unsafeRunSync()