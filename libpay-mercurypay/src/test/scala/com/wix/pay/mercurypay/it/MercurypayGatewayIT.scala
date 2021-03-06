package com.wix.pay.mercurypay.it


import com.google.api.client.http.javanet.NetHttpTransport
import com.wix.pay.creditcard.{CreditCard, CreditCardOptionalFields, YearMonth}
import com.wix.pay.mercurypay.MercurypayMatchers._
import com.wix.pay.mercurypay._
import com.wix.pay.mercurypay.testkit.MercurypayDriver
import com.wix.pay.model.{CurrencyAmount, Deal, Payment}
import com.wix.pay.{PaymentErrorException, PaymentGateway, PaymentRejectedException}
import org.specs2.mutable.SpecWithJUnit
import org.specs2.specification.Scope


class MercurypayGatewayIT extends SpecWithJUnit {
  val mercurypayPort = 10008

  val driver = new MercurypayDriver(port = mercurypayPort)
  step {
    driver.start()
  }

  sequential

  trait Ctx extends Scope {
    val requestFactory = new NetHttpTransport().createRequestFactory()
    val merchantParser = new JsonMercurypayMerchantParser()
    val authorizationParser = new JsonMercurypayAuthorizationParser()

    val someMerchant = new MercurypayMerchant("someMerchantId", "somePassword")
    val merchantKey = merchantParser.stringify(someMerchant)

    val someCurrencyAmount = CurrencyAmount("USD", 33.3)
    val somePayment = Payment(someCurrencyAmount, 1)
    val someAdditionalFields = CreditCardOptionalFields.withFields(
      csc = Some("123"),
      billingAddress = Some("some billing address"),
      billingPostalCode = Some("some billing code"))
    val someCreditCard = CreditCard(
      number = "4012888818888",
      expiration = YearMonth(2020, 12),
      additionalFields = Some(someAdditionalFields))

    val someDeal = Deal(
      id = "some deal ID",
      title = Some("some deal title"),
      description = Some("some deal description"),
      invoiceId = Some("123")
    )

    val someAuthorization = MercurypayAuthorization(
      invoiceNo = "456",
      acctNo = "some credit card number",
      expDate = "some expiration date",
      authCode = "some auth code",
      acqRefData = "some acqRefData",
      authorize = "some authorize"
    )
    val authorizationKey = authorizationParser.stringify(someAuthorization)
    val someCaptureAmount = 11.1
    val someTransactionId = "some transaction ID"

    val somePosNameAndVersion = "SomePos 1.3.0"

    val mercurypay: PaymentGateway = new MercurypayGateway(
      merchantParser = merchantParser,
      authorizationParser = authorizationParser,
      requestFactory = requestFactory,
      endpointUrl = s"http://localhost:$mercurypayPort/",
      posNameAndVersion = somePosNameAndVersion
    )

    driver.reset()
  }

  "sale request via MercuryPay gateway" should {
    "gracefully fail on invalid deal" in new Ctx {
      mercurypay.sale(
        merchantKey = merchantKey,
        creditCard = someCreditCard,
        payment = somePayment,
        deal = None
      ) must beAFailedTry(
        check = beAnInstanceOf[PaymentErrorException]
      )
    }

    "gracefully fail on rejected card" in new Ctx {
      driver.aSaleRequestFor(
        username = someMerchant.merchantId,
        password = someMerchant.password,
        posNameAndVersion = somePosNameAndVersion,
        invoiceId = someDeal.invoiceId.get,
        creditCard = someCreditCard,
        currencyAmount = someCurrencyAmount
      ) getsRejected()

      mercurypay.sale(
        merchantKey = merchantKey,
        creditCard = someCreditCard,
        payment = somePayment,
        deal = Some(someDeal)
      ) must beAFailedTry(
        check = beAnInstanceOf[PaymentRejectedException]
      )
    }

    "successfully yield a transaction ID on valid request" in new Ctx {
      driver.aSaleRequestFor(
        username = someMerchant.merchantId,
        password = someMerchant.password,
        posNameAndVersion = somePosNameAndVersion,
        invoiceId = someDeal.invoiceId.get,
        creditCard = someCreditCard,
        currencyAmount = someCurrencyAmount
      ) returns someTransactionId

      mercurypay.sale(
        merchantKey = merchantKey,
        creditCard = someCreditCard,
        payment = somePayment,
        deal = Some(someDeal)
      ) must beASuccessfulTry(
        check = ===(someTransactionId)
      )
    }
  }

  "authorize request via MercuryPay gateway" should {
    "gracefully fail on invalid deal" in new Ctx {
      mercurypay.authorize(
        merchantKey = merchantKey,
        creditCard = someCreditCard,
        payment = somePayment,
        deal = None
      ) must beAFailedTry(
        check = beAnInstanceOf[PaymentErrorException]
      )
    }

    "gracefully fail on rejected card" in new Ctx {
      driver.anAuthorizeRequestFor(
        username = someMerchant.merchantId,
        password = someMerchant.password,
        posNameAndVersion = somePosNameAndVersion,
        invoiceId = someDeal.invoiceId.get,
        creditCard = someCreditCard,
        currencyAmount = someCurrencyAmount
      ) getsRejected()

      mercurypay.authorize(
        merchantKey = merchantKey,
        creditCard = someCreditCard,
        payment = somePayment,
        deal = Some(someDeal)
      ) must beAFailedTry(
        check = beAnInstanceOf[PaymentRejectedException]
      )
    }

    "successfully yield an authorization key on valid request" in new Ctx {
      driver.anAuthorizeRequestFor(
        username = someMerchant.merchantId,
        password = someMerchant.password,
        posNameAndVersion = somePosNameAndVersion,
        invoiceId = someDeal.invoiceId.get,
        creditCard = someCreditCard,
        currencyAmount = someCurrencyAmount
      ) returns(
        invoiceNo = someAuthorization.invoiceNo,
        acctNo = someAuthorization.acctNo,
        expDate = someAuthorization.expDate,
        authCode = someAuthorization.authCode,
        acqRefData = someAuthorization.acqRefData,
        authorize = someAuthorization.authorize
      )

      mercurypay.authorize(
        merchantKey = merchantKey,
        creditCard = someCreditCard,
        payment = somePayment,
        deal = Some(someDeal)
      ) must beASuccessfulTry(
        check = beAuthorizationKey(
          authorization = beAuthorization(
            invoiceNo = ===(someAuthorization.invoiceNo),
            acctNo = ===(someAuthorization.acctNo),
            expDate = ===(someAuthorization.expDate),
            authCode = ===(someAuthorization.authCode),
            acqRefData = ===(someAuthorization.acqRefData),
            authorize = ===(someAuthorization.authorize)
          )
        )
      )
    }
  }

  "capture request via MercuryPay gateway" should {
    "successfully yield a transaction ID on valid request" in new Ctx {
      driver.aCaptureRequestFor(
        username = someMerchant.merchantId,
        password = someMerchant.password,
        invoiceNo = someAuthorization.invoiceNo,
        authorize = someAuthorization.authorize,
        acctNo = someAuthorization.acctNo,
        expDate = someAuthorization.expDate,
        authCode = someAuthorization.authCode,
        acqRefData = someAuthorization.acqRefData,
        amount = someCaptureAmount
      ) returns someTransactionId

      mercurypay.capture(
        merchantKey = merchantKey,
        authorizationKey = authorizationKey,
        amount = someCaptureAmount
      ) must beASuccessfulTry(
        check = ===(someTransactionId)
      )
    }
  }

  "voidAuthorization request via MercuryPay gateway" should {
    "successfully yield a transaction ID on valid request" in new Ctx {
      mercurypay.voidAuthorization(
        merchantKey = merchantKey,
        authorizationKey = authorizationKey
      ) must beASuccessfulTry(
        check = ===("") // TODO: change this
      )
    }
  }

  step {
    driver.stop()
  }
}
