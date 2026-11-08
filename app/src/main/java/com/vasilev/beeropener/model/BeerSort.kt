package com.vasilev.beeropener.model

/**
 * Сорта напитка с параметрами цвета бутылки, жидкости, пены и крышки.
 */
enum class BeerSort(
    val title: String,
    val subtitle: String,
    val glassColorHex: Long,
    val liquidColorHex: Long,
    val highlightColorHex: Long,
    val foamColorHex: Long,
    val capColorHex: Long
) {
    LAGER(
        title = "Светлое",
        subtitle = "Классический лагер • 4.5%",
        glassColorHex = 0xFF78350F,
        liquidColorHex = 0xFFD97706,
        highlightColorHex = 0xFFFBBF24,
        foamColorHex = 0xFFFFFBEB,
        capColorHex = 0xFFDC2626
    ),
    WHEAT(
        title = "Пшеничное",
        subtitle = "Нефильтрованное • 5.0%",
        glassColorHex = 0xFF14532D,
        liquidColorHex = 0xFFEAB308,
        highlightColorHex = 0xFFFDE047,
        foamColorHex = 0xFFFFFFFF,
        capColorHex = 0xFF2563EB
    ),
    STOUT(
        title = "Тёмное",
        subtitle = "Бархатный стаут • 5.8%",
        glassColorHex = 0xFF27150C,
        liquidColorHex = 0xFF3B1D0E,
        highlightColorHex = 0xFF92400E,
        foamColorHex = 0xFFFDE68A,
        capColorHex = 0xFFD97706
    )
}
