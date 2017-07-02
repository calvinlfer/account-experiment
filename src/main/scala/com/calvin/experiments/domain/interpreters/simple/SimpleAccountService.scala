package com.calvin.experiments.domain.interpreters.simple

import cats.Monad
import SimpleAccount._
import com.calvin.experiments.domain.AccountService

trait SimpleAccountService extends AccountService[MoneyBD, SimpleAccount, ErrorOr] {
  override implicit val effectMonad: Monad[ErrorOr] = cats.instances.either.catsStdInstancesForEither

  override def credit(amount: MoneyBD): SimpleAccount => SimpleAccount = account =>
    account.copy(balance = account.balance + amount)

  override def debit(amount: MoneyBD): SimpleAccount => ErrorOr[SimpleAccount] = account =>
    if (account.balance < amount) error(s"Not enough money in account")
    else success {
      account.copy(balance = account.balance - amount)
    }

  override def preAuth(amount: MoneyBD): SimpleAccount => ErrorOr[SimpleAccount] = account =>
    if (account.balance < amount) error(s"Not enough money in account")
    else success {
      account.copy(
        balance = account.balance - amount,
        balanceHeld = account.balanceHeld + amount
      )
    }

  override def cancel(amount: MoneyBD): SimpleAccount => ErrorOr[SimpleAccount] = account =>
    if (account.balanceHeld < amount) error("Trying to remove too much from held balance")
    else success {
      account.copy(
        balance = account.balance + amount,
        balanceHeld = account.balanceHeld - amount
      )
    }
}

object SimpleAccountService extends SimpleAccountService
