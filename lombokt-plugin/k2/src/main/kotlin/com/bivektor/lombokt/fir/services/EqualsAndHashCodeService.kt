package com.bivektor.lombokt.fir.services

import com.bivektor.lombokt.LomboktNames.EQUALS_HASHCODE_ANNOTATION_NAME as ANNOTATION_NAME
import org.jetbrains.kotlin.fir.FirSession

class EqualsAndHashCodeService(session: FirSession) : AnnotatedClassMatchingService(session, ANNOTATION_NAME)

val FirSession.equalsAndHashCodeService: EqualsAndHashCodeService by FirSession.sessionComponentAccessor()
