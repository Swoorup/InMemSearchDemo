package inmemdb.domain

object JsonCodec:
  import io.circe.{Json, Decoder}
  import java.time.OffsetDateTime

  given Decoder[User] = c => 
    for {
      userId <- c.downField("_id").as[UserId]
      name <- c.downField("name").as[String]
      createdAt <- c.downField("created_at").as[OffsetDateTime]
      verified <- c.downField("verified").as[Boolean]
    } yield {
      User(userId, name, createdAt, verified)
    }