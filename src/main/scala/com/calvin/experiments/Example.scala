package com.calvin.experiments

import akka.actor.ActorSystem
import akka.pattern.ask
import com.calvin.experiments.actor.Account
import Account._
import akka.util.Timeout
import scala.concurrent.duration._

object Example extends App {
  val system = ActorSystem("account-test-system")
  val account = system.actorOf(Account.props, "account-actor")
  implicit val ec = system.dispatcher
  implicit val timeout = Timeout(3.seconds)

  account ! Credit(40)
  account ! PreAuth(20)
  account ! Capture(20)

  val balanceResponse = (account ? BalancesQuery).mapTo[BalanceResponse]
  balanceResponse.foreach(response => system.log.info(response.toString))

  balanceResponse.onComplete(_ => system.terminate())
}
