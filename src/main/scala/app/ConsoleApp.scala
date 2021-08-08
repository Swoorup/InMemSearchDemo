package inmemdb.app

import cats.effect.std.Console
import cats.*
import cats.implicits.*
import inmemdb.store.DocumentStore

import inmemdb.domain.*
import inmemdb.domain.JsonCodec.given
import inmemdb.domain.DocumentSchema.given
import inmemdb.store.DocumentSchema

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
      _ <- userSchema.allFields
        .traverse_(field => printlnSuccess(s"${field.name.padTo(30, ' ')}${field.select(user)}"))
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
        case Right(success) => printlnSuccess(result.toString)
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
