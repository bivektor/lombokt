package com.bivektor.lombokt

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

object LomboktNames {
  val EQUALS_HASHCODE_ANNOTATION_NAME = FqName("lombokt.EqualsAndHashCode")
  val EQUALS_METHOD_NAME = Name.identifier("equals")
  val HASHCODE_METHOD_NAME = Name.identifier("hashCode")

  val TO_STRING_ANNOTATION_NAME = FqName("lombokt.ToString")
  val TO_STRING_METHOD_NAME = Name.identifier("toString")

  val BUILDABLE_ANNOTATION_NAME = FqName("lombokt.Buildable")
  val BUILDER_ANNOTATION_NAME = BUILDABLE_ANNOTATION_NAME.child(Name.identifier("Builder"))
  val BUILDABLE_ANNOTATION_ID = ClassId.topLevel(BUILDABLE_ANNOTATION_NAME)
  val BUILDER_ANNOTATION_ID = BUILDABLE_ANNOTATION_ID.createNestedClassId(Name.identifier("Builder"))
  val BUILDER_BUILD_METHOD_NAME = Name.identifier("build")
}
