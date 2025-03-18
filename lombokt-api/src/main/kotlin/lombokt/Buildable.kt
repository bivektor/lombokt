package lombokt

/**
 * Marks a class as **Buildable**, indicating that a nested **Builder** class exists to construct instances of it.
 *
 * A **Buildable** class must:
 * - Have a **primary constructor**.
 * - Contain a **nested class** annotated with [Builder].
 *
 * This annotation supports **regular classes** and **data classes**.
 * It does **not** support **inner classes, objects, interfaces, enums, inline, or local classes**.
 *
 * See [Builder] for details on implementing a builder.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Buildable {

  /**
   * Marks a **nested class** as the **Builder** for its enclosing **Buildable** class.
   *
   * The **Builder** class must:
   * - Be a **nested class** inside a class annotated with [Buildable].
   * - Declare a **setter method** for each **constructor parameter** in the enclosing class.
   * - Provide a **`build()` method** that returns an instance of the enclosing class.
   *
   * #### **Method Requirements**
   * - Each setter method must have the **same name** as the corresponding constructor parameter.
   * - Each setter method must have a single parameter of the same type as the corresponding constructor parameter.
   * - Each setter method must return the **Builder instance** to allow method chaining.
   * - The `build()` method must return an instance of the enclosing class.
   *
   * **Lombokt automatically overrides all method bodies that match these criteria.**
   * For this reason, the simplest way to define a Builder class is:
   * - Return `this` in setter methods.
   * - Throw an error in the `build()` method.
   *
   * #### **Example**
   * ```kotlin
   * @Buildable
   * class Person(val name: String) {
   *
   *     @Buildable.Builder
   *     class Builder {
   *         fun name(name: String) = this
   *         fun build(): Person = error("not implemented")
   *     }
   * }
   * ```
   *
   * **How Lombokt Generates the Builder Implementation:**
   * - It creates **private nullable properties** in the Builder class, corresponding to the constructor parameters.
   * - Each setter method updates the corresponding private property.
   * - The `build()` method calls the **constructor** of the Buildable class, passing the values from the Builder properties.
   *
   * #### **Handling Default Values**
   * If a **constructor parameter has a default value**, Lombokt ensures:
   * - If the property is **unset**, the default value is used when calling the constructor.
   * - If the property is **nullable but has a non-null default value**, Lombokt cannot distinguish between an **unset property**
   *   and one explicitly set to `null`.
   *   In this case, Lombokt generates an **additional private Boolean flag** to track whether the property was explicitly set.
   */
  @Target(AnnotationTarget.CLASS)
  @Retention(AnnotationRetention.SOURCE)
  annotation class Builder
}
