package com.bivektor.lombokt.fir

import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.types.classLikeLookupTagIfAny
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.name.ClassId

fun List<FirAnnotation>.findAnnotation(classId: ClassId): FirAnnotation? {
  return firstOrNull { it.annotationTypeRef.coneType.classLikeLookupTagIfAny?.classId == classId }
}