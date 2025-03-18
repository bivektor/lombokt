package lombokt

/**
 * Generates `equals` and `hashCode` methods for the annotated class based on its properties.
 *
 * If either of these methods is declared by the annotated class or one of its super classes as `final` (preventing overrides),
 * code generation is skipped.
 *
 * This annotation supports only regular classes and data classes. It does not work with objects, interfaces, inner,
 * local, inline, or enum classes.
 *
 * By default, it includes all properties with a backing field. For **regular classes**, this means all properties
 * from the primary constructor (if present) and properties declared in the class body. For **data classes**, only
 * primary constructor properties are included.
 *
 * Setting [onlyExplicitlyIncluded] to `true` disables the default convention, requiring properties to be explicitly
 * marked with [Include]. However, in **data classes**, [Include] cannot override the default behavior,
 * meaning properties declared in the class body are always excluded. Since data classes already generate `equals`
 * and `hashCode`, this annotation is only useful for excluding specific properties from their equality logic.
 *
 * By default, property getters are used when generating methods. To use backing fields instead, set [doNotUseGetters]
 * to `true`.
 *
 * The generated equality and hash code logic closely follows Kotlin's data class behavior. This ensures consistency,
 * but note that **arrays do not compare their elements** in `equals` and `hashCode` methods. As a result, array
 * comparisons may not work as expected.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class EqualsAndHashCode(
  /**
   * When `true`, only properties explicitly marked with [Include] are considered. Defaults to `false`.
   */
  val onlyExplicitlyIncluded: Boolean = false,

  /**
   * When `true`, includes superclass implementations of `equals` and `hashCode`. Defaults to `false`.
   *
   * For `equals`, if `super.equals` returns `false`, the generated method also returns `false`. This means classes
   * without a superclass (implicitly inheriting from `Any`) should not call `super.equals`, since `Any` implements
   * equality using **object identity**, which only considers two references equal if they point to the same instance.
   *
   * In general, overriding equality methods in subclasses is discouraged unless you fully control or understand
   * how the superclass implements them.
   */
  val callSuper: Boolean = false,

  /**
   * When `true`, uses backing fields instead of property getters. Defaults to `false`.
   */
  val doNotUseGetters: Boolean = false,
) {

  /**
   * Marks a property for inclusion in the equality logic when [onlyExplicitlyIncluded] is `true`.
   */
  @Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
  @Retention(AnnotationRetention.SOURCE)
  annotation class Include()

  /**
   * Excludes a property from the equality logic.
   */
  @Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
  @Retention(AnnotationRetention.SOURCE)
  annotation class Exclude()
}
