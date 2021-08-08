package inmemdb.domain

import io.circe.Decoder
import java.time.OffsetDateTime
import java.util.UUID

opaque type UserId   = Long
opaque type TicketId = UUID

given (using decoder: Decoder[Long]): Decoder[UserId]   = decoder
given (using decoder: Decoder[UUID]): Decoder[TicketId] = decoder

object UserId { def fromLong(l: Long): UserId = l }
extension (x: UserId) def value: Long = x

object TicketId { def fromUUID(l: UUID): TicketId = l }
extension (x: TicketId) def value: UUID = x

case class User(
    id: UserId,
    name: String,
    createdAt: OffsetDateTime,
    verified: Option[Boolean]
)

case class Ticket(
    id: TicketId,
    createdAt: OffsetDateTime,
    ticketType: Option[String],
    subject: String,
    assigneeId: Option[UserId],
    tags: List[String]
)
