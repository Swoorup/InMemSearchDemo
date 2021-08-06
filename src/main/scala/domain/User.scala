package Domain

import io.circe.Codec
import java.time.OffsetDateTime

opaque type UserId = Long
given Codec[UserId] = Codec[Long]

case class User(
  _id: UserId,
  name: String,
  created_at: OffsetDateTime,
  verified: Boolean
)
