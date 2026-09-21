package com.shifumon.config

/** Posição de um elemento: deslocamento em pixels da interface a partir da [anchor]. */
data class HudPosition(
    var anchor: HudAnchor = HudAnchor.TOP_LEFT,
    var x: Int = 0,
    var y: Int = 0,
)
