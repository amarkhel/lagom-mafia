package utils.silhouette

import com.amarkhel.user.api.User
import com.mohiva.play.silhouette.api.Env
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator

trait MyEnv extends Env {
  type I = User
  type A = CookieAuthenticator
}