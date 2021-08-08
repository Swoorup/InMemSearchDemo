package inmemdb.app

import cats.effect.{IO, IOApp, ExitCode}
import cats.implicits.*
import com.monovore.decline.*
import com.monovore.decline.effect.*
import inmemdb.store.DocumentStore
import java.io.File

import inmemdb.domain.*
import inmemdb.domain.JsonCodec.given
import inmemdb.domain.DocumentSchema.given

case class AppConfig(usersJsonPath: String, ticketsJsonPath: String)

val appConfigOpts: Opts[AppConfig] =
  val userJsonFileOpts   = Opts.option[String]("users", "The path to the user json file.", short = "u")
  val ticketJsonFileOpts = Opts.option[String]("tickets", "The path to the ticket json file.", short = "t")
  (userJsonFileOpts, ticketJsonFileOpts).mapN(AppConfig.apply)

def decodeJsonFile[T](path: String)(using io.circe.Decoder[T]) =
  import io.circe.jawn.*
  IO.fromEither(decodeFile[T](new File(path)))

enum AppCommand:
  case SearchZendesk
  case EnumerateSearchableFields
  case Quit

object InMemDbDemoApp
    extends CommandIOApp(
      name = "InMemDbDemo",
      header = "InMemDbDemo command line"
    ) {

  override def main: Opts[IO[ExitCode]] =
    appConfigOpts.map { config =>
      for {
        users   <- decodeJsonFile[List[User]](config.usersJsonPath)
        tickets <- decodeJsonFile[List[Ticket]](config.ticketsJsonPath)

        // initialize the in memory document store
        store <- DocumentStore[IO]

        // add initial data
        _ <- store.bulkInsert[User, UserId](users)
        _ <- store.bulkInsert[Ticket, TicketId](tickets)

        _ <- new ConsoleApp(store).run
      } yield ExitCode.Success
    }
}
