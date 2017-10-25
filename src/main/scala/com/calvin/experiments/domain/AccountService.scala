package com.calvin.experiments.domain

import cats.Monad
import cats.syntax.flatMap._
import cats.syntax.functor._

import scala.language.higherKinds

/**
  * Algebra for Accounts that implement 1 and 2 step process for fulfillment
  * 1 step => purchase
  * 2 step => preauth -> capture
  * @tparam Money
  * @tparam Account
  * @tparam Effect
  */
trait AccountService[Money, Account, Effect[_]] {
  // impose effectful composition
  implicit val effectMonad: Monad[Effect]

  def preAuth(amount: Money): Account => Effect[Account]

  def cancel(amount: Money): Account => Effect[Account]

  def credit(amount: Money): Account => Account

  def debit(amount: Money): Account => Effect[Account]

  def purchase(amount: Money): Account => Effect[Account] = debit(amount)

  def capture(amount: Money): Account => Effect[Account] =
    account =>
      for {
        accWithMoney <- cancel(amount)(account) // remove the hold
        accResult <- debit(amount)(accWithMoney) // debit the account
      } yield accResult

  def void(amount: Money): Account => Effect[Account] =
    account =>
      for {
        accWithMoney <- cancel(amount)(account) // remove the hold
        accResult = credit(amount)(accWithMoney) // credit the account
      } yield accResult
}
