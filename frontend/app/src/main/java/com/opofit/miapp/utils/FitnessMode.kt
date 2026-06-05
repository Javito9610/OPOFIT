package com.opofit.miapp.utils

object FitnessMode {
    const val PLAN_OPOSICION_ID = 1

    fun isFitness(modoUso: String?): Boolean =
        modoUso?.equals("FITNESS", ignoreCase = true) == true

    fun planOposicionId(oposicionId: Int?, modoUso: String?): Int =
        if (isFitness(modoUso) || oposicionId == null || oposicionId <= 0) PLAN_OPOSICION_ID else oposicionId
}
