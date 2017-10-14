## Comprehension cancellation

```kotlin
val (binding: IO<List<User>>, dispose: Disposable) = ioMonadError.bindingECancellable {
    val userProfile = bindAsync(ioAsync) { getUserProfile("123") }
    val friendProfiles = profile.friends().map { friend ->
        bindAsync(ioAsync) { getProfile(friend.id) }
    }
    yields(listOf(userProfile) + friendProfiles)
}
```