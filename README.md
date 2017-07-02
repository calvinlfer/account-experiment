# Account experiments
Modelling accounts using Functional and Reactive Domain Driven Design techniques.

The aim of this approach is two-fold:

- develop an equationally reasonable and functional domain model that can be verified in isolation
- decouple the aforementioned domain model from communication protocols, execution concerns, and state management

This follows the Simple Domain Object Pattern by Dr. Roland Kuhn and the construction of the functional 
domain model from Debasish Ghosh.


### Domain Modelling
The domain algebra (domain model) follows Domain Driven Design and defined in terms of the Ubiquitous Language of the 
domain experts. For accounts, you expect to see, credits, debits, etc. 

We define the operations that take place in the domain in terms of functions and defer giving meaning to the types that
we operate on implying that the functions are polymorphic. For example:

```scala
trait AccountService[Money, Account, Effect[_]] {

  // ... 
  def credit(amount: Money): Account => Account

  def debit(amount: Money): Account => Effect[Account]

  // ...
}
```

We also have the notation of an effect (a Higher Kinded Type) in order to represent a context that can represent 
asynchronous boundaries, failures, etc. to indicate that these operations can cause side-effects in the system.
 
We define this collection of domain functions to be an Algebra. Please note that there are no implementation details in
an algebra. However, given this compositional form, we can write derived combinators that can use existing domain 
functions to procure new behavior without providing an implementation. For example:

```scala
trait AccountService[Money, Account, Effect[_]] {
  // ... 
  def preAuth(amount: Money): Account => Effect[Account]
  
  def cancel(amount: Money): Account => Effect[Account]
  
  def credit(amount: Money): Account => Account

  def debit(amount: Money): Account => Effect[Account]

  // derived combinator
  def capture(amount: Money): Account => Effect[Account] = account =>
    for {
      accWithMoney <- cancel(amount)(account)       // remove the hold
      accResult    <- debit(amount)(accWithMoney)   // debit the account
    } yield accResult

  // ...
}
```

Note that I have removed some boilerplate. In order to use the for-comprehension, you must impose that the 
First-Order-Higher-Kinded-Type Effect has a Monad implementation in order to gain `flatMap` and `map` behavior so you
may compose those effectful domain functions.

Notice that `capture` uses the existing algebra to create new behavior. This means if you implement the core functions, 
you get this additional behavior for free. 

You can develop an interpreter (implementation) for this algebra and verify that the implementation is correct through
property-based testing. For example:

```scala
  property("A credit followed by a debit of the same amount will not change the account balances") =
    forAll(accountGen) { account =>
      val amount = 100
      val s1 = credit(amount)(account)
      val updatedAccount = debit(amount)(s1)
      updatedAccount.isRight &&
        updatedAccount.right.get == account
    }
```

This covers the domain model. Now let's address reactivity by using actors and layering on the communication protocol
and state management. 

### Communication protocols, concurrency and state management

Now that we have our domain model, it is time to deal with state and communication. We use the actor model to adhere to
the reactive manifesto and provide a solution to concurrent access. Notice that functions like `capture` rely on
the `cancel` and `debit` operations to be executed atomically which is one of the key features that the Actor model 
provides. We also need to be able to store the most up to date state of the Account and be able to determine the 
sequence of events that lead up to the current state of the account balances. This is accomplished by Event Sourcing. 

In order to use the domain model, we encapsulate it with an Actor and define Commands and Events in order to interact
with the Actor which will make use of the domain model. For example:

```scala
object Account {
  sealed trait Command
  // ...
  case class Credit(amount: MoneyBD) extends Command
  case class Debit(amount: MoneyBD) extends Command
  // ...
  case object BalancesQuery extends Command

  sealed trait Event
  case class AccountCredited(amount: MoneyBD) extends Event
  case class AccountDebited(amount: MoneyBD) extends Event
  // ...

  case class ValidationError(message: String)
  case class BalanceResponse(balance: MoneyBD, heldBalance: MoneyBD)

  def props: Props = Props[Account]
}

class Account extends PersistentActor with ActorLogging {
  // the most up-to-date in-memory state
  var currentAcctState: SimpleAccount = SimpleAccount()

  // use events to (re)construct the in-memory model
  def updateState(event: Event): Unit = event match {
    case AccountCredited(amount) => currentAcctState = credit(amount)(currentAcctState)
    case AccountDebited(amount) => currentAcctState = debit(amount)(currentAcctState).right.get
  }

  override def persistenceId: String = s"calvin-account"

  // Commands go here
  override def receiveCommand: Receive = LoggingReceive {
    // ... 
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

    case BalancesQuery =>
      sender() ! BalanceResponse(currentAcctState.balance, currentAcctState.balanceHeld)
  }

  // During recovery, Events from the journal will come here
  override def receiveRecover: Receive = {
    case e: Event => updateState(e)
  }
}
```

This shows a basic implementation of layering on the communication protocol and state management. This also demonstrates
how the Actor uses the domain model to change the state of the account. The main idea is that Commands are sent to the 
Actor which are then validated. If the validation succeeds then Events are generated and persisted to the event journal,
once that takes place then we update the in-memory model of the Account. If the Actor dies, it can bring itself up to
date by playing back all the Events from the event journal to restore itself to the most current in-memory model. 

Please note, we have not spoken of concerns like Schema Evolution, Snapshots, Cluster Sharding, etc. which are critical 
to the application running correctly.