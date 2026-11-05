@file:OptIn(InternalResourceApi::class)

package com.lagradost.cloudstream4.shared.generated.resources

import kotlin.OptIn
import kotlin.String
import kotlin.collections.MutableMap
import org.jetbrains.compose.resources.FontResource
import org.jetbrains.compose.resources.InternalResourceApi
import org.jetbrains.compose.resources.ResourceContentHash
import org.jetbrains.compose.resources.ResourceItem

private const val MD: String =
    "composeResources/com.lagradost.cloudstream4.shared.generated.resources/"

@delegate:ResourceContentHash(611_555_010)
internal val Res.font.productsans_black: FontResource by lazy {
      FontResource("font:productsans_black", setOf(
        ResourceItem(setOf(), "${MD}font/productsans_black.ttf", -1, -1),
      ))
    }

@delegate:ResourceContentHash(-771_478_809)
internal val Res.font.productsans_blackitalic: FontResource by lazy {
      FontResource("font:productsans_blackitalic", setOf(
        ResourceItem(setOf(), "${MD}font/productsans_blackitalic.ttf", -1, -1),
      ))
    }

@delegate:ResourceContentHash(-1_008_627_421)
internal val Res.font.productsans_bold: FontResource by lazy {
      FontResource("font:productsans_bold", setOf(
        ResourceItem(setOf(), "${MD}font/productsans_bold.ttf", -1, -1),
      ))
    }

@delegate:ResourceContentHash(1_235_149_050)
internal val Res.font.productsans_bolditalic: FontResource by lazy {
      FontResource("font:productsans_bolditalic", setOf(
        ResourceItem(setOf(), "${MD}font/productsans_bolditalic.ttf", -1, -1),
      ))
    }

@delegate:ResourceContentHash(578_188_792)
internal val Res.font.productsans_italic: FontResource by lazy {
      FontResource("font:productsans_italic", setOf(
        ResourceItem(setOf(), "${MD}font/productsans_italic.ttf", -1, -1),
      ))
    }

@delegate:ResourceContentHash(-373_999_550)
internal val Res.font.productsans_light: FontResource by lazy {
      FontResource("font:productsans_light", setOf(
        ResourceItem(setOf(), "${MD}font/productsans_light.ttf", -1, -1),
      ))
    }

@delegate:ResourceContentHash(1_059_771_191)
internal val Res.font.productsans_lightitalic: FontResource by lazy {
      FontResource("font:productsans_lightitalic", setOf(
        ResourceItem(setOf(), "${MD}font/productsans_lightitalic.ttf", -1, -1),
      ))
    }

@delegate:ResourceContentHash(719_622_017)
internal val Res.font.productsans_medium: FontResource by lazy {
      FontResource("font:productsans_medium", setOf(
        ResourceItem(setOf(), "${MD}font/productsans_medium.ttf", -1, -1),
      ))
    }

@delegate:ResourceContentHash(-1_603_372_832)
internal val Res.font.productsans_mediumitalic: FontResource by lazy {
      FontResource("font:productsans_mediumitalic", setOf(
        ResourceItem(setOf(), "${MD}font/productsans_mediumitalic.ttf", -1, -1),
      ))
    }

@delegate:ResourceContentHash(1_495_636_086)
internal val Res.font.productsans_regular: FontResource by lazy {
      FontResource("font:productsans_regular", setOf(
        ResourceItem(setOf(), "${MD}font/productsans_regular.ttf", -1, -1),
      ))
    }

@delegate:ResourceContentHash(16_707_340)
internal val Res.font.productsans_thin: FontResource by lazy {
      FontResource("font:productsans_thin", setOf(
        ResourceItem(setOf(), "${MD}font/productsans_thin.ttf", -1, -1),
      ))
    }

@delegate:ResourceContentHash(766_043_637)
internal val Res.font.productsans_thinitalic: FontResource by lazy {
      FontResource("font:productsans_thinitalic", setOf(
        ResourceItem(setOf(), "${MD}font/productsans_thinitalic.ttf", -1, -1),
      ))
    }

@InternalResourceApi
internal fun _collectCommonMainFont0Resources(map: MutableMap<String, FontResource>) {
  map.put("productsans_black", Res.font.productsans_black)
  map.put("productsans_blackitalic", Res.font.productsans_blackitalic)
  map.put("productsans_bold", Res.font.productsans_bold)
  map.put("productsans_bolditalic", Res.font.productsans_bolditalic)
  map.put("productsans_italic", Res.font.productsans_italic)
  map.put("productsans_light", Res.font.productsans_light)
  map.put("productsans_lightitalic", Res.font.productsans_lightitalic)
  map.put("productsans_medium", Res.font.productsans_medium)
  map.put("productsans_mediumitalic", Res.font.productsans_mediumitalic)
  map.put("productsans_regular", Res.font.productsans_regular)
  map.put("productsans_thin", Res.font.productsans_thin)
  map.put("productsans_thinitalic", Res.font.productsans_thinitalic)
}
