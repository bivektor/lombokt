package lombokt

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class EqualsAndHashCode(
  /**
   * When set to `true` only properties explicitly included are used. Note that for lateinit properties, if [includeLateInits]
   * is also `true`, they are treated as if they are explicitly included.
   * For other properties, including the ones in the primary constructor, [Include] annotation is required.
   * Defaults to `false`
   */
  val onlyExplicitlyIncluded: Boolean = false,
  val callSuper: Boolean = false,
  val doNotUseGetters: Boolean = false,

  /**
   * Whether to include lateinit properties. Defaults to `false`.
   * When set to `true`, all lateinit properties are treated as if they are explicitly included.
   *
   * Note that lateinit properties are problematic for equality because equals and hashcode methods are expected to not
   * throw exceptions but uninitialized lateinit properties throw runtime exceptions. That's why, if a lateinit property
   * is not explicitly included either via this option or through [Include] annotation, compilation fails with an exception
   * @see [onlyExplicitlyIncluded]
   */
  val includeLateInits: Boolean = false
) {

  @Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
  @Retention(AnnotationRetention.SOURCE)
  annotation class Include()

  @Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
  @Retention(AnnotationRetention.SOURCE)
  annotation class Exclude()
}
