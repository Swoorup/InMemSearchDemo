package inmemdb.domain

object DocumentSchema:
  import java.util.UUID
  import inmemdb.store.*
  import inmemdb.store.Implicits.given

  given InputDecoder[UserId] with
    def decode(payload: String): Either[String, UserId] = 
      summon[InputDecoder[Long]]
        .decode(payload)
        .map(UserId.fromLong)
        .left.map(_ => "Failed to parse user id.")

  given IndexEncoder[UserId] with
    def encode(value: UserId) = PrimitiveValue.Num(value.value)

  given InputDecoder[TicketId] with
    def decode(payload: String): Either[String, TicketId] = 
      summon[InputDecoder[UUID]].decode(payload).map(TicketId.fromUUID)
        .left.map(_ => "Failed to parse ticket id.")

  given IndexEncoder[TicketId] with
    def encode(value: TicketId) = PrimitiveValue.Str(value.value.toString)

  given DocumentSchema[User, UserId] with
    def name = "User"
    def primary = Field("_id", _.id)
    def fields = List(
      Field("name", _.name),
      Field("created_at", _.createdAt),
      Field("verified", _.verified),
    )

  given DocumentSchema[Ticket, TicketId] with
    def name = "Ticket"
    def primary = Field("_id", _.id)
    def fields = List(
      Field("created_at", _.createdAt),
      Field("type", _.ticketType),
      Field("subject", _.subject),
      Field("assignee_id", _.assigneeId),
      Field("tags", _.tags),
    )