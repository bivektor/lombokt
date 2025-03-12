package com.bivektor.lombokt

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

object LomboktNames {
  val EQUALS_HASHCODE_ANNOTATION_NAME = FqName("lombokt.EqualsAndHashCode")
  val EQUALS_METHOD_NAME = Name.identifier("equals")
  val HASHCODE_METHOD_NAME = Name.identifier("hashCode")

  val TO_STRING_ANNOTATION_NAME = FqName("lombokt.ToString")
  val TO_STRING_METHOD_NAME = Name.identifier("toString")
}
