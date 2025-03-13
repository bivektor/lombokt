# lombokt

A lightweight port of [Project Lombok](https://projectlombok.org/) to Kotlin, providing useful annotations like `@ToString`, `@EqualsAndHashCode`.
Kotlin data classes already provide support for toString, equals and hashcode generation but for regular classes, one has to implement these methods manually. This plugin aims to reduce such boilerplate.
It also supports data classes mainly for excluding specific properties or maybe inclusion of additional properties especially for toString generation

## Features

- `@ToString` – Generates a `toString()` method automatically.
- `@EqualsAndHashCode` – Generates `equals()` and `hashCode()` methods.

## Requirements

- Kotlin JVM 2.1+ (K2 Compiler)

Currently only JVM platform is supported through K2 compiler and there is no plan to support K1.

## Installation

Add the dependency to your `build.gradle.kts`:

```kotlin
dependencies {
    compileOnly("com.bivektor.lombokt:lombok-api:3.0.0-beta.1")
    kotlinCompilerPluginClasspath("com.bivektor.lombokt:lombokt-plugin:3.0.0-beta.2")
}
```

Please do not use versions before 3.0.0 as they are in fact experimental versions mistakenly published as beta.

Or if using Maven:

TODO

## Usage

### `@ToString`

Works for both regular classes and data classes.
For data classes, supports inclusion of properties defined in the class body and exclusion of specific properties
from the primary constructor.

Only fields and getters are included. For a property with backing field, always getter is used.
Getters without backing fields are also supported.

```kotlin
import lombokt.ToString

@ToString
data class User(val username: String, @ToString.Exclude val password: Int) {
  @ToString.Include(name="emailAddress")
  var email: String? = null
}

@ToString(onlyExplicitlyIncluded=true)
class Person(@ToString.Include val name: String, @ToString.Include(name="custom") private val surname: String) {

  @ToString.Include
  val fullName: String get() = "$name $surname"
}


```

### `@EqualsAndHashCode`

Works for both regular classes and data classes.

Only properties with backing fields are included.
Access is through getters by default but that can be configured as shown below.
For data classes, only properties from the primary constructor are considered.
For non-data classes, all properties including the ones declared in the class body are included by default.

```kotlin
import lombokt.EqualsAndHashCode

@EqualsAndHashCode
data class Order(
  val orderId: String,
  val items: List<Item>,

  @EqualsAndHashCode.Exclude
  val createdBy: String
) {
  // This is not included because properties declared in class body are not supported for data classes
  var code: String = "some"
}

@EqualsAndHashCode
class Order(
  val orderId: String,
  val items: List<Item>,

  @EqualsAndHashCode.Exclude
  val createdBy: String
) {
  // This property is automatically included because this is not a data class. Its getter is used by default.
  var code: String = "some"
    get() = field.uppercase()
}

@EqualsAndHashCode(doNotUseGetters=true)
class Order(
  val orderId: String,
  val items: List<Item>,

  @EqualsAndHashCode.Exclude
  val createdBy: String
) {
  // Direct field access. Getter is not used
  var code: String = "some"
    get() = field.uppercase()
}


@EqualsAndHashCode(onlyExplicitlyIncluded=true)
class Order(
  // Only orderId is included
  @EqualsAndHashCode.Include val orderId: String,
  val items: List<Item>,
  val createdBy: String
)

@EqualsAndHashCode
open class Vehicle(val type: String)

// Super class methods are used both for equals and hashCode methods in addition to own properties
// Do not call super when there is no super class or if you are not sure super class handles equality correctly. See Lombok for more info
@EqualsAndHashCode(callSuper=true)
class Car(val model: String) : Vehicle("car")


```

## Contributing

Contributions are welcome! Feel free to open issues and submit pull requests.

## License

[Apache License 2](LICENSE)

