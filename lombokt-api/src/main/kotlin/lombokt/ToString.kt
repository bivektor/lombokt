package lombokt

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class ToString(
  val onlyExplicitlyIncluded: Boolean = false,
  val callSuper: Boolean = false
) {

  @Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
  @Retention(AnnotationRetention.SOURCE)
  annotation class Include()

  @Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
  @Retention(AnnotationRetention.SOURCE)
  annotation class Exclude()
}