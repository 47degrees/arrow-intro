## Higher Kinded Types emulation with KindedJ

For any datatype annotated with @higherkind

```kotlin
@higherkind
sealed class Either<A, B> : EitherKind<A, B>
```

we generate the following boilerplate

```kotlin
class EitherHK private constructor()

typealias EitherKindPartial<A> = kindedj.Hk<EitherHK, A>
typealias EitherKind<A, B> = kindedj.Hk<EitherKindPartial<A>, B>

inline fun <A, B> EitherKind<A, B>.ev(): Either<A, B> = this as Either<A, B>
```
