package arrow.intro

import arrow.optics.*
import arrow.data.*
import arrow.*
import arrow.core.ForOption
import arrow.core.Option
import arrow.core.fix
import arrow.typeclasses.*
import arrow.instances.*

@optics
data class Db(val content: MapK<Int, String>) {
  companion object
}

@optics
data class Street(val number: Int, val name: String) {
  companion object
}

@optics
data class Address(val city: String, val street: Street) {
  companion object
}

@optics
data class Company(val name: String, val address: Address) {
  companion object
}

@optics
data class Employee(val name: String, val company: Company) {
  companion object
}

@optics
sealed class NetworkResult {
  companion object
}

@optics
data class Success(val content: String) : NetworkResult() {
  companion object
}

@optics
sealed class NetworkError : NetworkResult() {
  companion object
}

@optics
data class HttpError(val message: String) : NetworkError() {
  companion object
}

object TimeoutError : NetworkError()

interface Service1<F> : Functor<F> {
  fun Kind<F, Int>.addOne(): Kind<F, Int> =
    map { it + 1 }
}

interface Service2<F> : Functor<F> {
  fun Kind<F, Int>.addTwo(): Kind<F, Int> =
    map { it + 2 }
}

interface App<F> : Service1<F>, Service2<F> {
  fun Kind<F, Int>.addThree(): Kind<F, Int> =
    addOne().addTwo()
}

fun <F, A> Functor<F>.app(f: App<F>.() -> A): A =
  f(object : App<F> {
    override fun <A, B> Kind<F, A>.map(f: (A) -> B): Kind<F, B> = this@app.run { map(f) }
  })

object test {
  @JvmStatic
  fun main(args: Array<String>) {
    println(ForOption extensions {
      app {
        Option(1).addThree().fix()
      }
    })
  }
}