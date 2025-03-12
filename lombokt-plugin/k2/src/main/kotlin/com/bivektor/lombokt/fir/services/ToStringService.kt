package com.bivektor.lombokt.fir.services

import com.bivektor.lombokt.LomboktNames.TO_STRING_ANNOTATION_NAME
import org.jetbrains.kotlin.fir.FirSession

class ToStringService(session: FirSession) : AnnotatedClassMatchingService(session, TO_STRING_ANNOTATION_NAME)

val FirSession.toStringService: ToStringService by FirSession.sessionComponentAccessor()
