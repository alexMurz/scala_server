import com.google.inject.AbstractModule
import javax.inject.Singleton
import net.codingwell.scalaguice.ScalaModule
import play.api.{Configuration, Environment}
import repo.{CSVRepository, DataRepository}

class Module(environment: Environment, configuration: Configuration)
  extends AbstractModule
    with ScalaModule {

  override def configure(): Unit = {
    bind(classOf[DataRepository]).to(classOf[CSVRepository]).in(classOf[Singleton])
  }
}
