package net.ijbrown.hellostream.impl

import com.lightbend.lagom.scaladsl.api.ServiceLocator.NoServiceLocator
import com.lightbend.lagom.scaladsl.server._
import play.api.libs.ws.ahc.AhcWSComponents
import net.ijbrown.hellostream.api.HelloStreamService
import net.ijbrown.hello.api.HelloService
import com.softwaremill.macwire._

class HelloStreamLoader extends LagomApplicationLoader {

  override def load(context: LagomApplicationContext): LagomApplication =
    new HelloStreamApplication(context) {
      override def serviceLocator = NoServiceLocator
    }

  override def loadDevMode(context: LagomApplicationContext): LagomApplication =
    new HelloStreamApplication(context) with LagomDevModeComponents
}

abstract class HelloStreamApplication(context: LagomApplicationContext)
  extends LagomApplication(context)
    with AhcWSComponents {

  // Bind the services that this server provides
  override lazy val lagomServer = LagomServer.forServices(
    bindService[HelloStreamService].to(wire[HelloStreamServiceImpl])
  )

  // Bind the HelloService client
  lazy val helloService = serviceClient.implement[HelloService]
}
