/*
package com.amarkhel.mafia.common
import org.scalajs.dom
import org.scalajs.dom.Element
import rx._
import rx.Obs
import rx.Ctx

import scala.util.{Failure, Success}
import scalatags.JsDom.all._


/**
 * A minimal binding between Scala.Rx and Scalatags and Scala-Js-Dom
 */
object Framework {

  /**
   * Wraps reactive strings in spans, so they can be referenced/replaced
   * when the Rx changes.
   */
  implicit def RxStr[T](r: Rx[T])(implicit f: T => Frag, ctx: Ctx.Owner): Frag = {
    rxMod(Rx(span(r())))
  }

  /**
   * Sticks some Rx into a Scalatags fragment, which means hooking up an Obs
   * to propagate changes into the DOM via the element's ID. Monkey-patches
   * the Obs onto the element itself so we have a reference to kill it when
   * the element leaves the DOM (e.g. it gets deleted).
   */
  implicit def rxMod[T <: dom.raw.HTMLElement](r: Rx[HtmlTag]): Frag = {
    def rSafe = r.toTry match {
      case Success(v) => v.render
      case Failure(e) => span(e.toString, backgroundColor := "red").render
    }
    var last = rSafe
    new Var(r){
      val newLast = rSafe
      last.parentElement.replaceChild(newLast, last)
      last = newLast
    }
    bindNode(last)
  }
  implicit def RxAttrValue[T: AttrValue] = new AttrValue[Rx[T]]{
    def apply(t: Element, a: Attr, r: Rx[T]): Unit = {
      Rx(r){ implicitly[AttrValue[T]].apply(t, a, r())}
    }
  }
  implicit def RxStyleValue[T: StyleValue] = new StyleValue[Rx[T]]{
    def apply(t: Element, s: Style, r: Rx[T]): Unit = {
      Rx(r){ implicitly[StyleValue[T]].apply(t, s, r())}
    }
  }

}*/
