package net.ijbrown.hello.impl

import java.io.File

import akka.cluster.Cluster
import akka.persistence.cassandra.testkit.CassandraLauncher
import com.lightbend.lagom.scaladsl.persistence.cassandra.testkit.TestUtil
import com.lightbend.lagom.scaladsl.server.{LagomApplicationContext, LocalServiceLocator}
import org.scalatest.{AsyncWordSpec, BeforeAndAfterAll, Matchers}
import play.api.Configuration
import play.core.server.NettyServer
import net.ijbrown.hello.api._

import scala.concurrent.Promise

class HelloServiceSpec extends AsyncWordSpec with Matchers with BeforeAndAfterAll {

  CassandraLauncher.start(new File("target/HelloServiceSpec"), CassandraLauncher.DefaultTestConfigResource,
    clean = true, port = 0)

  private val port = Promise[Int]()
  private val app = new HelloApplication(LagomApplicationContext.Test) with LocalServiceLocator {
    override def lagomServicePort = port.future
    override def additionalConfiguration: Configuration = Configuration(TestUtil.persistenceConfig(
      "HelloServiceSpec",
      CassandraLauncher.randomPort
    ))
  }
  val server = NettyServer.fromApplication(app.application)
  port.success(server.httpPort.get)
  val client = app.serviceClient.implement[HelloService]

  // Start the cluster
  val cluster = Cluster(app.actorSystem)
  cluster.join(cluster.selfAddress)

  override protected def afterAll(): Unit = {
    server.stop()
    CassandraLauncher.stop()
  }

  "Hello service" should {

    "say hello" in {
      client.hello("Alice").invoke().map { answer =>
        answer should ===("Hello, Alice!")
      }
    }

    "allow responding with a custom message" in {
      for {
        _ <- client.useGreeting("Bob").invoke(GreetingMessage("Hi"))
        answer <- client.hello("Bob").invoke()
      } yield {
        answer should ===("Hi, Bob!")
      }
    }
  }
}
