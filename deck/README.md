

## What started as...

Learning Exercise to learn FP over Slack

---

## ...Ended in

Solution for Typed FP in Kotlin

---

## What is KΛTEGORY?

A library for typed FP in Kotlin with the traditional type classes and data types

---

## Data types

Many data types to cover general use cases.

|                |                                                      |
|----------------|------------------------------------------------------|
| Error Handling | `Option`,`Try`, `Validated`, `Either`, `Ior`         |
| Collections    | `ListKW`, `SequenceKW`, `MapKW`, `SetKW`             |
| RWS            | `Reader`, `Writer`, `State`                          |
| Transformers   | `ReaderT`, `WriterT`, `OptionT`, `StateT`, `EitherT` |
| Evaluation     | `Eval`, `Trampoline`, `Free`, `FunctionN`            |
| Effects        | `IO`, `Free`, `ObservableKW`                         |
| Others         | `Coproduct`, `Coreader`, `Const`, ...                |

---

## A few syntax examples

```kotlin
import kategory.Option

Option(1).map { it + 1 }
//Option(2)
```

---

## A few syntax examples

```kotlin
import kategory.Try

Try { throw RuntimeException("BOOM!") }.map { it + 1 }
//Failure(RuntimeException("BOOM!"))
```

---

## A few syntax examples

```kotlin
import kategory.Either.*

val x = Right(1)
val y = 1.right()
x == y
//true
```

---

## Applicative Builder

```kotlin:ank
import kategory.*

data class Profile(val id: Long, val name: String, val phone: Int)

fun profile(val maybeId: Option<Long>,
            val maybeName: Option<String>,
            val maybePhone: Option<Int>): Option<Profile> =
  Option.applicative().map(id, name, phone, { (a, b, c) ->
       Profile(a, b, c)
  })

profile(1L.some(), "William Alvin Howard".some(), 555555555.some())
//Some(Profile(1L, "William Alvin Howard", 555555555)
```

---

## Comprehensions - Vanilla

Generalized to all monads

```kotlin:ank
fun profile(val maybeId: Option<Long>,
            val maybeName: Option<String>,
            val maybePhone: Option<Int>): Option<Profile> =
  Option.monad().binding { // <-- `coroutine starts`
    val id = maybeId.bind() // <-- `suspended`
    val name = maybeName.bind() // <-- `suspended`
    val phone = maybePhone.bind() // <-- `suspended`
    yields(Profile(id, name, phone))
  } // <-- `coroutine ends`

profile(2L.some(), "Haskell Brooks Curry".some(), 555555555.some())
```

---

## Comprehensions - Exception Aware

Automatically captures exceptions for instances of `MonadError<F, Throwable>`

```kotlin
Try.monadError().bindingE {
  val name = profileService().bind()
  val phone = phoneService().bind()
  throw RuntimeException("BOOM") // <-- `raises errors to MonadError<F, Throwable>`
  val addresses = addressService().bind()
  yields(Profile(name, phone, addresses))  
}
//Failure(RuntimeException("BOOM"))
```

---

## Comprehensions - Stack-Safe

Stack-Safe comprehensions for Stack-Unsafe data types

```kotlin
fun <F> stackSafeTestProgram(M: Monad<F>, n: Int = 0, stopAt: Int = 1000000): Free<F, Int> =
        M.bindingStackSafe {
            val v = pure(n + 1).bind() // <-- auto binds on `Free<F, Int>`
            val r = if (v < stopAt) { stackSafeTestProgram(M, v, stopAt).bind() }
                    else { pure(v).bind() }
            yields(r)
        }

val M = Id.monad()
stackSafeTestProgram(M).run(M).ev()
//Id(1000000)
```

---

## Monad Comprehensions - Cancellable

Supports cancelling tasks initiated inside the coroutine

```kotlin
val (binding: IO<List<User>>, unsafeCancel: Disposable) =
  ioMonadError.bindingECancellable {
    val userProfile = bindAsync(ioAsync) { getUserProfile("123") }
    val friendProfiles = userProfile.friends().map { friend ->
        bindAsync(ioAsync) { getProfile(friend.id) }
    }
    yields(listOf(userProfile) + friendProfiles)
  } // <- returns `Tuple2<IO<List<User>>, Disposable>`

unsafeCancel() //the disposable instance can cancel all operations inside the coroutine
```

---

## Monad Comprehensions - Context Aware

Allows switching contexts in which `bind/flatMap` takes place

```kotlin
ioMonad.binding {
    val user = bindAsync(ioAsync) { getUserProfile("123") } //<-- binds on IO's pool
    bindIn(DatabaseContext) { storeUser(user) } //<-- binds on DB's pool
    bindIn(UIContext) { toastMessage("User cached!") } //<-- binds on UI thread
    yields(user)
}
```

---

## Transforming immutable data

KΛTEGORY includes an `optics` library that make working with immutable data a breeze

```kotlin
data class Street(val number: Int, val name: String)
data class Address(val city: String, val street: Street)
data class Company(val name: String, val address: Address)
data class Employee(val name: String, val company: Company)

val employee = Employee("John Doe", Company("Kategory", Address("Functional city", Street(23, "lambda street"))))
employee
//Employee(name=John Doe, company=Company(name=Kategory, address=Address(city=Functional city, street=Street(number=23, name=lambda street))))
```

---

## Transforming immutable data

while `kotlin` provides a synthetic `copy` method on data classes dealing with nested data can be tedious

```kotlin
employee.copy(
        company = employee.company.copy(
                address = employee.company.address.copy(
                        street = employee.company.address.street.copy(
                                name = employee.company.address.street.name.capitalize()
                        )
                )
        )
)
//Employee(name=John Doe, company=Company(name=Kategory, address=Address(city=Functional city, street=Street(number=23, name=Lambda street))))
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

val addressStrees: Lens<Address, Street> = Lens(
        get = { it.street },
        set = { street -> { address -> address.copy(street = street) } }
)

val streetName: Lens<Street, String> = Lens(
        get = { it.name },
        set = { name -> { street -> street.copy(name = name) } }
)

val employeeStreetName: Lens<Employee, String> = employeeCompany compose companyAddress compose addressStrees compose streetName

employeeStreetName.modify(employee, String::capitalize)
```

---

## Optics without boilerplate

Or just let KΛTEGORY `@lenses` do the dirty work

```diff
+ @lenses data class Employee(val name: String, val company: Company)
- val employeeCompany: Lens<Employee, Company> = Lens(
-        get = { it.company },
-        set = { company -> { employee -> employee.copy(company = company) } }
- )
-
- val companyAddress: Lens<Company, Address> = Lens(
-        get = { it.address },
-        set = { address -> { company -> company.copy(address = address) } }
- )
-
- val addressStrees: Lens<Address, Street> = Lens(
-        get = { it.street },
-        set = { street -> { address -> address.copy(street = street) } }
- )
-
- val streetName: Lens<Street, String> = Lens(
-        get = { it.name },
-        set = { name -> { street -> street.copy(name = name) } }
- )

val employeeStreetName: Lens<Employee, String> = employeeCompany compose companyAddress compose addressStrees compose streetName

employeeStreetName.modify(employee, String::capitalize)
```

---

## Optics without boilerplate

You can also define custom `Prism` for your sum types

```kotlin
import kategory.*
import kategory.optics.*

sealed class NetworkResult {
    data class Success(val content: String): NetworkResult()
    object Failure: NetworkResult()
}

val networkSuccessPrism: Prism<NetworkResult, NetworkResult.Success> = Prism(
        getOrModify = { networkResult ->
            when(networkResult) {
                is NetworkResult.Success -> networkResult.right()
                else -> networkResult.left()
            }
        },
        reverseGet = { networkResult -> networkResult } //::identity
)
val networkResult = NetworkResult.Success("content")

networkSuccessPrism.modify(networkResult) { success ->
    success.copy(content = "different content")
}
//Success(content=different content)
```

---

## Optics without boilerplate

Or let `@prisms` do that for you

```diff
+ @prisms sealed class NetworkResult {
+    data class Success(val content: String) : NetworkResult()
+    object Failure : NetworkResult()
+ }
-
- sealed class NetworkResult {
-    data class Success(val content: String): NetworkResult()
-    object Failure: NetworkResult()
- }
-
- val networkSuccessPrism: Prism<NetworkResult, NetworkResult.Success> = Prism(
-        getOrModify = { networkResult ->
-            when(networkResult) {
-                is NetworkResult.Success -> networkResult.right()
-                else -> networkResult.left()
-            }
-        },
-        reverseGet = { networkResult -> networkResult } //::identity
- )
```

---

## `@free` & `@tagless`

[Freestyle](http://frees.io) is being ported to Kotlin by the team [@47deg](http://47deg.com).

```diff
+ @free interface GesturesDSL<F> : GesturesDSLKind<A> {
+  fun click(view: UiObject): FreeS<F, Boolean>
+  fun pinchIn(view: UiObject, percent: Int, val steps: Int): FreeS<F, Boolean>
+  fun pinchOut(view: UiObject, percent: Int, val steps: Int): FreeS<F, Boolean>
+ }
-
- typealias ActionDSL<A> = Free<GesturesDSLHK, A>
-
- @higherkind sealed class GesturesDSL<A> : GesturesDSLKind<A> {
-    object PressHome : GesturesDSL<Boolean>()
-    data class Click(val view: UiObject) : GesturesDSL<Boolean>()
-    data class PinchIn(val view: UiObject, val percent: Int, val steps: Int) : GesturesDSL<Boolean>()
-    data class PinchOut(val view: UiObject, val percent: Int, val steps: Int) : GesturesDSL<Boolean>()
-    companion object : FreeMonadInstance<GesturesDSLHK>
- }
-
- fun click(view: UiObject): ActionDSL<Boolean> =
-  Free.liftF(GesturesDSL.Click(ui))
-  
- fun pinchIn(view: UiObject, percent: Int, val steps: Int): ActionDSL<Boolean> =
-  Free.liftF(GesturesDSL.PinchIn(percent, steps))
-   
- fun pinchOut(view: UiObject, percent: Int, val steps: Int): ActionDSL<Boolean> =
-  Free.liftF(GesturesDSL.PinchOut(ui, percent, steps))
```

---

<!-- .slide: class="table-large" -->

## KΛTEGORY is becoming modular

Pick and choose what you'd like to use.

| Module            | Contents                                                              |
|-------------------|-----------------------------------------------------------------------|
| typeclasses       | Semigroup,Monoid, Functor, Applicative, Monad...                      |
| data              | Option, Try, Either, Validated...                                     |
| effects           | IO                                                                    |
| effects-rx2       | ObservableKW                                                          |
| mtl               | MonadReader, MonadState, MonadFilter,...                              |
| free              | Free, FreeApplicative, Trampoline, ...                                |
| freestyle         | @free, @tagless                                                       |
| recursion-schemes |                                                                       |
| optics            | Prism, Iso, Lens, ...                                                 |
| collections       |                                                                       |
| meta              | @higherkind, @deriving, @implicit, @instance, @lenses, @prisms, @isos |

---

## Kotlin limitations for Typed FP

---

## Kotlin limitations for Typed FP

Emulated Higher Kinds through [Lightweight higher-kinded Polymorphism](https://www.cl.cam.ac.uk/~jdy22/papers/lightweight-higher-kinded-polymorphism.pdf)

```kotlin
interface HK<out F, out A>

class OptionHK private constructor()

typealias OptionKind<A> = kategory.HK<OptionHK, A>

inline fun <A> OptionKind<A>.ev(): Option<A> = this as Option<A>

sealed class Option<out A> : OptionKind<A>
```

---

## Kotlin limitations for Typed FP

Fear not, `@higherkind`'s got your back!

```diff
+ @higherkind sealed class Either<A, B> : EitherKind<A, B>
-
- class EitherHK private constructor()
-
- typealias EitherKindPartial<A> = kindedj.Hk<EitherHK, A>
- typealias EitherKind<A, B> = kindedj.Hk<EitherKindPartial<A>, B>
-
- inline fun <A, B> EitherKind<A, B>.ev(): Either<A, B> = this as Either<A, B>
```

---

## Kotlin limitations for Typed FP

No notion of implicits or Type class instance evidences verified at compile time

```kotlin
fun <F, A> A.some(AA: Applicative<OptionHK> /*<-- User is forced to provide instances explicitly */): Option<A> =
  AA.pure(this).ev()

1.some(Option.applicative()) //Option(1)
```

---

## Kotlin limitations for Typed FP

`(for now)` an implicit lookup system based on a global registry is provided

```kotlin
fun <A> A.some(AA: Applicative<OptionHK> = applicative() /*<-- Instances are discovered implicitly */): Option<A> =
  AA.pure(this).ev()

1.some() //Option(1)
```

---

## KΛTEGORY ad-hoc polymorphism

With emulated Higher Kinds and Type classes we can now write polymorphic code

```kotlin
inline fun <reified F, reified E, A> raiseError(e: E, ME: MonadError<F, E> = monadError()): HK<F, A> =
   ME.raiseError(e)

raiseError<EitherKindPartial<String>, String, Int>("Not Found").ev() // <-- This is far from a ideal but `KEEP` this thought
//Left("Not Found")
```

---

## Type Classes

This is how you define Type Classes in KΛTEGORY (for now)

```kotlin
interface Functor<F> : Typeclass {
  fun <A, B> map(fa: HK<F, A>, f: (A) -> B): HK<F, B>
}
```

---

## Implementing type class instances is easy...

---

## @deriving

KΛTEGORY can derive instances based on conventions in your data types

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

For those 3rd party data types not following conventions we can also generate the implicit machinery for discovery

```kotlin
@instance(Either::class)
interface EitherFunctorInstance<L> : Functor<EitherKindPartial<L>> {
    override fun <A, B> map(fa: EitherKind<L, A>, f: (A) -> B): Either<L, B> =
    fa.ev().map(f)
}
```

---

## KEEP-87

But we are not stopping here, we want to get rid of some of these hacks!

KEEP-87 is A KEEP to introduce Type Classes in Kotlin

https://github.com/Kotlin/KEEP/pull/87

---

## KEEP-87

If KEEP-87 makes it to the lang

```kotlin
extension interface Functor<F<_>> { // <-- real Higher kinds positions
  fun <A, B> map(fa: F<A>, f: (A) -> B): F<B>  
}

extension object OptionFunctor: Functor<Option> { // <-- real Higher kinds positions
  fun <A, B> map(fa: Option<A>, f: (A) -> B): Option<B>  
}

fun <F<_>, A, B> transform(fa: F<A>, f: (A) -> B): F<B> given Functor<F> = map(fa, f) // <-- compile time verified

transform(Option(1), { it + 1 })// <-- no need to cast from HK representation
//Option(2)
```

---

<!-- .slide: class="fix-ul" -->

## What if KEEP-87 does not make it to Kotlin?

- `@implicit` as global implicits through a annotation processor / compiler plugin.
- Discuss if a fork to the compiler with compatibility for `KEEP-87` under a compiler flag makes sense.

---

## Credits

KΛTEGORY is inspired in great libraries that have proven useful to the FP community:

- [Cats](https://typelevel.org/cats/)
- [Scalaz](https://github.com/scalaz/scalaz)
- [Freestyle](http://frees.io)
- [Monocle](http://julien-truffaut.github.io/Monocle/)
- [Funktionale](https://github.com/MarioAriasC/funKTionale)
- [Paguro](https://github.com/GlenKPeterson/Paguro)

---

<!-- .slide: class="team" -->

## Team


- ![](https://github.com/anstaendig.png?size=48) **@anstaendig**
- ![](https://github.com/arturogutierrez.png?size=48) **@arturogutierrez**
- ![](https://github.com/ffgiraldez.png?size=48) **@ffgiraldez**
- ![](https://github.com/Guardiola31337.png?size=48) **@Guardiola31337**
- ![](https://github.com/javipacheco.png?size=48) **@javipacheco**
- ![](https://github.com/JMPergar.png?size=48)  **@JMPergar**
- ![](https://github.com/JorgeCastilloPrz.png?size=48) **@JorgeCastilloPrz**
- ![](https://github.com/jrgonzalezg.png?size=48) **@jrgonzalezg**
- ![](https://github.com/nomisRev.png?size=48) **@nomisRev**
- ![](https://github.com/npatarino.png?size=48) **@npatarino**
- ![](https://github.com/pablisco.png?size=48) **@pablisco**
- ![](https://github.com/pakoito.png?size=48)  **@pakoit**
- ![](https://github.com/pedrovgs.png?size=48) **@pedrovgs**
- ![](https://github.com/pt2121.png?size=48)   **@pt2121**
- ![](https://github.com/raulraja.png?size=48) **@raulraja**
- ![](https://github.com/wiyarmir.png?size=48) **@wiyarmir**
- ![](https://github.com/andyscott.png?size=48) **@andyscott**
- ![](https://github.com/Atternatt.png?size=48) **@Atternatt**
- ![](https://github.com/calvellido.png?size=48) **@calvellido**
- ![](https://github.com/dominv.png?size=48) **@dominv**
- ![](https://github.com/GlenKPeterson.png?size=48) **@GlenKPeterson**
- ![](https://github.com/israelperezglez.png?size=48) **@israelperezglez**
- ![](https://github.com/sanogueralorenzo.png?size=48) **@sanogueralorenzo**
- ![](https://github.com/Takhion.png?size=48) **@Takhion**
- ![](https://github.com/victorg1991.png?size=48) **@victorg1991**
- ![](https://github.com/tonilopezmr.png?size=48) **@tonilopezmr**
- ![](https://github.com/NigelHeylen.png?size=48) **@NigelHeylen**
- ![](https://github.com/ersin-ertan.png?size=48) **@ersin-ertan**


---

<!-- .slide: class="join-us" -->

## Join us!

|        |                                                 |
|--------|-------------------------------------------------|
| Github | https://github.com/kategory                     |
| Slack  | https://kotlinlang.slack.com/messages/C5UPMM0A0 |
| Gitter | https://gitter.im/kategory/Lobby                |

We provide 1:1 mentoring for both users & new contributors!

---

## Thanks!

Thanks to everyone that makes KΛTEGORY possible
