package com.calvin.experiments.actor

import akka.actor.{ ActorLogging, Props }
import akka.persistence.PersistentActor
import com.calvin.experiments.actor.Account._
import com.calvin.experiments.domain.interpreters.simple._
import SimpleAccount._
import SimpleAccountService._
import akka.event.LoggingReceive

object Account {
  sealed trait Command
  case class PreAuth(amount: MoneyBD) extends Command
  case class Capture(amount: MoneyBD) extends Command
  case class Void(amount: MoneyBD) extends Command
  case class Cancel(amount: MoneyBD) extends Command
  case class Credit(amount: MoneyBD) extends Command
  case class Debit(amount: MoneyBD) extends Command
  case class Purchase(amount: MoneyBD) extends Command
  case object BalancesQuery extends Command

  sealed trait Event
  case class AccountCredited(amount: MoneyBD) extends Event
  case class AccountDebited(amount: MoneyBD) extends Event
  case class AccountPreAuthorized(amount: MoneyBD) extends Event
  case class AccountPreAuthCancelled(amount: MoneyBD) extends Event
  case class BalanceCaptured(amount: MoneyBD) extends Event

  case class ValidationError(message: String)
  case class BalanceResponse(balance: MoneyBD, heldBalance: MoneyBD)

  def props: Props = Props[Account]
}

class Account extends PersistentActor with ActorLogging {
  var currentAcctState: SimpleAccount = SimpleAccount()

  // use events to (re)construct the in-memory model
  def updateState(event: Event): Unit = event match {
    case AccountCredited(amount) => currentAcctState = credit(amount)(currentAcctState)

    // TODO: The problem with incorporating validation with the domain model is that it becomes annoying to handle
    // reconstruction of the current state (we know that if the events ended up in the journal, they must be valid)
    case AccountDebited(amount) => currentAcctState = debit(amount)(currentAcctState).right.get
    case AccountPreAuthorized(amount) => currentAcctState = preAuth(amount)(currentAcctState).right.get
    case AccountPreAuthCancelled(amount) => currentAcctState = cancel(amount)(currentAcctState).right.get
    case BalanceCaptured(amount) => currentAcctState = capture(amount)(currentAcctState).right.get
  }

  override def persistenceId: String = s"calvin-account"

  // Commands go here
  override def receiveCommand: Receive = LoggingReceive {
    case PreAuth(amount) =>
      // TODO: decouple validation from domain model so you would validate here and then
      // persist would use the domain service to update the in-memory model
      // See how the Credit command is handled
      preAuth(amount)(currentAcctState).fold(
        error => sender() ! ValidationError(error),
        updatedAccount =>
          persist(AccountPreAuthorized(amount)) { event =>
            currentAcctState = updatedAccount
        }
      )

    case Capture(amount) =>
      capture(amount)(currentAcctState).fold(
        error => sender() ! ValidationError(error),
        updatedAccount =>
          persist(BalanceCaptured(amount)) { event =>
            currentAcctState = updatedAccount
        }
      )

    // Void and Cancel have the same underlying logic
    // Void is used to cancel a pre-authorization if an order has not been shipped
    case Void(amount) =>
      void(amount)(currentAcctState).fold(
        error => sender() ! ValidationError(error),
        updatedAccount =>
          persist(AccountPreAuthCancelled(amount)) { event =>
            currentAcctState = updatedAccount
        }
      )

    // Cancel is used to cancel a pre-authorization whilst the order is in the process of shipping
    case Cancel(amount) =>
      void(amount)(currentAcctState).fold(
        error => sender() ! ValidationError(error),
        updatedAccount =>
          persist(AccountPreAuthCancelled(amount)) { event =>
            currentAcctState = updatedAccount
        }
      )

    case Credit(amount) =>
      persist(AccountCredited(amount)) { event =>
        updateState(event)
      }

    case Debit(amount) =>
      debit(amount)(currentAcctState).fold(
        error => sender() ! ValidationError(error),
        updatedAccount =>
          persist(AccountDebited(amount)) { event =>
            currentAcctState = updatedAccount
        }
      )

    case Purchase(amount) =>
      purchase(amount)(currentAcctState).fold(
        error => sender() ! ValidationError(error),
        updatedAccount =>
          persist(AccountDebited(amount)) { event =>
            currentAcctState = updatedAccount
        }
      )

    case BalancesQuery =>
      sender() ! BalanceResponse(currentAcctState.balance, currentAcctState.balanceHeld)
  }

  override def receiveRecover: Receive = {
    case e: Event => updateState(e)
  }
}
