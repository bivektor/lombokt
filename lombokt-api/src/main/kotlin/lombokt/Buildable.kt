package lombokt

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Buildable {

  @Target(AnnotationTarget.CLASS)
  @Retention(AnnotationRetention.SOURCE)
  annotation class Builder
}
