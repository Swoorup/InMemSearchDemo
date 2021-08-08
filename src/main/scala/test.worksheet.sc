import inmemdb.domain.*
import inmemdb.domain.DocumentSchema.given
import inmemdb.domain.JsonCodec.given
import inmemdb.store.*

import cats.effect.*
import cats.*
import cats.implicits.*
import cats.effect.unsafe.IORuntime

import io.circe.jawn.*
import java.time.*

val json = decode[Vector[User]]("""[ 
  {
    "_id": 73,
    "name": "Moran Daniels",
    "created_at": "2016-07-06T03:42:35-10:00",
    "verified": false
  },
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
""")

val user = User(
  UserId.fromLong(1231231L),
  "asdcasdcsad",
  OffsetDateTime.now,
  None
)

given IORuntime = IORuntime.global
val app = for {
  store   <- DocumentStore[IO]
  _       <- store.bulkInsert[User, UserId](List(user))
  result  <- store.searchByField[User, UserId]("_id", "131231")
} yield { result }

app.unsafeRunSync()