package inmemdb

import io.circe.Decoder
import java.time.OffsetDateTime
import java.util.UUID
import store.DocumentSchema

opaque type UserId = Long
opaque type TicketId = UUID

given Decoder[UserId] = Decoder[Long]
given Decoder[TicketId] = Decoder[UUID]

object UserId { def fromLong(l: Long): UserId = l }
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

import store.Implicits.given
given DocumentSchema[User, UserId] with

  def name = "User"

  def primary = 
    Field("_id", _.id)

  def fields = List(
    Field("name", _.name),
    Field("created_at", _.createdAt),
    Field("verified", _.verified),
    Field("tags.verified", _.verified),
  )