package lombokt

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class EqualsAndHashCode(
  /**
   * When set to `true` only properties explicitly included via [Include] annotation are used.Defaults to `false`
   */
  val onlyExplicitlyIncluded: Boolean = false,
  val callSuper: Boolean = false,
  val doNotUseGetters: Boolean = false,
) {

  @Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
  @Retention(AnnotationRetention.SOURCE)
  annotation class Include()

  @Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
  @Retention(AnnotationRetention.SOURCE)
  annotation class Exclude()
}
