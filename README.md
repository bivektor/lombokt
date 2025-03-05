# lombokt

A lightweight port of Project Lombok to Kotlin, providing useful annotations like `@ToString`, `@EqualsAndHashCode`.
Kotlin data classes already provide support for toString, equals and hashcode generation but for regular classes, one has to implement these methods manually. This plugin aims to reduce such boilerplate.
It also supports data classes mainly for excluding specific properties or maybe inclusion of additional properties especially for toString generation

## Features

- `@ToString` – Generates a `toString()` method automatically. 
- `@EqualsAndHashCode` – Generates `equals()` and `hashCode()` methods. 

## Requirements

- JDK 17+
- Kotlin JVM 2.1+ (K2 Compiler)

Currently only K2 compiler is supported and there is no plan to support K1 but help is welcome if you think K1 should be supported too

## Installation

Add the dependency to your `build.gradle.kts`:

```kotlin
dependencies {
    compileOnly("com.bivektor.lombokt:lombok-api:2.1.10-beta.1")
    kotlinCompilerPluginClasspath("com.bivektor.lombokt:lombokt-plugin:2.1.10-beta.2")
}
```

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

Only fields and properties with backing fields are included. 
Access is always by fields, thus property getters are not used.

```kotlin
import lombokt.EqualsAndHashCode

@EqualsAndHashCode
data class Order(
  val orderId: String,
  val items: List<Item>,
  
  @EqualsAndHashCode.Exclude
  val createdBy: String
)

@EqualsAndHashCode(onlyExplicitlyIncluded=true)
class Order(
  @EqualsAndHashCode.Include val orderId: String,
  val items: List<Item>,
  val createdBy: String
)


```

## Contributing

Contributions are welcome! Feel free to open issues and submit pull requests.

## License

[Apache License 2](LICENSE)

