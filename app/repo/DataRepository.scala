package repo

import java.time.LocalDate

import akka.actor.ActorSystem
import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.libs.concurrent.CustomExecutionContext
import repo.DataPoint.dateInBounds

import scala.concurrent.Future

case class DataId private (id: Int) {
  override def toString: String = id.toString
}
object DataId {
  def apply(i: Int) = new DataId(i)
  def apply(s: String) = new DataId(s.toInt)
}

/// Repository data point
case class DataPoint(id: DataId, first: LocalDate, last: LocalDate, value: Float) {

  def contains(date: LocalDate): Boolean = dateInBounds(date, first, last)

  def containedIn(firstInclusive: LocalDate, lastExclusive: LocalDate, strict: Boolean = false): Boolean = {
    val a = dateInBounds(first, firstInclusive, lastExclusive)
    val b = dateInBounds(last, firstInclusive, lastExclusive)
    if (strict) a && b else a || b
  }

}
object DataPoint {
  private def dateInBounds(v: LocalDate, firstInclusive: LocalDate, lastExclusive: LocalDate): Boolean = {
    val a = v.isEqual(firstInclusive) || v.isAfter(firstInclusive)
    val b = v.isBefore(lastExclusive)
    a && b
  }
}

class RepositoryContext @Inject()(actorSystem: ActorSystem)
  extends CustomExecutionContext(actorSystem, "repository.dispatcher")

/// Pure repository
trait DataRepository {

  /// Get all data points
  def getAll: Future[Iterable[DataPoint]]

  /// Get DataPoint for given date or None
  def dataForDate(date: LocalDate): Future[Option[DataPoint]]

  /// Get iterable over all DataPoints over given date range
  def dateInRange(firstInclusive: LocalDate, lastExclusive: LocalDate, strict: Boolean = false): Future[Iterable[DataPoint]]

}

/// List Repository
//@Singleton
class ListRepository(list: List[DataPoint])
                    (implicit val ec: RepositoryContext)
  extends DataRepository
{
  private val logger = Logger(this.getClass)

  override def getAll: Future[Iterable[DataPoint]] = Future {
    logger.trace("getAll")
    list
  }

  override def dataForDate(date: LocalDate): Future[Option[DataPoint]] = Future {
    logger.trace(s"getForDate $date")
    list find {_.contains(date)}
  }

  override def dateInRange(firstInclusive: LocalDate, lastExclusive: LocalDate, strict: Boolean): Future[Iterable[DataPoint]] = Future {
    logger.trace(s"getInRange $firstInclusive - $lastExclusive (strict = $strict)")
    list filter {_.containedIn(firstInclusive, lastExclusive, strict)}
  }

}
