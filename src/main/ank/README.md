

## Started as...

Learning Exercise to learn FP over Slack

<img src="custom/images/ojetehandler.png" alt="Ojete Handler Logo">

---

## ...then KΛTEGORY was born

Solution for Typed FP in Kotlin

<img src="custom/images/kategory-logo.svg" alt="Kategory logo">

---

## Λrrow = KΛTEGORY + Funktionale

We merged with Funktionale to provide a single path to FP in Kotlin

<img src="custom/images/arrow-brand-transparent.svg" width="256" alt="Λrrow logo">

---

## Type classes

Λrrow contains many FP related type classes

|                |                                                      |
|----------------|------------------------------------------------------|
| Error Handling | `ApplicativeError`,`MonadError`                      |
| Computation    | `Functor`, `Applicative`, `Monad`, `Bimonad`, `Comonad`                    |
| Folding        | `Foldable`, `Traverse`                          |
| Combining      | `Semigroup`, `SemigroupK`, `Monoid`, `MonoidK` |
| Effects        | `MonadDefer`, `Async`, `Effect`           |
| Recursion      | `Recursive`, `BiRecursive`,...                                |
| MTL         | `FunctorFilter`, `MonadState`, `MonadReader`, `MonadWriter`, `MonadFilter`, ...                |

---

## Data types

Λrrow contains many data types to cover general use cases.

|                |                                                      |
|----------------|------------------------------------------------------|
| Error Handling | `Option`,`Try`, `Validated`, `Either`, `Ior`         |
| Collections    | `ListKW`, `SequenceKW`, `MapKW`, `SetKW`             |
| RWS            | `Reader`, `Writer`, `State`                          |
| Transformers   | `ReaderT`, `WriterT`, `OptionT`, `StateT`, `EitherT` |
| Evaluation     | `Eval`, `Trampoline`, `Free`, `FunctionN`            |
| Effects        | `IO`, `Free`, `ObservableKW`                         |
| Optics         | `Lens`, `Prism`, `Iso`,...                           |
| Recursion      | `Fix`, `Mu`, `Nu`,...                                |
| Others         | `Coproduct`, `Coreader`, `Const`, ...                |

---

## A few syntax examples

```kotlin:ank
import arrow.*
import arrow.core.*
import arrow.intro.* // slides stuff

Option(1).map { it + 1 }
```

---

## A few syntax examples

```kotlin:ank
Try<Int> { throw RuntimeException("BOOM!") }.map { it + 1 }
```

---

## A few syntax examples

```kotlin:ank
val x = Right(1)
val y = 1.right()
x == y
```

---

## Applicative Builder

```kotlin:ank
import arrow.instances.*

data class Profile(val id: Long, val name: String, val phone: Int)

fun profile(maybeId: Option<Long>, maybeName: Option<String>, maybePhone: Option<Int>): Option<Profile> =
  ForOption extensions { 
    map(maybeId, maybeName, maybePhone, { (a, b, c) ->
       Profile(a, b, c)
    }).fix()
  }

profile(1L.some(), "William Alvin Howard".some(), 555555555.some())
```

---

## Applicative Builder

```kotlin
@generic
data class Profile(val id: Long, val name: String, val phone: Int) {
  companion object
}

fun profile(maybeId: Option<Long>, maybeName: Option<String>, maybePhone: Option<Int>): Option<Profile> =
  mapToProfile(maybeId, maybeName, maybePhone).fix()
  
profile(1L.some(), "William Alvin Howard".some(), 555555555.some())
// Some(Profile(id=1, name=William Alvin Howard, phone=555555555))
```

---

## Applicative Builder (Same for all data types)

```kotlin
@generic
data class Profile(val id: Long, val name: String, val phone: Int) {
  companion object
}

fun profile(tryId: Try<Long>, tryName: Try<String>, tryPhone: Try<Int>): Try<Profile> =
  mapToProfile(tryId, tryName, tryPhone).fix()
  
profile(Try { 1L }, Try { "William Alvin Howard" }, Try { 555555555 })
// Success(Profile(id=1, name=William Alvin Howard, phone=555555555))
```

---

## Comprehensions - Vanilla

Generalized to all monads. A suspended function provides a non blocking `F<A> -> A`

```kotlin:ank
import arrow.typeclasses.*

fun profile(maybeId: Option<Long>,
            maybeName: Option<String>,
            maybePhone: Option<Int>): Option<Profile> =
  ForOption extensions { 
   binding { // <-- `coroutine starts`
     val id = maybeId.bind() // <-- `suspended`
     val name = maybeName.bind() // <-- `suspended`
     val phone = maybePhone.bind() // <-- `suspended`
     Profile(id, name, phone)
   }.fix() // <-- `coroutine ends`
  }

profile(2L.some(), "Haskell Brooks Curry".some(), 555555555.some())
```

---

## Comprehensions - Exception Aware

Automatically captures exceptions for instances of `MonadError<F, Throwable>`

```kotlin:ank
ForTry extensions { 
  bindingCatch {
      val a = Try { 1 }.bind()
      val b = Try { 1 }.bind()
      throw RuntimeException("BOOM") // <-- `raises errors to MonadError<F, Throwable>`
      val c = Try { 1 }.bind()
      a + b + c
  }
}
```

---

## Comprehensions - Filterable

Imperative filtering control for data types that can provide `empty` values.

```kotlin:ank
import arrow.data.*
import arrow.mtl.typeclasses.*
import arrow.mtl.instances.*

fun <F> MonadFilter<F>.continueIfEven(fa: Kind<F, Int>): Kind<F, Int> = 
  bindingFilter {
    val v = fa.bind()
    continueIf(v % 2 == 0)
    v + 1
  }
```
```kotlin:ank
ForOption extensions { continueIfEven(Option(2)) }
```
```kotlin:ank
ForListK extensions { continueIfEven(listOf(2, 4, 6).k()) }
```

---

## Integrations - Rx2

Let’s take an example and convert it to a comprehension. 

```kotlin
getSongUrlAsync()
  .flatMap { MediaPlayer.load(it) }
  .flatMap {
    val totalTime = musicPlayer.getTotaltime()
	audioTimeline.click()
	  .map { (timelineClick / totalTime * 100).toInt() }
   }
```

---

## Integrations - Rx2

Arrow provides `MonadError<F, Throwable>` for `Observable`

```kotlin
import arrow.effects.*
import arrow.typeclasses.*

ForObservableK extensions { 
 bindingCatch {
   val songUrl = getSongUrlAsync().bind()
   val musicPlayer = MediaPlayer.load(songUrl)
   val totalTime = musicPlayer.getTotaltime() 
   (audioTimeline.click().bind() / totalTime * 100).toInt()
 }
}
```

---

## Integrations - Kotlin Coroutines 

Arrow provides `MonadError<F, Throwable>` for `Deferred`

```kotlin
import arrow.effects.*
import arrow.typeclasses.*

ForDeferredK extensions { 
 bindingCatch {
   val songUrl = getSongUrlAsync().bind()
   val musicPlayer = MediaPlayer.load(songUrl)
   val totalTime = musicPlayer.getTotaltime() 
   (audioTimeline.click().bind() / totalTime * 100).toInt()
 }
}
```

---

## Transforming immutable data

Λrrow includes an `optics` library that make working with immutable data a breeze

```kotlin
data class Street(val number: Int, val name: String)
data class Address(val city: String, val street: Street)
data class Company(val name: String, val address: Address)
data class Employee(val name: String, val company: Company)
```

---

## Transforming immutable data

Λrrow includes an `optics` library that make working with immutable data a breeze

```kotlin:ank
val employee = Employee("John Doe",
                 Company("Λrrow",
                  Address("Functional city",
                    Street(23, "lambda street")))) 
employee
```

---

## Transforming immutable data

while `kotlin` provides a synthetic `copy` dealing with nested data can be tedious

```kotlin:ank
employee.copy(
  company = employee.company.copy(
    address = employee.company.address.copy(
      street = employee.company.address.street.copy(
        name = employee.company.address.street.name.capitalize()
      )
    )
  )
)
```

---

## Optics without boilerplate

You may define composable `Lenses` to work with immutable data transformations

```kotlin
val employeeCompany: Lens<Employee, Company> = Lens(
        get = { it.company },
        set = { company -> { employee -> employee.copy(company = company) } }
)

val companyAddress: Lens<Company, Address> = Lens(
        get = { it.address },
        set = { address -> { company -> company.copy(address = address) } }
)
...
```

---

## Optics without boilerplate

You may define composable `Lenses` to work with immutable data transformations

```kotlin:ank
import arrow.optics.*

val employeeStreetName: Lens<Employee, String> =
  Employee.company compose Company.address compose Address.street compose Street.name

employeeStreetName.modify(employee, String::capitalize)
```

---

## Optics without boilerplate

Or just let Λrrow `@optics` do the dirty work

```diff
+ @optics data class Employee(val name: String, val company: Company)
- val employeeCompany: Lens<Employee, Company> = Lens(
-        get = { it.company },
-        set = { company -> { employee -> employee.copy(company = company) } }
- )
-
- val companyAddress: Lens<Company, Address> = Lens(
-        get = { it.address },
-        set = { address -> { company -> company.copy(address = address) } }
- )
- ...
```

---

## Optics without boilerplate

Optics comes with a succinct and powerful DSL to manipulate deeply nested immutable properties

```kotlin:ank
import arrow.optics.dsl.*

Employee.company.address.street.name.modify(employee, String::toUpperCase)
```

---

## Optics without boilerplate

You can also define `@optics` for your sealed hierarchies

```kotlin
@optics sealed class NetworkResult
@optics data class Success(val content: String): NetworkResult()
@optics sealed class NetworkError : NetworkResult()
@optics data class HttpError(val message: String): NetworkError()
object TimeoutError: NetworkError()
```

---

## Optics without boilerplate

Where you operate over sealed hierarchies manually...

```kotlin:ank
val networkResult: NetworkResult = HttpError("boom!")
val f: (String) -> String = String::toUpperCase

val result = when (networkResult) {
  is HttpError -> networkResult.copy(f(networkResult.message))
  else -> networkResult
}

result
```

---

## Optics without boilerplate

...you cruise now through properties with the new optics DSL

```kotlin:ank
NetworkResult.networkError.httpError.message.modify(networkResult, f)
```

---

## In the works

|        |                                                 |
|--------|-------------------------------------------------|
| arrow-generic | Generic programming with products, coproducts and derivation |
| arrow-streams | A functional `Stream<F, A>` impl that abstract over F and complementes `arrow-effect` |
| arrow-android | FP solutions to common Android issues `Ex: Activity lifecycle` |

---

<!-- .slide: class="table-large" -->

## Λrrow is modular

Pick and choose what you'd like to use.

| Module            | Contents                                                              |
|-------------------|-----------------------------------------------------------------------|
| typeclasses       | `Semigroup`, `Monoid`, `Functor`, `Applicative`, `Monad`...                      |
| core/data              | `Option`, `Try`, `Either`, `Validated`...                                     |
| effects           | `IO`                                                                    |
| effects-rx2       | `ObservableKW`, `FlowableKW`, `MaybeK`, `SingleK`                                                          |
| effects-coroutines       | `DeferredK`                                                       |
| mtl               | `MonadReader`, `MonadState`, `MonadFilter`,...                              |
| free              | `Free`, `FreeApplicative`, `Trampoline`, ...                                |
| recursion-schemes | `Fix`, `Mu`, `Nu`                                                                     |
| optics            | `Prism`, `Iso`, `Lens`, ...                                                 |
| meta              | `@higherkind`, `@deriving`, `@instance`, `@optics` |

---

## Kotlin limitations for Typed FP

---

## Kotlin limitations for Typed FP

Emulated Higher Kinds through [Lightweight higher-kinded Polymorphism](https://www.cl.cam.ac.uk/~jdy22/papers/lightweight-higher-kinded-polymorphism.pdf)

---

## Kotlin limitations for Typed FP

Fear not, `@higherkind`'s got your back!

```diff
+ @higherkind sealed class Option<A> : OptionOf<A>
- class ForOption private constructor() { companion object }
- typealias OptionOf<A> = Kind<ForOption, A>
- inline fun <A, B> OptionOf<A>.fix(): Option<A> = this as Option<A>
```

---

## Λrrow ad-hoc polymorphism

With emulated Higher Kinds and Type classes we can now write polymorphic code

```kotlin:ank
import arrow.Kind
import arrow.core.*
import arrow.effects.*
import arrow.typeclasses.*
```

---

## Λrrow ad-hoc polymorphism

With emulated Higher Kinds and Type classes we can now write polymorphic code

```kotlin:ank
interface Service1<F> : Functor<F> {
  fun Kind<F, Int>.addOne(): Kind<F, Int> =
    map { it + 1 }
}
```

---

## Λrrow ad-hoc polymorphism

With emulated Higher Kinds and Type classes we can now write polymorphic code

```kotlin
interface Service2<F> : Functor<F> {
  fun Kind<F, Int>.addTwo(): Kind<F, Int> =
    map { it + 2 }
}
```

---

## Λrrow ad-hoc polymorphism

With emulated Higher Kinds and Type classes we can now write polymorphic code

```kotlin
interface App<F> : Service1<F>, Service2<F> {
  fun Kind<F, Int>.addThree(): Kind<F, Int> =
    addOne().addTwo()
}
```

---

## Λrrow ad-hoc polymorphism

With emulated Higher Kinds and Type classes we can now write polymorphic code

```kotlin
/* Our app works for all functors */
fun <F, A> Functor<F>.app(f: App<F>.() -> A): A =
  f(object : App<F> {
    override fun <A, B> Kind<F, A>.map(f: (A) -> B): Kind<F, B> = this@app.run { map(f) }
  })
```

---

## Λrrow ad-hoc polymorphism

Program that are abstract and work in many runtimes!

```kotlin:ank
ForOption extensions { 
  app { 
    Option(1).addThree() 
  }
}
```

---

## Λrrow ad-hoc polymorphism

Program that are abstract and work in many runtimes!

```kotlin:ank
ForTry extensions { 
  app { 
    Try { 1 }.addThree() 
  }
}
```

---

## Λrrow ad-hoc polymorphism

Program that are abstract and work in many runtimes!

```kotlin:ank
ForIO extensions { 
  app { 
    IO { 1 }.addThree().fix().unsafeRunSync()
  }
} 
```

---

## Type Classes

This is how you define Type Classes in Λrrow (for now)

```kotlin
interface Functor<F> : Typeclass {
  fun <A, B> map(fa: HK<F, A>, f: (A) -> B): HK<F, B>
}
```

---

## Implementing type class instances is easy...

---

## @deriving

Λrrow can derive instances based on conventions in your data types

```kotlin
@higherkind
@deriving(
        Functor::class,
        Applicative::class,
        Monad::class,
        Foldable::class,
        Traverse::class,
        TraverseFilter::class,
        MonadFilter::class)
sealed class Option<out A> : OptionKind<A> {
   ...
}
```

---

## @instance

Λrrow allows you to hand craft instances

```kotlin
@instance(Either::class)
interface EitherFunctorInstance<L> : Functor<EitherKindPartial<L>> {
    override fun <A, B> map(fa: EitherKind<L, A>, f: (A) -> B): Either<L, B> =
    fa.ev().map(f)
}
//Either.functor<L>() is available after @instance is processed
```

---

## KEEP-87

But we are not stopping here, we want to get rid of some of the codegen.

KEEP-87 is A KEEP to introduce Type Classes in Kotlin!

https://github.com/Kotlin/KEEP/pull/87

---

## KEEP-87

Type Classes & Instances

```kotlin
extension interface Monoid<A> {
  fun A.combine(b: A): A
  val empty: A
}

extension object IntMonoid : Monoid<Int> {
  fun Int.combine(b: Int): Int = this + b
  val empty: Int = 0
}
```

---

## KEEP-87

Declaration site

```kotlin
fun combineOneAndTwo(with Monoid<Int>) =
  1.combine(2) // `this` is an instance of `Monoid<Int>`
```

Desugars to

```kotlin
fun combineOneAndTwo(ev: Monoid<Int>) =
  with(ev) { 1.combine(2) } // `this` is ev
```

---

## KEEP-87

Call site

```kotlin
import IntMonoid
combineOneAndTwo() // instance is resolved via imports and injected by the compiler
```

Desugars to

```kotlin
import IntMonoid
combineOneAndTwo(IntMonoid) // compatible with java and allows explicit overrides
```

---

## An ecosystem of libraries

### __Λnk__ 

Markdown documentation, verification and snippet evaluator for Kotlin

<img src="custom/images/Ank.svg" alt="Λnk">

---

## An ecosystem of libraries

### __Helios__

A fast, purely functional JSON lib for Kotlin

<img src="custom/images/Helios.svg" alt="Helios">

---

## An ecosystem of libraries

### __Kollect__

Efficient data access with id dedup, parallelization, batching and caching.

<img src="custom/images/Kollect.svg" alt="Kollect">

---

## Credits

Λrrow is inspired in great libraries that have proven useful to the FP community:

- [Cats](https://typelevel.org/cats/)
- [Scalaz](https://github.com/scalaz/scalaz)
- [Freestyle](http://frees.io)
- [Monocle](http://julien-truffaut.github.io/Monocle/)
- [Funktionale](https://github.com/MarioAriasC/funKTionale)
- [Paguro](https://github.com/GlenKPeterson/Paguro)

---

<!-- .slide: class="team" -->

## 72 Contributors and counting

- [![](https://github.com/anstaendig.png) **anstaendig**](https://github.com/anstaendig)
- [![](https://github.com/arturogutierrez.png) **arturogutierrez**](https://github.com/arturogutierrez)
- [![](https://github.com/ffgiraldez.png) **ffgiraldez**](https://github.com/ffgiraldez)
- [![](https://github.com/Guardiola31337.png) **Guardiola31337**](https://github.com/Guardiola31337)
- [![](https://github.com/javipacheco.png) **javipacheco**](https://github.com/javipacheco)
- [![](https://github.com/JMPergar.png)  **JMPergar**](https://github.com/JMPergar)
- [![](https://github.com/JorgeCastilloPrz.png) **JorgeCastilloPrz**](https://github.com/JorgeCastilloPrz)
- [![](https://github.com/jrgonzalezg.png) **jrgonzalezg**](https://github.com/jrgonzalezg)
- [![](https://github.com/nomisRev.png) **nomisRev**](https://github.com/nomisRev)
- [![](https://github.com/npatarino.png) **npatarino**](https://github.com/npatarino)
- [![](https://github.com/pablisco.png) **pablisco**](https://github.com/pablisco)
- [![](https://github.com/pakoito.png)  **pakoito**](https://github.com/pakoito)
- [![](https://github.com/pedrovgs.png) **pedrovgs**](https://github.com/pedrovgs)
- [![](https://github.com/pt2121.png)   **pt2121**](https://github.com/pt2121)
- [![](https://github.com/raulraja.png) **raulraja**](https://github.com/raulraja)
- [![](https://github.com/wiyarmir.png) **wiyarmir**](https://github.com/wiyarmir)
- [![](https://github.com/andyscott.png) **andyscott**](https://github.com/andyscott)
- [![](https://github.com/Atternatt.png) **Atternatt**](https://github.com/Atternatt)
- [![](https://github.com/calvellido.png) **calvellido**](https://github.com/calvellido)
- [![](https://github.com/dominv.png) **dominv**](https://github.com/dominv)
- [![](https://github.com/GlenKPeterson.png) **GlenKPeterson**](https://github.com/GlenKPeterson)
- [![](https://github.com/israelperezglez.png) **israelperezglez**](https://github.com/israelperezglez)
- [![](https://github.com/sanogueralorenzo.png) **sanogueralorenzo**](https://github.com/sanogueralorenzo)
- [![](https://github.com/Takhion.png) **Takhion**](https://github.com/Takhion)
- [![](https://github.com/victorg1991.png) **victorg1991**](https://github.com/victorg1991)
- [![](https://github.com/tonilopezmr.png) **tonilopezmr**](https://github.com/tonilopezmr)
- [![](https://github.com/NigelHeylen.png) **NigelHeylen**](https://github.com/NigelHeylen)
- [![](https://github.com/ersin-ertan.png) **ersin-ertan**](https://github.com/ersin-ertan)


---

<!-- .slide: class="join-us" -->

## Join us!

|        |                                                 |
|--------|-------------------------------------------------|
| Github | https://github.com/arrow-kt/arrow                     |
| Slack  | https://kotlinlang.slack.com/messages/C5UPMM0A0 |
| Gitter | https://gitter.im/kategory/Lobby                |

We provide 1:1 mentoring for both users & new contributors!

---

## Thanks!

Thanks to everyone that makes Λrrow possible

- [![](custom/images/47deg-logo.png)](https://www.47deg.com/)
- [![](custom/images/kotlin.png)](https://kotlinlang.org/)
- [![](custom/images/lw-logo.png)](http://www.lambda.world/)
- [![](custom/images/FineCinnamon.png)](https://github.com/FineCinnamon)
- [![](custom/images/jug.png)](https://twitter.com/madridjug)
