@file:OptIn(InternalResourceApi::class)

package com.lagradost.cloudstream4.shared.generated.resources

import kotlin.OptIn
import kotlin.String
import kotlin.collections.MutableMap
import org.jetbrains.compose.resources.InternalResourceApi
import org.jetbrains.compose.resources.ResourceContentHash
import org.jetbrains.compose.resources.ResourceItem
import org.jetbrains.compose.resources.StringResource

private const val MD: String =
    "composeResources/com.lagradost.cloudstream4.shared.generated.resources/"

@delegate:ResourceContentHash(-1_581_170_172)
internal val Res.string.action_bar_up_description: StringResource by lazy {
      StringResource("string:action_bar_up_description", "action_bar_up_description", setOf(
        ResourceItem(setOf(), "${MD}values/strings.commonMain.cvr", 10, 49),
      ))
    }

@delegate:ResourceContentHash(915_272_357)
internal val Res.string.app_name: StringResource by lazy {
      StringResource("string:app_name", "app_name", setOf(
        ResourceItem(setOf(), "${MD}values/strings.commonMain.cvr", 60, 32),
      ))
    }

@delegate:ResourceContentHash(-1_320_945_082)
internal val Res.string.cancel: StringResource by lazy {
      StringResource("string:cancel", "cancel", setOf(
        ResourceItem(setOf(), "${MD}values/strings.commonMain.cvr", 93, 22),
      ))
    }

@delegate:ResourceContentHash(-1_694_702_464)
internal val Res.string.color: StringResource by lazy {
      StringResource("string:color", "color", setOf(
        ResourceItem(setOf(), "${MD}values/strings.commonMain.cvr", 152, 21),
      ))
    }

@delegate:ResourceContentHash(964_389_357)
internal val Res.string.color_wheel: StringResource by lazy {
      StringResource("string:color_wheel", "color_wheel", setOf(
        ResourceItem(setOf(), "${MD}values/strings.commonMain.cvr", 116, 35),
      ))
    }

@delegate:ResourceContentHash(297_146_880)
internal val Res.string.lorem_content: StringResource by lazy {
      StringResource("string:lorem_content", "lorem_content", setOf(
        ResourceItem(setOf(), "${MD}values/strings.commonMain.cvr", 174, 617),
      ))
    }

@delegate:ResourceContentHash(-1_132_581_930)
internal val Res.string.lorem_title: StringResource by lazy {
      StringResource("string:lorem_title", "lorem_title", setOf(
        ResourceItem(setOf(), "${MD}values/strings.commonMain.cvr", 792, 35),
      ))
    }

@delegate:ResourceContentHash(1_563_659_940)
internal val Res.string.luminance: StringResource by lazy {
      StringResource("string:luminance", "luminance", setOf(
        ResourceItem(setOf(), "${MD}values/strings.commonMain.cvr", 828, 29),
      ))
    }

@delegate:ResourceContentHash(31_574_105)
internal val Res.string.no_results_found: StringResource by lazy {
      StringResource("string:no_results_found", "no_results_found", setOf(
        ResourceItem(setOf(), "${MD}values/strings.commonMain.cvr", 858, 48),
      ))
    }

@delegate:ResourceContentHash(63_508_044)
internal val Res.string.none: StringResource by lazy {
      StringResource("string:none", "none", setOf(
        ResourceItem(setOf(), "${MD}values/strings.commonMain.cvr", 907, 20),
      ))
    }

@delegate:ResourceContentHash(2_554_120)
internal val Res.string.ok: StringResource by lazy {
      StringResource("string:ok", "ok", setOf(
        ResourceItem(setOf(), "${MD}values/strings.commonMain.cvr", 928, 14),
      ))
    }

@delegate:ResourceContentHash(1_398_224_184)
internal val Res.string.transparency: StringResource by lazy {
      StringResource("string:transparency", "transparency", setOf(
        ResourceItem(setOf(), "${MD}values/strings.commonMain.cvr", 943, 36),
      ))
    }

@InternalResourceApi
internal fun _collectCommonMainString0Resources(map: MutableMap<String, StringResource>) {
  map.put("action_bar_up_description", Res.string.action_bar_up_description)
  map.put("app_name", Res.string.app_name)
  map.put("cancel", Res.string.cancel)
  map.put("color", Res.string.color)
  map.put("color_wheel", Res.string.color_wheel)
  map.put("lorem_content", Res.string.lorem_content)
  map.put("lorem_title", Res.string.lorem_title)
  map.put("luminance", Res.string.luminance)
  map.put("no_results_found", Res.string.no_results_found)
  map.put("none", Res.string.none)
  map.put("ok", Res.string.ok)
  map.put("transparency", Res.string.transparency)
}
