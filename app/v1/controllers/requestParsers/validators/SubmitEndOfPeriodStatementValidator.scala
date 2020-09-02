/*
 * Copyright 2020 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package v1.controllers.requestParsers.validators

import config.AppConfig
import javax.inject.Inject
import play.api.Logger
import play.api.libs.json.{Format, JsValue, Json}
import v1.controllers.requestParsers.validators.validations._
import v1.models.des.TypeOfBusiness
import v1.models.errors.{BusinessIdFormatError, EndDateFormatError, FinalisedFormatError, MtdError, RangeEndDateBeforeStartDateError, RuleIncorrectOrEmptyBodyError, RuleNotFinalisedError, StartDateFormatError, TaxYearFormatError, TypeOfBusinessFormatError}
import v1.models.requestData.{SubmitEndOfPeriodBody, SubmitEndOfPeriodStatementRawData}

class SubmitEndOfPeriodStatementValidator @Inject()(appConfig: AppConfig) extends Validator[SubmitEndOfPeriodStatementRawData] {

  val logger: Logger = Logger(this.getClass)

  private val validationSet = List(parameterFormatValidation, bodyFormatValidator)

  private def parameterFormatValidation: SubmitEndOfPeriodStatementRawData => List[List[MtdError]] = { data =>
    (List(NinoValidation.validate(data.nino)))
  }

  ////    val taxYearValidation = TaxYearValidation.validate(data.taxYear)
  ////
  ////    val minTaxYearValidation = if (taxYearValidation.contains(TaxYearFormatError)) {
  ////      Seq()
  ////    } else {
  ////      Seq(MinTaxYearValidation.validate(data.taxYear, appConfig.minTaxYearPensionCharge.toInt))
  ////    }
  //
  //    (List(
  //      NinoValidation.validate(data.nino)//,
  ////      taxYearValidation
  //    ))//++ minTaxYearValidation).distinct

  def jsonValidation(json: JsValue): List[MtdError] = {

    val typeOfBusiness: Option[String] = (json \ "typeOfBusiness").asOpt[String]
    val finalised: Option[String] = (json \ "finalised").asOpt[String]

    (typeOfBusiness, finalised) match {
      case (Some(typeOfBusiness), Some(finalised)) => typeOfBusinessFormat(typeOfBusiness) ++ finalisedCorrectFormat(finalised)
      case _ => List()
    }
  }

  lazy val log = s"[JsonFormatValidation][validate] - Request body failed validation with errors -"

  def typeOfBusinessFormat(typeOfBusiness: String): List[MtdError] = {
    val validTypeOfBusiness: Boolean = try {
      Json.parse(s""""$typeOfBusiness"""").asOpt[TypeOfBusiness].isDefined
    } catch {
      case _: Exception =>
        logger.warn(s"$log typeOfBusiness is invalid. typeOfBusiness: $typeOfBusiness")
        false
    }

    //400 FORMAT_TYPE_OF_BUSINESS The provided Type of business is invalid
    if(validTypeOfBusiness) NoValidationErrors else List(TypeOfBusinessFormatError)
  }

  def finalisedCorrectFormat(finalised: String): List[MtdError] = {
    val validBoolean: Option[Boolean] = try {
      Some(finalised.toBoolean)
    } catch {
      case _: Exception =>
        logger.warn(s"$log finalised was not of type boolean. finalised: $finalised")
        None
    }
    //400 FORMAT_FINALISED The provided Finalised value is invalid
    validBoolean.fold(List(FinalisedFormatError))(_ => NoValidationErrors)
  }

  def validateBusinessId(businessId: String): List[MtdError] ={
    //400 FORMAT_BUSINESS_ID The provided Business ID is invalid
    if(businessId.matches("")) NoValidationErrors else List(BusinessIdFormatError)
  }

  def validateFinalised(finalised: Boolean): List[MtdError] ={
    //400 RULE_NOT_FINALISED Finalised must be set to "true"
    if(finalised) NoValidationErrors else List(RuleNotFinalisedError)
  }

  def validateStartDate(startDate: String): List[MtdError] ={
    //400 FORMAT_START_DATE The provided Start date is invalid
    if(startDate.matches("")) NoValidationErrors else List(StartDateFormatError)
  }
  def validateEndDate(endDate: String): List[MtdError] ={
    //400 FORMAT_END_DATE The provided From date is invalid
    if(endDate.matches("")) NoValidationErrors else List(EndDateFormatError)
  }

  def validateDates(startDate: String, endDate: String): List[MtdError] ={
    //400 RANGE_END_DATE_BEFORE_START_DATE The End date must be after the Start date
    List(RangeEndDateBeforeStartDateError)
  }

  private def bodyFormatValidator: SubmitEndOfPeriodStatementRawData => List[List[MtdError]] = { data =>

    val validationErrors: List[MtdError] = JsonFormatValidation.validate[SubmitEndOfPeriodBody](data.body.json, Some(jsonValidation))

    lazy val jsonAsModel: Option[SubmitEndOfPeriodBody] = data.body.json.asOpt[SubmitEndOfPeriodBody]

    val errors = List(
      if (validationErrors.nonEmpty) {
        validationErrors
      } else if (jsonAsModel.isDefined) {

        val model = jsonAsModel.get
        validateBusinessId(model.businessId) ++
          validateFinalised(model.finalised) ++
          validateStartDate(model.accountingPeriod.startDate) ++
          validateEndDate(model.accountingPeriod.endDate) ++
          validateDates(model.accountingPeriod.startDate,model.accountingPeriod.endDate)

      } else {
        List(RuleIncorrectOrEmptyBodyError)
      }
    )

    List(Validator.flattenErrors(errors))
  }

  override def validate(data: SubmitEndOfPeriodStatementRawData): List[MtdError] = run(validationSet, data)
}
