package repo

import java.nio.charset.CodingErrorAction
import java.time.LocalDate

import akka.actor.ActorSystem
import controllers.Assets.Asset
import javax.inject.{Inject, Singleton}
import play.api.libs.concurrent.CustomExecutionContext

import scala.concurrent.ExecutionContext
import scala.io.Codec
import scala.reflect.io.File

/// Convert `String` to List[DataPoint]
object DataFromString {

  // Get inner content of `,` separated values = `"value"`
  private val r = "\"([^\"]*)\"".r
  private def intoParts(string: String) = r
    .findAllMatchIn(string)
    .toList
    .map { _.group(1) }

  def test(): Unit = {
    println(intoParts("\"Hello\", \"World\", \"123,45\""))
  }

  // LocalDate from String
  private val dateMonths = List(
    "янв", "фев",
    "мар", "апр", "май",
    "июн", "июл", "авг",
    "сен", "окт", "ноя",
    "дек"
  )
  private def getLocalDate(s: String): LocalDate = {
    s match {
      case s"$a.$b.$c" =>
        LocalDate.of(c.toInt + 2000, dateMonths.indexOf(b)+1, a.toInt)
    }
  }

  def apply(): List[DataPoint] = {
    implicit val codec: Codec = Codec("UTF-8")
    codec.onMalformedInput(CodingErrorAction.REPLACE)
    codec.onUnmappableCharacter(CodingErrorAction.REPLACE)
    DataFromString(File("assets/source.csv").slurp())
  }
  def apply(content: String): List[DataPoint] = {
    var id = 0
    // Lines
    content.split("\n")
    // Skip first one
      .drop(1)
    // Split into preprocessed parts
      .map(intoParts)
    // Process parts of each line
      .map { parts =>
        id += 1
        val first = getLocalDate(parts.head)
        val last = getLocalDate(parts(1))
        val value = parts(2).replace(",", ".").toFloat
        DataPoint(DataId(id), first, last, value)
      }
      .toList
  }
}

/// ListRepository initialized from CSV File
@Singleton
class CSVRepository @Inject ()(implicit ec: RepositoryContext)
  extends ListRepository(DataFromString())

