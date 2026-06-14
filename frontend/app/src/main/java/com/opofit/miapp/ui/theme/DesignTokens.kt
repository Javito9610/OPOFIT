package com.opofit.miapp.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * Design tokens OpoFit Dark Pro.
 * Referencias: Strava, TrainingPeaks, Whoop, Hevy.
 */

object OpoSpacing {
    val xs   =  4.dp   // gap mínimo, padding chips
    val sm   =  8.dp   // gap entre elementos inline
    val md   = 12.dp   // padding interno medio
    val lg   = 16.dp   // padding card / lista
    val xl   = 20.dp   // padding lateral de pantalla
    val xxl  = 24.dp   // separación entre secciones
    val xxxl = 32.dp   // bloques principales
}

object OpoRadii {
    val xs   = RoundedCornerShape(4.dp)
    val sm   = RoundedCornerShape(8.dp)
    val md   = RoundedCornerShape(12.dp)   // botones, chips
    val lg   = RoundedCornerShape(16.dp)   // cards estándar
    val xl   = RoundedCornerShape(20.dp)   // hero cards / sheets
    val full = RoundedCornerShape(50)      // pill
}

object OpoElevation {
    val l0 = 0.dp
    val l1 = 1.dp    // card discreta
    val l2 = 3.dp    // card destacada
    val l3 = 6.dp    // FAB
    val l4 = 10.dp   // dialog / modal
}

object OpoSizes {
    val buttonHeight      = 52.dp
    val buttonHeightSmall = 40.dp
    val touchTarget       = 48.dp
    val iconSm            = 16.dp
    val iconMd            = 20.dp
    val iconLg            = 24.dp
    val iconXl            = 32.dp
    val avatar            = 40.dp
    val avatarLg          = 56.dp
    // Dark Pro extras
    val statNumber        = 48.dp   // altura de fila para métricas grandes
    val heroCard          = 200.dp  // altura mínima del hero card de entreno
}

object OpoAnims {
    const val fast   = 120
    const val normal = 220
    const val slow   = 380
}
