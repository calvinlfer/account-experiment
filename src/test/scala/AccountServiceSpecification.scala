import com.calvin.experiments.domain.AccountService
import com.calvin.experiments.domain.interpreters.simple.SimpleAccount._
import com.calvin.experiments.domain.interpreters.simple._
import org.scalacheck.{Gen, Prop, Properties}
import Prop.forAll

class AccountServiceSpecification extends Properties("Account Algebra Verification via SimpleAccount")
  with AccountService[MoneyBD, SimpleAccount, ErrorOr]
  with SimpleAccountService {

  // Generators will provide different data for each Property test
  val accountGen: Gen[SimpleAccount] = for {
    valueA <- Gen.chooseNum(100, 1000000)
    valueB <- Gen.chooseNum(100, 100000)
    balance = BigDecimal.valueOf(valueA)
    holdBalance = BigDecimal.valueOf(valueB)
  } yield SimpleAccount(balance, holdBalance)

  property("A credit followed by a debit of the same amount will not change the account balances") =
    forAll(accountGen) { account =>
      val amount = 100
      val s1 = credit(amount)(account)
      val updatedAccount = debit(amount)(s1)
      updatedAccount.isRight &&
        updatedAccount.right.get == account
    }

  property(
    """A pre-authorization followed by a capture of $X will cause a reduction to the account of $X and must not
      |affect an existing holding balances""".stripMargin) =
    forAll(accountGen) { initialAccount =>
      val amount = 10
      val updatedAccount = for {
        s1 <- preAuth(amount)(initialAccount)
        s2 <- capture(amount)(s1)
      } yield s2

      updatedAccount.isRight &&
        updatedAccount.right.get.balance == initialAccount.balance - amount &&
        updatedAccount.right.get.balanceHeld == initialAccount.balanceHeld
    }

  property(
    """A purchase (acts like a debit) of $X will reduce the balance in the account by $X without touching
      |holding balances""".stripMargin) =
    forAll(accountGen) { initialAccount =>
      val amount = 10
      val updatedAccount = purchase(amount)(initialAccount)

      updatedAccount.isRight &&
        updatedAccount.right.get.balance == initialAccount.balance - amount &&
        updatedAccount.right.get.balanceHeld == initialAccount.balanceHeld
    }

  property("A pre-authorization of $X followed by a cancellation of $X will not change the account balances") =
    forAll(accountGen) { initialAccount =>
      val amount = 10
      val updatedAccount = for {
        s1 <- preAuth(amount)(initialAccount)
        s2 <- cancel(amount)(s1)
      } yield s2

      updatedAccount.isRight && updatedAccount.right.get == initialAccount
    }
}
