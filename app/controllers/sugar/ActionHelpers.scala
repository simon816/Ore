package controllers.sugar

import com.google.common.base.Preconditions.{checkArgument, checkNotNull}

import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.Results.Redirect
import play.api.mvc.{Call, Result}

/**
  * A helper class for some common functions of controllers.
  */
trait ActionHelpers {

  /**
    * Redirects to the specified call with the first error of the specified
    * Form.
    *
    * @param call Call to redirect to
    * @param form Form with error
    * @return     Redirect to call
    */
  def FormError(call: Call, form: Form[_]) = {
    checkNotNull(call, "null call", "")
    checkNotNull(form, "null form", "")
    checkArgument(form.errors.nonEmpty, "no errors", "")
    val firstError = form.errors.head
    Redirect(call).flashing("error" -> (firstError.message + '.' + firstError.key))
  }

  implicit final class SpongeResult(result: Result) {

    /**
      * Adds an alert message to the result.
      *
      * @param tpe    Alert type
      * @param alert  Alert message
      * @return       Result with error
      */
    def withAlert(tpe: String, alert: String): Result = result.flashing(tpe -> alert)

    /**
      * Adds one or more alerts messages to the result.
      *
      * @param tpe    Alert type
      * @param alerts  Alert messages
      * @return        Result with alerts
      */
    //TODO: Use NEL[String] if we get the type
    def withAlerts(tpe: String, alerts: Seq[String]): Result = alerts match {
      case Seq()       => result
      case Seq(single) => withAlert(tpe, single)
      case multiple    =>
        val flash = multiple.zipWithIndex
          .map { case (e, i) => s"$tpe-$i" -> e } :+ (s"$tpe-num" -> multiple.size.toString)

        result.flashing(flash: _*)
    }

    /**
      * Adds an error message to the result.
      *
      * @param error  Error message
      * @return       Result with error
      */
    def withError(error: String): Result = withAlert("error", error)

    /**
      * Adds one or more errors messages to the result.
      *
      * @param errors  Error messages
      * @return        Result with errors
      */
    def withErrors(errors: Seq[String]): Result = withAlerts("error", errors)

    /**
      * Adds a success message to the result.
      *
      * @param message  Success message
      * @return         Result with message
      */
    def withSuccess(message: String): Result = withAlert("success", message)

    /**
      * Adds one or more success messages to the result.
      *
      * @param errors  Success messages
      * @return        Result with message
      */
    def withSuccesses(errors: Seq[String]): Result = withAlerts("success", errors)

  }

}
