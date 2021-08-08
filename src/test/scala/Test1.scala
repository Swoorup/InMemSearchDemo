import cats.effect.{IO, SyncIO}
import munit.CatsEffectSuite
class ExampleSuite extends CatsEffectSuite {
  test("tests can return IO[Unit] with assertions expressed via a map") {
    IO(42).map(it => assertEquals(it, 42))
  }
  test("alternatively, asertions can be written via assertIO") {
    assertIO(IO(42), 42)
  }
  test("or via assertEquals syntax") {
    IO(42).assertEquals(42)
  }
  test("or via plain assert syntax on IO[Boolean]") {
    IO(true).assert
  }
  test("SyncIO works too") {
    SyncIO(42).assertEquals(42)
  }
  import cats.effect.std.Dispatcher
  val dispatcher = ResourceFixture(Dispatcher[IO])
  dispatcher.test("resources can be lifted to munit fixtures") { dsp =>
    dsp.unsafeRunAndForget(IO(42))
  }
}
