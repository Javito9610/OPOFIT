package com.opofit.miapp.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * Design tokens del sistema OpoFit Pro.
 *
 * Una sola fuente de verdad para spacing, radios, elevaciones y duración de
 * animaciones — pensado a partir de cómo lo hacen las apps de referencia:
 *  - Strava: spacing 4/8/16, cards radio 12-16
 *  - Whoop / Caliber: cards grandes con radio 20, sombras suaves
 *  - Apple Fitness+: 12 entre elementos, 16-20 entre secciones, radio 20-24
 *  - Hevy: botones 52dp altura, weight semibold
 *
 * Importar como: `import com.opofit.miapp.ui.theme.OpoSpacing`
 */

/** Spacing system (escala 4dp). Cubre el 95% de los casos. */
object OpoSpacing {
    val xs = 4.dp     // padding interno de chips, gap pequeño
    val sm = 8.dp     // gap por defecto en filas/columnas
    val md = 12.dp    // padding interno medio
    val lg = 16.dp    // padding card / lista pro
    val xl = 20.dp    // padding ancho de pantalla
    val xxl = 24.dp   // gap entre secciones grandes
    val xxxl = 32.dp  // separación de bloques principales
}

/** Esquinas redondeadas. */
object OpoRadii {
    val xs = RoundedCornerShape(6.dp)
    val sm = RoundedCornerShape(10.dp)
    val md = RoundedCornerShape(14.dp)   // botones pro
    val lg = RoundedCornerShape(18.dp)   // cards estándar
    val xl = RoundedCornerShape(24.dp)   // cards grandes / sheets
    val full = RoundedCornerShape(50)    // botón pill
}

/** Elevación / sombras. M3 usa tonal elevation + drop shadow combinados. */
object OpoElevation {
    val l0 = 0.dp
    val l1 = 1.dp     // card discreta, lista
    val l2 = 3.dp     // card destacada
    val l3 = 6.dp     // FAB
    val l4 = 10.dp    // modal / dialog
}

/** Alturas estándar de touch targets (mínimo 48dp Android). */
object OpoSizes {
    val buttonHeight = 52.dp           // ProButton estándar (Hevy/Caliber)
    val buttonHeightSmall = 40.dp      // botón secundario en línea
    val touchTarget = 48.dp            // mínimo Material
    val iconSm = 16.dp
    val iconMd = 20.dp
    val iconLg = 24.dp
    val iconXl = 32.dp
    val avatar = 40.dp
    val avatarLg = 56.dp
}

/** Duración de animaciones (ms). */
object OpoAnims {
    const val fast = 150
    const val normal = 250
    const val slow = 400
}
