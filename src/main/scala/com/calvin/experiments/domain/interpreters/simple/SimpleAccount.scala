package com.calvin.experiments.domain.interpreters.simple

import SimpleAccount._

case class SimpleAccount(balance: MoneyBD = 0, balanceHeld: MoneyBD = 0)

object SimpleAccount {
  type MoneyBD = BigDecimal
  type ErrorOr[A] = Either[String, A]

  def error[A](message: String): ErrorOr[A] = Left(message)

  def success[A](a: A): ErrorOr[A] = Right(a)
}
