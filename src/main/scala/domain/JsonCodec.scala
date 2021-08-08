package inmemdb.domain

object JsonCodec:
  import io.circe.{Json, Decoder}
  import java.time.OffsetDateTime

  given Decoder[User] = c =>
    for {
      userId    <- c.downField("_id").as[UserId]
      name      <- c.downField("name").as[String]
      createdAt <- c.downField("created_at").as[OffsetDateTime]
      verified  <- c.downField("verified").as[Option[Boolean]]
    } yield {
      User(userId, name, createdAt, verified)
    }

  given Decoder[Ticket] = c =>
    for {
      ticketId   <- c.downField("_id").as[TicketId]
      createdAt  <- c.downField("created_at").as[OffsetDateTime]
      ticketType <- c.downField("type").as[Option[String]]
      subject    <- c.downField("subject").as[String]
      assigneeId <- c.downField("assignee_id").as[Option[UserId]]
      tags       <- c.downField("tags").as[List[String]]
    } yield {
      Ticket(ticketId, createdAt, ticketType, subject, assigneeId, tags)
    }
