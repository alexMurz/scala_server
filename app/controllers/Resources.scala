package controllers

import java.time.LocalDate

import javax.inject.{Inject, Provider}
import play.api.{Logger, MarkerContext}
import play.api.libs.json.{Format, Json, Writes}
import repo.{DataFromString, DataId, DataPoint, DataRepository}

import scala.concurrent.{ExecutionContext, Future}

case class ResourceDataPoint(id: String, link: String, first: String, last: String, value: Float)
object ResourceDataPoint {
  implicit val format: Format[ResourceDataPoint] = Json.format
}

case class ResourceFloat(value: Float)
object ResourceFloat {
  implicit val format: Format[ResourceFloat] = Json.format
}

case class ResourceMinMax(min: Float, max: Float)
object ResourceMinMax {
  implicit val format: Format[ResourceMinMax] = Json.format
}

class DataResourceHandler @Inject()(routerProvider: Provider[BaseRouter],
                                    repo: DataRepository)(implicit ec: ExecutionContext) {
  private val logger = Logger(this.getClass)

  def all(implicit mc: MarkerContext): Future[Iterable[ResourceDataPoint]] = repo.getAll.map { it =>
    logger.trace(s"All item count: ${it.size}")
    it.map(toResource)
  }

  def lookup(raw_id: String)(implicit mc: MarkerContext): Future[Option[ResourceDataPoint]] = {
    logger.trace(s"find: $raw_id")
    val id = DataId(raw_id)
    repo.getAll.map {
        _.find { _.id == id }.map(toResource)
    }
  }

  def averageInRange(lo: String, hi: String)(implicit mc: MarkerContext): Future[ResourceFloat] = {
    logger.trace(s"average in range: $lo - $hi")
    val first = LocalDate.parse(lo)
    val last = LocalDate.parse(hi)
    repo.dateInRange(first, last).map { list =>
      val count = list.size
      ResourceFloat(list.map(_.value).sum / count)
    }
  }

  def minMaxInRange(lo: String, hi: String)(implicit mc: MarkerContext): Future[ResourceMinMax] = {
    logger.trace(s"minMax in range $lo - $hi")
    val first = LocalDate.parse(lo)
    val last = LocalDate.parse(hi)
    repo.dateInRange(first, last).map { list =>
      if (list.isEmpty) ResourceMinMax(0.0f, 0.0f)
      else {
        var min = list.head.value
        var max = min
        list.foreach { it =>
          min = it.value.min(min)
          max = it.value.max(max)
        }
        ResourceMinMax(min, max)
      }
    }
  }

  private def toResource(f: Float): ResourceFloat = ResourceFloat(f)
  private def toResource(p: DataPoint): ResourceDataPoint = {
    ResourceDataPoint(p.id.toString, routerProvider.get.link(p.id), p.first.toString, p.last.toString, p.value)
  }

}