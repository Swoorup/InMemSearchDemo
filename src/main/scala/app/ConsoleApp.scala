package inmemdb.app

import cats.effect.std.Console
import cats.*
import cats.implicits.*
import inmemdb.store.DocumentStore

import inmemdb.domain.*
import inmemdb.domain.JsonCodec.given
import inmemdb.domain.DocumentSchema.given
import inmemdb.store.DocumentSchema

extension (str: String) def padField: String = str.padTo(18, ' ')

extension (list: List[String]) def displayString: String = list.mkString("[", ",", "]")

given userShow: Show[User] with
  def show(t: User): String =
    import scala.Console.*
    s"${"_id".padField}${t.id}\n" ++
      s"${"name".padField}${t.name}\n" ++
      s"${"created_at".padField}${t.createdAt}\n" ++
      s"${"verified".padField}${t.verified.getOrElse("")}"

given ticketShow: Show[Ticket] with
  def show(t: Ticket): String =
    import scala.Console.*
    s"${"_id".padField}${t.id}\n" ++
      s"${"type".padField}${t.ticketType.getOrElse("")}\n" ++
      s"${"subject".padField}${t.subject}\n" ++
      s"${"assignee_id".padField}${t.assigneeId.getOrElse("")}\n" ++
      s"${"tags".padField}${t.tags.displayString}"

class ConsoleApp[F[_]: Console: Monad](store: DocumentStore[F]) {
  val userSchema    = summon[DocumentSchema[User, UserId]]
  val ticketSchema  = summon[DocumentSchema[Ticket, TicketId]]
  val displayBanner = """
      Welcome to Zendesk Search
      Type 'quit' to exit at any time, Press 'Enter' to continue
  """

  def printlnInfo(msg: String): F[Unit] =
    import scala.Console.*
    Console[F].println(s"${RESET}${BLUE}${msg}${RESET}")

  def printlnErr(msg: String): F[Unit] =
    import scala.Console.*
    Console[F].println(s"${RESET}${RED}${msg}${RESET}")

  def printlnSuccess(msg: String): F[Unit] =
    import scala.Console.*
    Console[F].println(s"${RESET}${GREEN}${msg}${RESET}")

  def highlight(str: String): String =
    import scala.Console.*
    s"${RESET}${BOLD}${UNDERLINED}${str}${RESET}"

  def searchingText(entity: String, searchTerm: String, searchValue: String): String =
    s"Searching $entity for ${highlight(searchTerm)} with a value of ${highlight(searchValue)}."

  def getSearchTermAndValue(entity: String): F[(String, String)] =
    for {
      searchTerm  <- Console[F].print("Enter search term: ") *> Console[F].readLine
      searchValue <- Console[F].print("Enter search value: ") *> Console[F].readLine
      _           <- Console[F].println("")
      _           <- Console[F].println(searchingText(entity, searchTerm, searchValue))
      _           <- Console[F].println("")
    } yield (searchTerm, searchValue)

  def displayUser(user: User): F[Unit] =
    for {
      _              <- printlnSuccess(userShow.show(user))
      ticketsForUser <- store.searchByField[Ticket, TicketId]("assignee_id", user.id.toString)
      _ <- ticketsForUser match
        case Left(error) => printlnErr(s"Error occurred while getting tickets: $error")
        case Right(tickets) =>
          val displayTickets = tickets.map(_.subject).displayString
          printlnSuccess(s"${"tickets".padField}$displayTickets")
      _ <- Console[F].println("----------------------------------------------------------------")
    } yield ()

  def displayTicket(ticket: Ticket): F[Unit] =
    for {
      _    <- printlnSuccess(ticketShow.show(ticket))
      user <- ticket.assigneeId.traverse(userId => store.lookUp[User, UserId](userId)).map(_.flatten)
      _    <- user.map(_.name).traverse { name => printlnSuccess(s"${"assignee_name".padField}$name") }
      _    <- Console[F].println("----------------------------------------------------------------")
    } yield ()

  def searchUsers: F[Unit] = {
    for {
      searchParam <- getSearchTermAndValue("users")
      (searchTerm, searchValue) = searchParam
      result <- store.searchByField[User, UserId](searchTerm, searchValue)
      _ <- result match
        case Left(error)  => printlnErr(s"Error occurred: $error")
        case Right(users) => users.traverse_(displayUser)

    } yield ()
  }

  def searchTickets: F[Unit] = {
    for {
      searchParam <- getSearchTermAndValue("tickets")
      (searchTerm, searchValue) = searchParam
      result <- store.searchByField[Ticket, TicketId](searchTerm, searchValue)
      _ <- result match
        case Left(error)    => printlnErr(s"Error occurred: $error")
        case Right(tickets) => tickets.traverse_(displayTicket)
    } yield ()
  }

  def searchZendesk: F[Unit] =
    for {
      input <- printlnInfo("Select 1) Users or 2) Tickets") *> Console[F].readLine
      _ <- input match
        case "1" => searchUsers
        case "2" => searchTickets
        case _   => printlnErr("Invalid search selection. Please try again.") *> searchZendesk
    } yield ()

  def viewSearchOptions: F[Unit] = {
    def displayFields(schema: DocumentSchema[?, ?]) =
      schema.allFields
        .map(_.name)
        .traverse_(Console[F].println)

    val searchOptionsText = "Select 1) Users or 2) Tickets"
    for {
      _ <- printlnInfo("----------------------------------------------------------------")
      _ <- printlnInfo("Search Users with")
      _ <- Console[F].println("")
      _ <- displayFields(userSchema)
      _ <- printlnInfo("Search Tickets with")
      _ <- printlnInfo("----------------------------------------------------------------")
      _ <- displayFields(ticketSchema)
      _ <- printlnInfo("----------------------------------------------------------------")
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
      _     <- printlnInfo(promptOptionsText)
      input <- Console[F].readLine
      _ <- input match
        case "1"    => searchZendesk *> prompt
        case "2"    => viewSearchOptions *> prompt
        case "quit" => Monad[F].unit
        case _      => printlnErr("Invalid selection. Try again.") *> prompt
    } yield ()
  }

  def run: F[Unit] = printlnInfo(displayBanner) *> prompt
}
