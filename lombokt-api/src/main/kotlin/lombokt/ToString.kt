package lombokt

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class ToString(
  val onlyExplicitlyIncluded: Boolean = false,
  val callSuper: Boolean = false,
  val doNotUseGetters: Boolean = false
) {

  @Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
  @Retention(AnnotationRetention.SOURCE)
  annotation class Include(val name: String = "")

  @Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
  @Retention(AnnotationRetention.SOURCE)
  annotation class Exclude()
}