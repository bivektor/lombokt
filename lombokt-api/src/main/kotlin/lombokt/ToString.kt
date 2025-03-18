package lombokt

/**
 * Generates a `toString` method for the annotated class based on its properties.
 *
 * The method is only generated if the class does not already declare `toString` and if no superclass
 * declares it as `final` which prevents us from overriding it.
 *
 * This annotation supports **regular classes**, **objects**, **nested**, and **inner classes**, as well as **data classes**.
 * It does **not** support interfaces, local, inline, or enum classes.
 *
 * By default, it includes all properties with a backing field. Properties from both the **primary constructor** (if present)
 * and the **class body** including `lateinit` properties are included. **Getter-only properties** (without a backing field)
 * are **not included by default** but can be explicitly included via [Include].
 *
 * To override this default inclusion:
 * - Use [Exclude] annotation to exclude specific properties
 * - Set [onlyExplicitlyIncluded] to `true` and use [Include] to explicitly mark properties for inclusion.
 *
 * By default, `toString` calls **property getters** to obtain values. To use backing fields instead, set [doNotUseGetters] to `true`.
 *
 * #### **Generated Output Format**
 * The generated `toString` method follows this format:
 * ```
 * <ShortClassName>(property1=value1, property2=value2, ...)
 * ```
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class ToString(
  /**
   * When `true`, only properties explicitly marked with [Include] are included in `toString()`. Defaults to `false`.
   */
  val onlyExplicitlyIncluded: Boolean = false,

  /**
   * When `true`, includes the super class's `toString()` output as the first item in the generated method.
   * The output will be formatted as `super=<super.toString()>`. Defaults to `false`.
   */
  val callSuper: Boolean = false,

  /**
   * When `true`, uses backing fields instead of calling property getters.
   * **Getter-only properties (without backing fields) are not affected**. Defaults to `false`.
   */
  val doNotUseGetters: Boolean = false
) {

  /**
   * Marks a property to be explicitly included in the `toString()` output.
   *
   * @param name Overrides the property name in the generated output.
   */
  @Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
  @Retention(AnnotationRetention.SOURCE)
  annotation class Include(val name: String = "")

  /**
   * Marks a property to be excluded from the `toString()` output.
   */
  @Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
  @Retention(AnnotationRetention.SOURCE)
  annotation class Exclude()
}
