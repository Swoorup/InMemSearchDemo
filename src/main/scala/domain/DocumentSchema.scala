package inmemdb.domain

object DocumentSchema:
  import java.util.UUID
  import inmemdb.db.*
  import inmemdb.db.Implicits.given

  given InputDecoder[UserId] with
    def decode(payload: String): Either[String, UserId] = {
      summon[InputDecoder[Long]]
        .decode(payload)
        .map(UserId.fromLong)
        .left
        .map(_ => "Failed to parse user id.")
    }

  given IndexEncoder[UserId] with
    def encode(value: UserId) = IndexPrimitiveValue.Num(value.value)

  given InputDecoder[TicketId] with
    def decode(payload: String): Either[String, TicketId] = {
      summon[InputDecoder[UUID]]
        .decode(payload)
        .map(TicketId.fromUUID)
        .left
        .map(_ => "Failed to parse ticket id.")
    }

  given IndexEncoder[TicketId] with
    def encode(value: TicketId) = IndexPrimitiveValue.Str(value.value.toString)

  given DocumentSchema[User, UserId] with
    def name    = "User"
    def primary = IndexField("_id", _.id)
    def nonPrimary = List(
      IndexField("name", _.name),
      IndexField("created_at", _.createdAt),
      IndexField("verified", _.verified)
    )

  given DocumentSchema[Ticket, TicketId] with
    def name    = "Ticket"
    def primary = IndexField("_id", _.id)
    def nonPrimary = List(
      IndexField("created_at", _.createdAt),
      IndexField("type", _.ticketType),
      IndexField("subject", _.subject),
      IndexField("assignee_id", _.assigneeId),
      IndexField("tags", _.tags)
    )
