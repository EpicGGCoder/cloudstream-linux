@file:OptIn(InternalResourceApi::class)

package com.lagradost.cloudstream4.shared.generated.resources

import kotlin.OptIn
import kotlin.String
import kotlin.collections.MutableMap
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.InternalResourceApi
import org.jetbrains.compose.resources.ResourceContentHash
import org.jetbrains.compose.resources.ResourceItem

private const val MD: String =
    "composeResources/com.lagradost.cloudstream4.shared.generated.resources/"

@delegate:ResourceContentHash(-1_150_353_540)
internal val Res.drawable.arrow_back: DrawableResource by lazy {
      DrawableResource("drawable:arrow_back", setOf(
        ResourceItem(setOf(), "${MD}drawable/arrow_back.xml", -1, -1),
      ))
    }

@delegate:ResourceContentHash(-828_137_309)
internal val Res.drawable.arrow_downward: DrawableResource by lazy {
      DrawableResource("drawable:arrow_downward", setOf(
        ResourceItem(setOf(), "${MD}drawable/arrow_downward.xml", -1, -1),
      ))
    }

@delegate:ResourceContentHash(2_140_871_578)
internal val Res.drawable.arrow_upward: DrawableResource by lazy {
      DrawableResource("drawable:arrow_upward", setOf(
        ResourceItem(setOf(), "${MD}drawable/arrow_upward.xml", -1, -1),
      ))
    }

@delegate:ResourceContentHash(284_235_162)
internal val Res.drawable.cancel: DrawableResource by lazy {
      DrawableResource("drawable:cancel", setOf(
        ResourceItem(setOf(), "${MD}drawable/cancel.xml", -1, -1),
      ))
    }

@delegate:ResourceContentHash(1_724_987_134)
internal val Res.drawable.check: DrawableResource by lazy {
      DrawableResource("drawable:check", setOf(
        ResourceItem(setOf(), "${MD}drawable/check.xml", -1, -1),
      ))
    }

@delegate:ResourceContentHash(818_685_633)
internal val Res.drawable.close: DrawableResource by lazy {
      DrawableResource("drawable:close", setOf(
        ResourceItem(setOf(), "${MD}drawable/close.xml", -1, -1),
      ))
    }

@delegate:ResourceContentHash(977_531_065)
internal val Res.drawable.default_icon: DrawableResource by lazy {
      DrawableResource("drawable:default_icon", setOf(
        ResourceItem(setOf(), "${MD}drawable/default_icon.png", -1, -1),
      ))
    }

@delegate:ResourceContentHash(-1_025_693_816)
internal val Res.drawable.error: DrawableResource by lazy {
      DrawableResource("drawable:error", setOf(
        ResourceItem(setOf(), "${MD}drawable/error.xml", -1, -1),
      ))
    }

@delegate:ResourceContentHash(1_854_316_322)
internal val Res.drawable.info: DrawableResource by lazy {
      DrawableResource("drawable:info", setOf(
        ResourceItem(setOf(), "${MD}drawable/info.xml", -1, -1),
      ))
    }

@delegate:ResourceContentHash(1_739_581_876)
internal val Res.drawable.preview: DrawableResource by lazy {
      DrawableResource("drawable:preview", setOf(
        ResourceItem(setOf(), "${MD}drawable/preview.xml", -1, -1),
      ))
    }

@InternalResourceApi
internal fun _collectCommonMainDrawable0Resources(map: MutableMap<String, DrawableResource>) {
  map.put("arrow_back", Res.drawable.arrow_back)
  map.put("arrow_downward", Res.drawable.arrow_downward)
  map.put("arrow_upward", Res.drawable.arrow_upward)
  map.put("cancel", Res.drawable.cancel)
  map.put("check", Res.drawable.check)
  map.put("close", Res.drawable.close)
  map.put("default_icon", Res.drawable.default_icon)
  map.put("error", Res.drawable.error)
  map.put("info", Res.drawable.info)
  map.put("preview", Res.drawable.preview)
}
