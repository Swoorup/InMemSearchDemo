package inmemdb

import cats.effect.{IO, IOApp, ExitCode}
import cats.implicits.*
import com.monovore.decline.*
import com.monovore.decline.effect.*
import inmemdb.store.DocumentStore
import java.io.File

import inmemdb.domain.*
import inmemdb.domain.JsonCodec.given
import inmemdb.domain.DocumentSchema.given

object InMemDbDemoApp
    extends CommandIOApp(
      name = "InMemDbDemo",
      header = "InMemDbDemo command line"
    ) {

  case class Config(usersJsonPath: String, ticketsJsonPath: String)

  enum AppCommand:
    case SearchZendesk
    case EnumerateSearchableField
    case Quit

  val configOpts: Opts[Config] =
    val userJsonFileOpts   = Opts.option[String]("users", "The path to the user json file.", short = "u")
    val ticketJsonFileOpts = Opts.option[String]("tickets", "The path to the ticket json file.", short = "t")
    (userJsonFileOpts, ticketJsonFileOpts).mapN(Config.apply)

  def decodeJsonFile[T](path: String)(using io.circe.Decoder[T]) =
    import io.circe.jawn.*
    IO.fromEither(decodeFile[T](new File(path)))

  override def main: Opts[IO[ExitCode]] =
    configOpts.map { config =>
      for {
        store   <- DocumentStore[IO]
        users   <- decodeJsonFile[List[User]](config.usersJsonPath)
        tickets <- decodeJsonFile[List[Ticket]](config.ticketsJsonPath)
        _       <- store.bulkInsert[User, UserId](users)
        _       <- store.bulkInsert[Ticket, TicketId](tickets)
        _       <- IO(println(users))
      } yield ExitCode.Success
    }
}
