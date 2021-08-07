package Domain

import io.circe.Decoder
import java.time.OffsetDateTime
import java.util.UUID

opaque type UserId = Long
opaque type TicketId = UUID

given Decoder[UserId] = Decoder[Long]
given Decoder[TicketId] = Decoder[UUID]

extension (x: UserId) def value: Long = x
extension (x: TicketId) def value: UUID = x

case class User(
  id: UserId,
  name: String,
  createdAt: OffsetDateTime,
  verified: Boolean
)

enum Incident:
  case Incident
  case Problem
  case Question
  case Task

case class Ticket(
  id: TicketId,
  createdAt: OffsetDateTime,
  ticketType: Option[Incident],
  assigneeId: Option[UserId],
  tags: List[String]
)