# lomboKT: Lombok for Kotlin

A lightweight port of [Project Lombok](https://projectlombok.org/) to Kotlin, providing useful
annotations like `@ToString`, `@EqualsAndHashCode`.

Kotlin data classes already provide support for toString, equals and hashcode generation, but for
regular classes, one has to implement these methods manually. This plugin aims to reduce such
boilerplate.
The plugin also provides basic Builder support mainly for Java Interop.

## Features

- `@ToString` – Generates a `toString()` method automatically.
- `@EqualsAndHashCode` – Generates `equals()` and `hashCode()` methods.

## Kotlin Compatibility Matrix

lomboKT package versions include a baseline Kotlin version suffix (for example,
`-kotlin-2.2.0`). If there is no package for a specific Kotlin patch version (for example,
`2.2.10`),
the nearest published baseline in the same Kotlin minor line (for example, `-kotlin-2.2.0`) is
expected to support that version.

| lomboKT version       | Supported Kotlin versions |
|-----------------------|---------------------------|
| `3.1.0-kotlin-2.2.0`  | `2.2.0`, `2.2.10`         |
| `3.1.1-kotlin-2.2.20` | `2.2.20`, `2.2.21`        |
| `3.2.0-kotlin-2.3.0`  | `2.3.0`, `2.3.10`         |
| `3.2.1-kotlin-2.3.20` | `2.3.0`, `2.3.20`         |

## Installation

Add the dependency to your `build.gradle.kts`:

```kotlin
dependencies {
  compileOnly("com.bivektor.lombokt:lombokt-api:<lombokt-version>")
  kotlinCompilerPluginClasspath("com.bivektor.lombokt:lombokt-plugin:<lombokt-version>")
}
```

Version `3.1.0` is the first stable release. Earlier versions including "beta" builds should
be considered alpha/experimental.

#### Maven

```xml

<plugin>
  <groupId>org.jetbrains.kotlin</groupId>
  <artifactId>kotlin-maven-plugin</artifactId>
  <version>${kotlin.version}</version>
  <executions>
    <execution>
      <id>compile</id>
      <phase>compile</phase>
      <goals>
        <goal>compile</goal>
      </goals>
    </execution>
    <execution>
      <id>test-compile</id>
      <phase>test-compile</phase>
      <goals>
        <goal>test-compile</goal>
      </goals>
    </execution>
  </executions>
  <configuration>
    <compilerPlugins>
      <plugin>lombokt</plugin>
    </compilerPlugins>
  </configuration>
  <dependencies>
    <dependency>
      <groupId>com.bivektor.lombokt</groupId>
      <artifactId>lombokt-maven</artifactId>
      <version>${lombokt.version}</version>
    </dependency>
  </dependencies>
</plugin>

```

Full POM can be found in **examples/maven** sample project

## Usage

### `@ToString`

Works for both regular classes and data classes.

For data classes, members declared in the body are not included by default. They require
explicit inclusion via `@ToString.Include`.

Only fields and properties with backing fields are included by default.
Getters without backing fields are also supported via `@ToString.Include`.

Access is always getter-based e.g., outputs are based on getter method results, not field values.

```kotlin
import lombokt.ToString

@ToString
data class User(val username: String, @ToString.Exclude val password: String) {
  @ToString.Include(name = "emailAddress")
  var email: String? = null
}

@ToString(onlyExplicitlyIncluded = true)
class Person(
  @ToString.Include val name: String,
  @ToString.Include(name = "lastName") private val surname: String
) {

  @ToString.Include
  val fullName: String get() = "$name $surname"
}


```

### `@EqualsAndHashCode`

Works for both regular classes and data classes.

Only fields and properties with backing fields are included. Unlike `@ToString`, there is
no way to include computed getters without backing fields. Attempting to do so fails the
compilation.

For regular classes, all such members are included by default. For data classes, only properties
from the primary constructor are considered. `@EqualsAndHashCode.Include` on a member declared
in data-class body fails compilation.

Access is through getters by default, but that can be configured as shown below.

Note that, similar to how data classes work, lomboKT just calls `equals` and `hashcode` methods on
included member values. That's why arrays don't work as expected because of how these methods are
defined in the `Array` class. Lombok handles this case for Java classes, but lomboKT does not have
such a special handling, thus one needs to use `List` or a similar collection.

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

@EqualsAndHashCode(doNotUseGetters = true)
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


@EqualsAndHashCode(onlyExplicitlyIncluded = true)
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
@EqualsAndHashCode(callSuper = true)
class Car(val model: String) : Vehicle("car")


```

## Contributing

Contributions are welcome! Feel free to open issues and submit pull requests.

## License

[Apache License 2](LICENSE)
