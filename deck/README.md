
<!-- .slide: class="center" -->

## KΛTEGORY

Functional data types & abstractions for Kotlin

---

## Origins

From Learning Exercise -> Solution for Typed FP in Kotlin

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
```

---

## A few syntax examples

```kotlin
import kategory.Try

Try { throw RuntimeException("BOOM!") }.map { it + 1 }
```

---

## A few syntax examples

```kotlin
import kategory.Either.*

val x = Right(1)
val y = 1.right()
x == y
```

---

## Applicative Builder

```kotlin:ank
import kategory.*

data class Profile(val id: Long, val name: String, val phone: Int)

fun profile(val maybeId: Option<Long>, val maybeName: Option<String>, val maybePhone: Option<Int>): Option<Profile> = 
  Option.applicative().map(id, name, phone, { (n, phone, addresses) ->
       Profile(name, phone, addresses)
  })

profile(1L.some(), "William Alvin Howard".some(), 555555555.some())
```

---

## Monad Comprehensions - Vanilla

```kotlin:ank
fun profile(val maybeId: Option<Long>, val maybeName: Option<String>, val maybePhone: Option<Int>): Option<Profile> = 
  Option.monad().binding {
    val id = maybeId.bind()
    val name = maybeName.bind()
    val phone = maybePhone.bind()
    yields(Profile(id, name, phone))
  }
  
profile(2L.some(), "Haskell Brooks Curry".some(), 555555555.some())
```

---

## Monad Comprehensions - Exception Aware

```kotlin
Try.monadError().bindingE {
  val name = profileService().bind()
  val phone = phoneService().bind() //throws ex
  val addresses = addressService().bind()
  yields(Profile(name, phone, addresses))  
}
// Failure(RuntimeException("Phone Service was unavailable"))
```

---

## Monad Comprehensions - Stack-Safe

Stack-Safe comprehensions for Stack-Unsafe data types

```kotlin:ank
fun <F> stackSafeTestProgram(M: Monad<F>, n: Int, stopAt: Int): Free<F, Int> = M.bindingStackSafe {
  val v = pure(n + 1).bind()
  val r = if (v < stopAt) stackSafeTestProgram(M, v, stopAt).bind() else pure(v).bind()
  yields(r)
}
stackSafeTestProgram(Id.monad(), 0, 500000)
```

---

## Monad Comprehensions - Cancellable

```kotlin
val (binding: IO<List<User>>, unsafeCancel: Disposable) = 
  ioMonadError.bindingECancellable {
    val userProfile = bindAsync(ioAsync) { getUserProfile("123") }
    val friendProfiles = userProfile.friends().map { friend ->
        bindAsync(ioAsync) { getProfile(friend.id) }
    }
    yields(listOf(userProfile) + friendProfiles)
  }
  
unsafeCancel() //cancels all operations inside the coroutine
```

---

## Monad Comprehensions - Context Aware

Support for CoroutineContext

```kotlin
ioMonad.binding {
    val user = bindAsync(ioAsync) { getUserProfile("123") }
    bindIn(DatabaseContext) { storeUser(user) }
    bindIn(UIContext) { toastMessage("User cached!") }
    yields(user)
}
```

---

## Kotlin limitations

- Lacks Higher Kinded Types
- Can't support compile time verified type class instances.

---

# Higher Kinded Types

For any datatype annotated with `@higherkind`

```kotlin
@higherkind
sealed class Either<A, B> : EitherKind<A, B>
```

A HK representation is automatically provided

```kotlin
class EitherHK private constructor()

typealias EitherKindPartial<A> = kindedj.Hk<EitherHK, A>
typealias EitherKind<A, B> = kindedj.Hk<EitherKindPartial<A>, B>

inline fun <A, B> EitherKind<A, B>.ev(): Either<A, B> = this as Either<A, B>
```

---

We can now write polymorphic code in Kotlin that uses higher kinds

```kotlin
inline fun <reified F, reified E, A> raiseError(e: E, ME: MonadError<F, E> = monadError()): HK<F, A> = ME.raiseError(e)

raiseError<EitherKindPartial<String>, String, Int>("Not Found")
```

---

# Type Classes

```kotlin
interface Functor<F> : Typeclass {
  fun <A, B> map(fa: HK<F, A>, f: (A) -> B): HK<F, B>
}
```

---

# @deriving

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

# @instance

```kotlin
@instance(Either::class)
interface EitherFunctorInstance<L> : Functor<EitherKindPartial<L>> {
    override fun <A, B> map(fa: EitherKind<L, A>, f: (A) -> B): Either<L, B> = 
    fa.ev().map(f)
}
```

---

# KEEP-87

A KEEP for Type Classes in Kotlin

https://github.com/Kotlin/KEEP/pull/87

---

# KEEP-87

Without type class support

```kotlin
interface Functor<F> : Typeclass {
  fun <A, B> map(fa: HK<F, A>, f: (A) -> B): HK<F, B> //emulated hks  
}

fun <F, A, B> transform(fa: HK<F, A>, f: (A) -> B, FF: Functor<F> = functor()): HK<F, B> = FF.map(fa, f) //reflection based runtime lookups

transform(Option(1), { it + 1}).ev() // safe downcast
```

---

# KEEP-87

If KEEP-87 makes it to the lang

```kotlin
extension interface Functor<F<_>> { //real Higher kinds positions
  fun <A, B> map(fa: F<A>, f: (A) -> B): F<B>  
}

fun <F<_>, A, B> transform(fa: F<A>, f: (A) -> B): F<B> given Functor<F> = map(fa, f) //compile time verified

transform(Option(1), { it + 1 })// no need to downcast
```

---

# KEEP-87

If KEEP-87 does not make it to the lang

We will either support

- Plan A: @implicit as global implicits through a compiler plugin
- Plan B: Maintain a compiler fork that includes support for `KEEP-87` under a compiler flag

---

Modular

Being built and refactored with Android size constrains in mind.

- meta (@higherkind, @deriving, @implicit, @instance, @lenses, @prisms, @isos)
- core (Semigroup, Monoid, Functor, Applicative, Monad...)
- data (Option, Try, Either, Validated...)
- effects (IO)
- effects-rx2 (ObservableKW)
- mtl (MonadReader, MonadState, MonadFilter,...)
- free (Free, FreeApplicative, Trampoline, ...)
- freestyle (@free, @tagless)
- recursion 
- optics (Prism, Iso, Lens, ...)

---

# Credits

---

# Team

Contributors:

|                                                      |                   |
|------------------------------------------------------|-------------------|
| ![](https://github.com/anstaendig.png?size=48)       | @anstaendig       |
| ![](https://github.com/arturogutierrez.png?size=48)  | @arturogutierrez  |
| ![](https://github.com/ffgiraldez.png?size=48)       | @ffgiraldez       |
| ![](https://github.com/Guardiola31337.png?size=48)   | @Guardiola31337   |
| ![](https://github.com/javipacheco.png?size=48)      | @javipacheco      |
| ![](https://github.com/JMPergar.png?size=48)         | @JMPergar         |
| ![](https://github.com/JorgeCastilloPrz.png?size=48) | @JorgeCastilloPrz |
| ![](https://github.com/jrgonzalezg.png?size=48)      | @jrgonzalezg      |
| ![](https://github.com/nomisRev.png?size=48)         | @nomisRev         |
| ![](https://github.com/npatarino.png?size=48)        | @npatarino        |
| ![](https://github.com/pablisco.png?size=48)         | @pablisco         |
| ![](https://github.com/pakoito.png?size=48)          | @pakoito          |
| ![](https://github.com/pedrovgs.png?size=48)         | @pedrovgs         |
| ![](https://github.com/pt2121.png?size=48)           | @pt2121           |
| ![](https://github.com/raulraja.png?size=48)         | @raulraja         |
| ![](https://github.com/wiyarmir.png?size=48)         | @wiyarmir         |
| ![](https://github.com/andyscott.png?size=48)        | @andyscott        |
| ![](https://github.com/Atternatt.png?size=48)        | @Atternatt        |
| ![](https://github.com/calvellido.png?size=48)       | @calvellido       |
| ![](https://github.com/dominv.png?size=48)           | @dominv           |
| ![](https://github.com/GlenKPeterson.png?size=48)    | @GlenKPeterson    |
| ![](https://github.com/israelperezglez.png?size=48)  | @israelperezglez  |
| ![](https://github.com/sanogueralorenzo.png?size=48) | @sanogueralorenzo |
| ![](https://github.com/Takhion.png?size=48)          | @Takhion          |
| ![](https://github.com/victorg1991.png?size=48)      | @victorg1991      |
| ![](https://github.com/tonilopezmr.png?size=48)      | @tonilopezmr      |
| ![](https://github.com/NigelHeylen.png?size=48)      | @NigelHeylen      |
| ![](https://github.com/ersin-ertan.png?size=48)      | @ersin-ertan      |


Teams:
Fine Cinnamon @ Slack, 47 Degrees, KindedJ organisation, JetBrains

---

# Join us!

[Kategory org](https://github.com/kategory)
[Kategory gitter](https://gitter.im/kategory)
#kategory in KotlinLang
#kategory in Android Study Group

We provide 1:1 mentoring for both users & new contributors!

---

# Thanks!

