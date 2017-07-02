object Nigel {

  trait AccountEvent

  object AccountEvent {

    // AccountDebited minuses the account balance
    case class AccountDebited(v: Int) extends AccountEvent

    // AccountCredited adds to the account balance
    case class AccountCredited(v: Int) extends AccountEvent

    // AmountHeld will remove from the account balance and add to the hold balance
    case class AmountHeld(v: Int) extends AccountEvent

    // HoldReleased will remove from the hold balance and add to the account balance
    case class HoldReleased(v: Int) extends AccountEvent

  }

  trait AccountCommand

  object AccountCommand {

    import AccountEvent._

    case class PreAuth(v: Int) extends AccountCommand

    def preAuth(v: Int)(lst: List[AccountEvent]): List[AccountEvent] =
      lst :+ AmountHeld(v)

    case class Cancel(v: Int) extends AccountCommand

    def cancel(v: Int)(lst: List[AccountEvent]): List[AccountEvent] =
      lst :+ HoldReleased(v)

    case class Capture(v: Int) extends AccountCommand

    def capture(v: Int)(lst: List[AccountEvent]): List[AccountEvent] =
      lst ++ List(HoldReleased(v), AccountDebited(v))

    case class Void(v: Int) extends AccountCommand

    def void(v: Int)(lst: List[AccountEvent]): List[AccountEvent] =
      lst :+ HoldReleased(v)

    case class Credit(v: Int) extends AccountCommand

    def credit(v: Int)(lst: List[AccountEvent]): List[AccountEvent] =
      lst :+ AccountCredited(v)

    case class Purchase(v: Int) extends AccountCommand

    def purchase(v: Int)(lst: List[AccountEvent]): List[AccountEvent] =
      lst :+ AccountDebited(v)
  }

}
