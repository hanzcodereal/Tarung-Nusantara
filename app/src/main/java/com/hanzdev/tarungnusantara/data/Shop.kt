package com.hanzdev.tarungnusantara.data

import com.hanzdev.tarungnusantara.game.FighterDef
import com.hanzdev.tarungnusantara.game.Headgear
import com.hanzdev.tarungnusantara.game.Look
import com.hanzdev.tarungnusantara.game.Outfit
import com.hanzdev.tarungnusantara.game.Upgrades

class ShopItem(
    val id: String,
    val slot: String,
    val nama: String,
    val harga: Int,
    val deskripsi: String,
    val glove: Int = 0,
    val gloveStyle: Int = 0,
    val head: Headgear = Headgear.NONE,
    val outfit: Outfit = Outfit.PLAIN,
    val c1: Int = 0,
    val c2: Int = 0
)

class JamuDef(val id: String, val nama: String, val harga: Int, val efek: String)

class UpgradeDef(val id: String, val nama: String, val efek: String)

sealed class BuyResult {
    object Ok : BuyResult()
    object NotEnough : BuyResult()
    object Already : BuyResult()
    object MaxLevel : BuyResult()
}

object Shop {
    private fun c(h: Long): Int = h.toInt()

    val gloves = listOf(
        ShopItem("glove_merah", "glove", "Merah Klasik", 0, "Sarung tinju merah polos. Gratis.", c(0xFFC2261D), 0),
        ShopItem("glove_biru", "glove", "Biru Samudra", 200, "Biru laut dalam, warna nelayan Nusantara.", c(0xFF1F5FA8), 0),
        ShopItem("glove_emas", "glove", "Emas Nusantara", 800, "Berkilau emas dengan sorot cahaya.", c(0xFFD9A21B), 1),
        ShopItem("glove_parang", "glove", "Batik Parang", 1200, "Motif parang, pola ombak batik klasik.", c(0xFF8A2420), 2)
    )
    val heads = listOf(
        ShopItem("head_udeng", "head", "Udeng Bali", 300, "Ikat kepala kain khas Bali.", head = Headgear.UDENG, c1 = c(0xFFF3E6CB), c2 = c(0xFFE3A72F)),
        ShopItem("head_blangkon", "head", "Blangkon Jawa", 300, "Penutup kepala batik khas Jawa.", head = Headgear.BLANGKON, c1 = c(0xFF6B3E22), c2 = c(0xFFD8B26B)),
        ShopItem("head_dayak", "head", "Ikat Kepala Dayak", 400, "Ikat kepala bermanik dengan bulu.", head = Headgear.DAYAK, c1 = c(0xFF8A2420), c2 = c(0xFFE3A72F)),
        ShopItem("head_siger", "head", "Siger Lampung", 500, "Mahkota emas bergerigi khas Lampung.", head = Headgear.SIGER, c1 = c(0xFFE3A72F), c2 = c(0xFFB8281F))
    )
    val costumes = listOf(
        ShopItem("cos_batik", "cos", "Celana Batik", 600, "Celana motif batik sogan.", outfit = Outfit.BATIK, c1 = c(0xFF6B3E22), c2 = c(0xFFD8B26B)),
        ShopItem("cos_sarung", "cos", "Sarung Bugis", 700, "Sarung kotak khas Bugis.", outfit = Outfit.SARUNG, c1 = c(0xFF8A2B2B), c2 = c(0xFFE3A72F)),
        ShopItem("cos_ulos", "cos", "Rompi Ulos", 800, "Rompi kain ulos Batak.", outfit = Outfit.ULOS, c1 = c(0xFF8A2420), c2 = c(0xFFF3E6CB)),
        ShopItem("cos_betang", "cos", "Cawat Betang", 900, "Celana adat bermotif Dayak.", outfit = Outfit.BETANG, c1 = c(0xFF171717), c2 = c(0xFFE3A72F))
    )
    val allItems: List<ShopItem> = gloves + heads + costumes

    val upgrades = listOf(
        UpgradeDef("hp", "HP", "+5 HP per level"),
        UpgradeDef("stamina", "Stamina", "+5 stamina per level"),
        UpgradeDef("power", "Kekuatan", "+1 kekuatan per level"),
        UpgradeDef("speed", "Kecepatan", "+1 kecepatan per level")
    )
    val upgradePrices = intArrayOf(150, 300, 500, 800, 1200)

    val jamu = listOf(
        JamuDef("kunyit", "Jamu Kunyit", 100, "Memulihkan sedikit HP di awal ronde"),
        JamuDef("kencur", "Jamu Beras Kencur", 150, "Stamina pulih lebih cepat"),
        JamuDef("temulawak", "Jamu Temulawak", 200, "Meteran jurus mulai dari 30%")
    )

    fun item(id: String): ShopItem? = allItems.firstOrNull { it.id == id }

    fun buy(item: ShopItem): BuyResult {
        if (Save.owns(item.id)) return BuyResult.Already
        if (Save.coins < item.harga) return BuyResult.NotEnough
        Save.coins -= item.harga
        Save.addOwned(item.id)
        return BuyResult.Ok
    }

    fun buyUpgrade(id: String): BuyResult {
        val lv = Save.upgrade(id)
        if (lv >= 5) return BuyResult.MaxLevel
        val price = upgradePrices[lv]
        if (Save.coins < price) return BuyResult.NotEnough
        Save.coins -= price
        Save.setUpgrade(id, lv + 1)
        return BuyResult.Ok
    }

    fun buyJamu(j: JamuDef): BuyResult {
        if (Save.coins < j.harga) return BuyResult.NotEnough
        Save.coins -= j.harga
        Save.setJamu(j.id, Save.jamu(j.id) + 1)
        return BuyResult.Ok
    }

    fun playerUpgrades(): Upgrades =
        Upgrades(Save.upgrade("hp"), Save.upgrade("stamina"), Save.upgrade("power"), Save.upgrade("speed"))

    /** Penampilan dari id item yang dipilih. Id kosong atau tidak dikenal berarti tampilan bawaan. */
    fun lookWith(def: FighterDef, gloveId: String, headId: String, cosId: String): Pair<Look, Int> {
        var look = def.look
        var style = 0
        item(gloveId)?.let { look = look.copy(glove = it.glove); style = it.gloveStyle }
        item(headId)?.let { look = look.copy(head = it.head, headC1 = it.c1, headC2 = it.c2) }
        item(cosId)?.let { look = look.copy(outfit = it.outfit, cloth1 = it.c1, cloth2 = it.c2, sleeves = false) }
        return Pair(look, style)
    }

    private fun ownedEquip(fighterId: String, slot: String): String {
        val id = Save.equipped(fighterId, slot)
        return if (id.isNotEmpty() && Save.owns(id)) id else ""
    }

    fun equipIds(fighterId: String): Array<String> =
        arrayOf(ownedEquip(fighterId, "glove"), ownedEquip(fighterId, "head"), ownedEquip(fighterId, "cos"))

    /** Penampilan petarung dengan pilihan garasi pemain. */
    fun lookFor(def: FighterDef): Pair<Look, Int> {
        val ids = equipIds(def.id)
        return lookWith(def, ids[0], ids[1], ids[2])
    }
}
