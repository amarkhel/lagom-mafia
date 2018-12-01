package config

object Routes {

  object Chat {
    val base = "/chat"
    def connectSSE(username: String) = base + s"/sse/$username"
    def talk = base + "/talk"
  }
}
