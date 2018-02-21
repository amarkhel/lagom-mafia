import akka.util.ByteString
import play.api.i18n.Messages
import views.html.helper.FieldConstructor
import play.api.http.{ContentTypeOf, ContentTypes, Writeable}
import play.api.libs.json.JsError
import play.api.mvc.{Codec, Request}

import scala.concurrent.ExecutionContext.Implicits.global

package object controllers {

  def isAjax[A](implicit request : Request[A]) = {
    request.headers.get("X-Requested-With") == Some("XMLHttpRequest")
  }

  implicit def contentTypeOf_Throwable(implicit codec: Codec): ContentTypeOf[Throwable] =
    ContentTypeOf[Throwable](Some(ContentTypes.TEXT))

  implicit def writeableOf_Throwable(implicit codec: Codec): Writeable[Throwable] = {
    Writeable(e => ByteString(e.getMessage.getBytes("utf-8")))
  }

  implicit def contentTypeOf_JsError(implicit codec: Codec): ContentTypeOf[JsError] =
    ContentTypeOf[JsError](Some(ContentTypes.JSON))

  implicit def writeableOf_JsError(implicit codec: Codec): Writeable[JsError] = {
    Writeable(e => ByteString(JsError.toFlatForm(e).toString.getBytes("utf-8")))
  }
}
