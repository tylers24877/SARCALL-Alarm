/*
 *  Copyright (C) Tyler Simmonds - All Rights Reserved
 *  Unauthorised copying of this file, via any medium is prohibited
 *  Written by Tyler Simmonds on behalf of SARCALL LTD, 2021
 *
 */

package uk.mrs.saralarm.support

import com.google.errorprone.annotations.Keep

@Keep
enum class SARResponseCode {
    SAR_A, SAR_L, SAR_N, SAR_H, SIGN_ON, SIGN_OFF
}