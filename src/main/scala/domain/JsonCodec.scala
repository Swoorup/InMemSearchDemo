package inmemdb.domain

object JsonCodec:
  import io.circe.{Json, Decoder}
  import java.time.OffsetDateTime

  given Decoder[User] = cursor => {
    for {
      userId    <- cursor.downField("_id").as[UserId]
      name      <- cursor.downField("name").as[String]
      createdAt <- cursor.downField("created_at").as[OffsetDateTime]
      verified  <- cursor.downField("verified").as[Option[Boolean]]
    } yield User(userId, name, createdAt, verified)
  }

  given Decoder[Ticket] = cursor => {
    for {
      ticketId   <- cursor.downField("_id").as[TicketId]
      createdAt  <- cursor.downField("created_at").as[OffsetDateTime]
      ticketType <- cursor.downField("type").as[Option[String]]
      subject    <- cursor.downField("subject").as[String]
      assigneeId <- cursor.downField("assignee_id").as[Option[UserId]]
      tags       <- cursor.downField("tags").as[List[String]]
    } yield Ticket(ticketId, createdAt, ticketType, subject, assigneeId, tags)
  }
