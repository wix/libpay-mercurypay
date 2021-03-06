package com.wix.pay.mercurypay.model

case class AuthorizeOrSaleResponse(ResponseOrigin: Option[String] = None,
                                   DSIXReturnCode: Option[String] = None,
                                   CmdStatus: Option[String] = None,
                                   TextResponse: Option[String] = None,
                                   UserTraceData: Option[String] = None,
                                   MerchantID: Option[String] = None,
                                   AcctNo: Option[String] = None,
                                   ExpDate: Option[String] = None,
                                   CardType: Option[String] = None,
                                   TranCode: Option[String] = None,
                                   AuthCode: Option[String] = None,
                                   RefNo: Option[String] = None,
                                   InvoiceNo: Option[String] = None,
                                   AVSResult: Option[String] = None,
                                   CVVResult: Option[String] = None,
                                   OperatorID: Option[String] = None,
                                   Memo: Option[String] = None,
                                   Purchase: Option[String] = None,
                                   Authorize: Option[String] = None,
                                   AcqRefData: Option[String] = None,
                                   RecordNo: Option[String] = None,
                                   ProcessData: Option[String] = None) extends Response