import java.time.Instant
import io.circe.Codec

opaque type UserId = Long
given Codec[UserId] = Codec[Long]

case class User(
  id: UserId,
  name: String,
  createdAt: Instant,
  verified: Boolean
)
