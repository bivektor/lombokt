# lombokt : Lombok for Kotlin

A lightweight port of [Project Lombok](https://projectlombok.org/) to Kotlin, providing useful annotations like `@ToString`, `@EqualsAndHashCode` and `@Buildable`.

Kotlin data classes already provide support for toString, equals and hashcode generation but for regular classes, one has to implement these methods manually. This plugin aims to reduce such boilerplate.
The plugin also provides basic Builder support mainly for Java Interop.

## Features

- `@ToString` – Generates a `toString()` method automatically.
- `@EqualsAndHashCode` – Generates `equals()` and `hashCode()` methods.
- `@Buildable` - Generates Builder class member bodies

## Requirements

- Kotlin JVM (K2 Compiler)
- Tested with Kotlin JVM 2.1.10 

Currently only JVM platform is supported. K2 compiler is required (languageVersion >= 2.0) and there is no plan to support K1.

## Installation

Add the dependency to your `build.gradle.kts`:

```kotlin
dependencies {
    compileOnly("com.bivektor.lombokt:lombok-api:3.0.0-beta.3")
    kotlinCompilerPluginClasspath("com.bivektor.lombokt:lombokt-plugin:3.0.0-beta.3")
}
```

Please do not use versions before 3.0.0 as they are in fact experimental versions mistakenly published as beta.

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

Note that, similar to how data classes work, Lombokt just calls `equals` and `hashcode` methods on the property values.
That's why arrays don't work as expected because of how these methods are defined in the `Array` class.
Lombok handles this case for Java classes but neither Lombokt nor Kotlin data classes have such a special handling, thus one needs to use `List` or similar collection

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

### `@Buildable`

Generates bodies of Builder class methods. Provides basic builder without a separate IDE plugin
thus, method declarations must be present in the user code.

While Builders often don't bring much benefit for Kotlin, we often feel the need for them for Java interop
or APIs targeted to Java especially for classes with many constructor arguments.

In those cases, we either switch to Lombok and implement such classes in Javaor implement the Builder manually.
Implementing Builders in Kotlin is indeed quite simple thanks to `apply` but it is still boilerplate and
you can't make sure your builder works from Java unless you use it in your Kotlin code too or have tests for
Java clients. This plugin makes sure Builder is correctly structured, checking your Builder class methods and
their signatures so we can safely use Kotlin constructors in Kotlin code and still expect Java clients work
through the Builder.

```Kotlin

  @Buildable
  class Person(

    // Required parameter
    val name: String,

    // Parameter without property is possible but problematic for creating a builder from existing instance
    prefix: String? = "Dr.",

    // Nullable parameter with non-null default value
    val age: Int? = 18,

    // Nullable parameter with null as default value
    var email: String? = null,

    // Parameter with default value of complex type
    val profile: Map<String, Any> = emptyMap()
  ) {

    val fullName = "$prefix $name"

    @Buildable.Builder
    class Builder {
      fun name(name: String) = this
      fun age(age: Int?) = this
      fun email(email: String?) = this
      fun profile(profile: Map<String, Any>) = this
      fun prefix(prefix: String?) = this
      fun build() = Person("")
    }

    companion object {
      @JvmStatic
      fun builder() = Builder()
    }
  }

```

```Kotlin
  val personFull = Person.builder().name("John").prefix("Mr.").age(25).email("some").profile(profile).build()
  val personWithDefaults = Person.builder().name("John").build()
```

* Primary constructor with at least one parameter is required.
* Builder class must have a method for each constructor parameter with same name and type
* Both `@Buildable` and `@Builder` annotations are required
* Compilation fails for missing or invalid builder methods
* Builder class cannot have extra property or method declarations
* Annotations can be placed on just regular classes including data classes but not on objects, interfaces, inline, local, enum and value classes
* User defined method bodies are dummy placeholders and, they are always overwritten by the compiler
* Constructor doesn't have to be public, as buildable classes often prefer private constructors
* Companion builder method is optional as the plugin doesn't check or validate Companion object methods
* Generic types are not [supported yet](https://github.com/bivektor/lombokt/issues/41).

## Contributing

Contributions are welcome! Feel free to open issues and submit pull requests.

## License

[Apache License 2](LICENSE)

