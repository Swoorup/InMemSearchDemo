package Domain

import java.util.UUID
import java.time.OffsetDateTime
import io.circe.Codec

opaque type TicketId = UUID
given Codec[TicketId] = Codec[UUID]

enum Incident:
  case Incident
  case Problem
  case Question
  case Task

case class Ticket(
  _id: TicketId,
  created_at: OffsetDateTime,
  `type`: Option[Incident],
  assignee_id: Option[UserId],
  tags: List[String]
)