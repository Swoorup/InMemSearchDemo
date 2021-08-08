package inmemdb.app

import cats.effect.std.Console
import cats.*
import cats.implicits.*
import inmemdb.db.Database

import inmemdb.domain.*
import inmemdb.domain.JsonCodec.given
import inmemdb.domain.DocumentSchema.given
import inmemdb.db.DocumentSchema

private object ConsoleAppUtil:
  extension (str: String) {
    def padField: String = str.padTo(18, ' ')
    def highlight: String = {
      import scala.Console.*
      s"${RESET}${BOLD}${UNDERLINED}${str}${RESET}"
    }
  }

  extension (list: List[String]) def displayString: String = list.mkString("[", ", ", "]")

  extension [F[_]: Console](console: Console[F])
    def printlnInfo(msg: String): F[Unit] = {
      import scala.Console.*
      Console[F].println(s"${RESET}${BLUE}${msg}${RESET}")
    }

    def printlnErr(msg: String): F[Unit] = {
      import scala.Console.*
      Console[F].println(s"${RESET}${RED}${msg}${RESET}")
    }

    def printlnSuccess(msg: String): F[Unit] = {
      import scala.Console.*
      Console[F].println(s"${RESET}${GREEN}${msg}${RESET}")
    }

  given userShow: Show[User] with
    def show(t: User): String = {
      import scala.Console.*
      s"${"_id".padField}${t.id}\n" ++
        s"${"name".padField}${t.name}\n" ++
        s"${"created_at".padField}${t.createdAt}\n" ++
        s"${"verified".padField}${t.verified.getOrElse("")}"
    }

  given ticketShow: Show[Ticket] with
    def show(t: Ticket): String = {
      import scala.Console.*
      s"${"_id".padField}${t.id}\n" ++
        s"${"type".padField}${t.ticketType.getOrElse("")}\n" ++
        s"${"subject".padField}${t.subject}\n" ++
        s"${"assignee_id".padField}${t.assigneeId.getOrElse("")}\n" ++
        s"${"tags".padField}${t.tags.displayString}"
    }

class ConsoleApp[F[_]: Console: Monad](db: Database[F]) {
  import ConsoleAppUtil.{*, given}

  val userSchema    = summon[DocumentSchema[User, UserId]]
  val ticketSchema  = summon[DocumentSchema[Ticket, TicketId]]
  val displayBanner = """
      Welcome to Zendesk Search
      Type 'quit' to exit at any time, Press 'Enter' to continue
  """

  def displayUser(user: User): F[Unit] = {
    for {
      _              <- Console[F].printlnSuccess(userShow.show(user))
      ticketsForUser <- db.searchByField[Ticket, TicketId]("assignee_id", user.id.toString)
      _ <- ticketsForUser match
        case Left(error) => Console[F].printlnErr(s"Error occurred while getting tickets: $error")
        case Right(tickets) =>
          val displayTickets = tickets.map(_.subject).displayString
          Console[F].printlnSuccess(s"${"tickets".padField}$displayTickets")
      _ <- Console[F].println("----------------------------------------------------------------")
    } yield ()
  }

  def displayTicket(ticket: Ticket): F[Unit] = {
    for {
      _    <- Console[F].printlnSuccess(ticketShow.show(ticket))
      user <- ticket.assigneeId.traverse(userId => db.lookUp[User, UserId](userId)).map(_.flatten)
      _    <- user.map(_.name).traverse { name => Console[F].printlnSuccess(s"${"assignee_name".padField}$name") }
      _    <- Console[F].println("----------------------------------------------------------------")
    } yield ()
  }

  def searchHandlingErrors[T, K](using DocumentSchema[T, K])(entity: String)(handleResults: List[T] => F[Unit]): F[Unit] = {
    for {
      searchTerm   <- Console[F].print("Enter search term: ") *> Console[F].readLine
      searchValue  <- Console[F].print("Enter search value: ") *> Console[F].readLine
      _            <- Console[F].println("")
      _            <- Console[F].println(s"Searching $entity for ${searchTerm.highlight} with a value of ${searchValue.highlight}.")
      _            <- Console[F].println("")
      searchResult <- db.searchByField[T, K](searchTerm, searchValue)
      _ <- searchResult match
        case Left(error) => Console[F].printlnErr(s"Error occurred: ${error.msg}")
        case Right(results) =>
          if results.isEmpty then Console[F].println("No results found")
          else handleResults(results)

    } yield ()
  }

  def searchZendesk: F[Unit] = {
    for {
      input <- Console[F].printlnInfo("Select 1) Users or 2) Tickets") *> Console[F].readLine
      _ <- input match
        case "1" => searchHandlingErrors[User, UserId]("users")(_.traverse_(displayUser))
        case "2" => searchHandlingErrors[Ticket, TicketId]("tickets")(_.traverse_(displayTicket))
        case _   => Console[F].printlnErr("Invalid search selection. Please try again.") *> searchZendesk
    } yield ()
  }

  def viewSearchOptions: F[Unit] = {
    def displayFields(schema: DocumentSchema[?, ?]) =
      schema.allFields
        .map(_.name)
        .traverse_(Console[F].println)

    val searchOptionsText = "Select 1) Users or 2) Tickets"
    for {
      _ <- Console[F].printlnInfo("----------------------------------------------------------------")
      _ <- Console[F].printlnInfo("Search Users with")
      _ <- Console[F].printlnInfo("----------------------------------------------------------------")
      _ <- Console[F].println("")
      _ <- displayFields(userSchema)
      _ <- Console[F].printlnInfo("----------------------------------------------------------------")
      _ <- Console[F].printlnInfo("Search Tickets with")
      _ <- Console[F].printlnInfo("----------------------------------------------------------------")
      _ <- displayFields(ticketSchema)
    } yield ()
  }

  def prompt: F[Unit] = {
    val promptOptionsText = """

          Select search options:
            * Press 1 to search Zendesk
            * Press 2 to view a list of searchable fields
            * Type 'quit' to exit

      """

    for {
      input <- Console[F].printlnInfo(promptOptionsText) *> Console[F].readLine
      _ <- input match
        case "1"    => searchZendesk *> prompt
        case "2"    => viewSearchOptions *> prompt
        case "quit" => Monad[F].unit
        case _      => Console[F].printlnErr("Invalid selection. Try again.") *> prompt
    } yield ()
  }

  def run: F[Unit] = Console[F].printlnInfo(displayBanner) *> prompt
}
