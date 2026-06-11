package com.opofit.miapp.gps.util

import org.junit.Assert.assertTrue
import org.junit.Test

class GpsPermissionRequestTest {

    @Test
    fun requiredPermissions_incluye_ubicacion() {
        val perms = GpsPermissionRequest.requiredPermissions()
        assertTrue(perms.any { it.contains("LOCATION") })
    }
}
