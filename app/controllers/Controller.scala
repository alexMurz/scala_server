package controllers

import javax.inject.Inject
import play.api.{Logger, MarkerContext}
import play.api.data.Form
import play.api.http.{FileMimeTypes, HttpVerbs}
import play.api.i18n.{Langs, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc.{Action, ActionBuilder, AnyContent, BaseController, BodyParser, ControllerComponents, DefaultActionBuilder, MessagesRequestHeader, PlayBodyParsers, PreferredMessagesProvider, Request, RequestHeader, Result, WrappedRequest}

import scala.concurrent.{ExecutionContext, Future}

trait MainRequestHeader extends MessagesRequestHeader with PreferredMessagesProvider

class MainRequest[A](request: Request[A], val messagesApi: MessagesApi)
  extends WrappedRequest(request) with MainRequestHeader

/**
 * This is the place to put logging, metrics, to augment
 * the request with contextual data, and manipulate the
 * result.
 */
class MainActionBuilder @Inject()(messagesApi: MessagesApi, playBodyParsers: PlayBodyParsers)
                                 (implicit val executionContext: ExecutionContext)
  extends ActionBuilder[MainRequest, AnyContent]
    with RequestMarkerContext
    with HttpVerbs {

  override val parser: BodyParser[AnyContent] = playBodyParsers.anyContent

  type RequestBlock[A] = MainRequest[A] => Future[Result]

  private val logger = Logger(this.getClass)

  override def invokeBlock[A](request: Request[A], block: RequestBlock[A]): Future[Result] = {
    // Convert to marker context and use request in block
    implicit val markerContext: MarkerContext = requestHeaderToMarkerContext(request)
    logger.trace(s"invokeBlock: ")

    val future = block(new MainRequest(request, messagesApi))

    future.map { result =>
      request.method match {
        case GET | HEAD =>
          result.withHeaders("Cache-Control" -> s"max-age: 100")
        case other =>
          result
      }
    }
  }
}

/**
 * Packaged resources for MainController
 */
case class MainControllerComponents @Inject()
(mainActionBuilder: MainActionBuilder,
 dataResourceHandler: DataResourceHandler,
 actionBuilder: DefaultActionBuilder,
 parsers: PlayBodyParsers,
 messagesApi: MessagesApi,
 langs: Langs,
 fileMimeTypes: FileMimeTypes,
 executionContext: ExecutionContext
) extends ControllerComponents


class BaseMainController @Inject()(cc: MainControllerComponents)
  extends BaseController
    with RequestMarkerContext {
  override protected def controllerComponents: ControllerComponents = cc

  def MainAction: MainActionBuilder = cc.mainActionBuilder

  def resources: DataResourceHandler = cc.dataResourceHandler
}

class MainController @Inject()(cc: MainControllerComponents)(implicit ec: ExecutionContext)
  extends BaseMainController(cc) {

  private val logger = Logger(getClass)

  def index: Action[AnyContent] = MainAction.async { implicit request =>
    logger.trace("index: ")
    resources.all.map { it => Ok(Json.toJson(it)) }
  }

  def lookup(raw_id: String): Action[AnyContent] = MainAction.async { implicit request =>
    logger.trace(s"lookup $raw_id")
    resources.lookup(raw_id).map { it => Ok(Json.toJson(it)) }
  }

  def averageInRange(): Action[AnyContent] = MainAction.async { implicit request =>
    val lo = request.getQueryString("lo").get
    val hi = request.getQueryString("hi").get
    logger.trace(s"find average in range $lo - $hi")
    resources.averageInRange(lo, hi).map { it => Ok(Json.toJson(it)) }
  }

  def minMaxInRange(): Action[AnyContent] = MainAction.async { implicit request =>
    val lo = request.getQueryString("lo").get
    val hi = request.getQueryString("hi").get
    logger.trace(s"find min/max in range $lo - $hi")
    resources.minMaxInRange(lo, hi).map { it => Ok(Json.toJson(it)) }
  }
}

