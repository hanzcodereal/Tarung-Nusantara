package com.hanzdev.tarungnusantara.ui

import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import com.hanzdev.tarungnusantara.audio.Sound
import com.hanzdev.tarungnusantara.data.BuyResult
import com.hanzdev.tarungnusantara.data.Save
import com.hanzdev.tarungnusantara.data.Shop
import com.hanzdev.tarungnusantara.data.ShopItem
import com.hanzdev.tarungnusantara.data.Teks
import com.hanzdev.tarungnusantara.game.Pal
import com.hanzdev.tarungnusantara.game.Roster

class ShopScreen(private val tab: Int = 0, private var fighterId: String = currentPlayer().id) : Screen() {
    private val tabs = listOf("Sarung Tinju", "Aksesori", "Kostum", "Upgrade", "Jamu")

    private fun again(t: Int = tab) = ShopScreen(t, fighterId)

    override fun build(ctx: Context): View {
        val root = LinearLayout(ctx)
        root.orientation = LinearLayout.HORIZONTAL
        val def = Roster.byId(fighterId)

        val left = Ui.panel(ctx, 10)
        left.gravity = Gravity.CENTER_HORIZONTAL
        left.addView(fighterView(ctx, def, false, true), Ui.lp(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        left.addView(Ui.text(ctx, def.nama, 15f, Pal.KUNYIT, 2).also { it.gravity = Gravity.CENTER })
        left.addView(Ui.text(ctx, "Pilihan garasi ini berlaku untuk ${Roster.shortName(def)}", 10.5f, Pal.KREM, 0).also { it.gravity = Gravity.CENTER })
        left.addView(Ui.spacer(ctx, 1, 6))
        val nav2 = LinearLayout(ctx)
        nav2.orientation = LinearLayout.HORIZONTAL
        val unlocked = Roster.all.filter { Save.isUnlocked(it.id) }
        val idx = unlocked.indexOfFirst { it.id == fighterId }.coerceAtLeast(0)
        fun go(step: Int) {
            fighterId = unlocked[(idx + step + unlocked.size) % unlocked.size].id
            nav.replace(again())
        }
        nav2.addView(Ui.button(ctx, "Sebelumnya", Kind.GHOST) { go(-1) }, Ui.lp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        nav2.addView(Ui.button(ctx, "Berikutnya", Kind.GHOST) { go(1) }, Ui.lp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        left.addView(nav2)
        root.addView(left, Ui.lp(0, ViewGroup.LayoutParams.MATCH_PARENT, 0.75f))
        root.addView(Ui.spacer(ctx, 10, 1))

        val right = LinearLayout(ctx)
        right.orientation = LinearLayout.VERTICAL
        right.addView(Widgets.choice(ctx, tabs, tab) { nav.replace(again(it)) })
        right.addView(Ui.spacer(ctx, 1, 8))
        val list = LinearLayout(ctx)
        list.orientation = LinearLayout.VERTICAL
        when (tab) {
            0 -> Shop.gloves.forEach { list.addView(itemCard(ctx, it)) }
            1 -> Shop.heads.forEach { list.addView(itemCard(ctx, it)) }
            2 -> Shop.costumes.forEach { list.addView(itemCard(ctx, it)) }
            3 -> upgradeList(ctx, list)
            else -> jamuList(ctx, list)
        }
        val sv = ScrollView(ctx)
        sv.addView(list)
        right.addView(sv, Ui.lp(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        root.addView(right, Ui.lp(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.5f))
        return Ui.frame(ctx, "Toko", { nav.pop() }, true, root)
    }

    private fun cardLp(): LinearLayout.LayoutParams {
        val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        lp.bottomMargin = Ui.dp(8)
        return lp
    }

    private fun itemCard(ctx: Context, item: ShopItem): View {
        val card = Ui.panel(ctx, 12)
        val owned = Save.owns(item.id)
        val equipped = Save.equipped(fighterId, item.slot) == item.id
        val row = LinearLayout(ctx)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        val info = LinearLayout(ctx)
        info.orientation = LinearLayout.VERTICAL
        info.addView(Ui.text(ctx, item.nama, 16f, Pal.KUNYIT, 2))
        info.addView(Ui.text(ctx, item.deskripsi, 12f, Pal.KREM, 0))
        info.addView(Ui.text(ctx, if (item.harga == 0) "Gratis" else if (owned) "Sudah dimiliki" else "Harga ${Ui.formatNumber(item.harga)} koin", 11.5f, Pal.NILA_TERANG, 1))
        row.addView(info, Ui.lp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        val action = when {
            !owned -> Ui.button(ctx, "Beli ${Ui.formatNumber(item.harga)}", Kind.PRIMARY) {
                when (Shop.buy(item)) {
                    BuyResult.Ok -> { Sound.play("coin"); nav.toast("${item.nama} dibeli."); nav.replace(again()) }
                    BuyResult.NotEnough -> nav.toast(Teks.KOIN_KURANG)
                    else -> {}
                }
            }
            equipped -> Ui.button(ctx, "Lepas", Kind.SECONDARY) { Save.equip(fighterId, item.slot, ""); nav.replace(again()) }
            else -> Ui.button(ctx, "Pakai", Kind.SECONDARY) { Save.equip(fighterId, item.slot, item.id); nav.replace(again()) }
        }
        row.addView(action)
        card.addView(row)
        if (equipped) card.addView(Ui.text(ctx, "Sedang dipakai oleh ${Roster.shortName(Roster.byId(fighterId))}", 11f, Pal.KUNYIT, 1))
        card.layoutParams = cardLp()
        return card
    }

    private fun upgradeList(ctx: Context, list: LinearLayout) {
        list.addView(Ui.text(ctx, "Upgrade berlaku untuk semua petarungmu. Tidak berlaku di mode Bluetooth agar adil.", 12f, Pal.KREM, 0))
        list.addView(Ui.spacer(ctx, 1, 8))
        for (u in Shop.upgrades) {
            val lv = Save.upgrade(u.id)
            val card = Ui.panel(ctx, 12)
            val row = LinearLayout(ctx)
            row.orientation = LinearLayout.HORIZONTAL
            row.gravity = Gravity.CENTER_VERTICAL
            val info = LinearLayout(ctx)
            info.orientation = LinearLayout.VERTICAL
            info.addView(Ui.text(ctx, "${u.nama}  (level $lv dari 5)", 16f, Pal.KUNYIT, 2))
            info.addView(Ui.text(ctx, u.efek, 12f, Pal.KREM, 0))
            info.addView(SegBar(ctx, lv, 5), Ui.lp(Ui.dp(140), Ui.dp(10)).also { it.topMargin = Ui.dp(6) })
            row.addView(info, Ui.lp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            val btn = if (lv >= 5) Ui.button(ctx, "Maksimal", Kind.SECONDARY, false) {}
            else Ui.button(ctx, "Naik ${Ui.formatNumber(Shop.upgradePrices[lv])}", Kind.PRIMARY) {
                when (Shop.buyUpgrade(u.id)) {
                    BuyResult.Ok -> { Sound.play("coin"); nav.replace(again()) }
                    BuyResult.NotEnough -> nav.toast(Teks.KOIN_KURANG)
                    else -> {}
                }
            }
            row.addView(btn)
            card.addView(row)
            card.layoutParams = cardLp()
            list.addView(card)
        }
    }

    private fun jamuList(ctx: Context, list: LinearLayout) {
        list.addView(Ui.text(ctx, "Pilih jamu sebelum pertandingan di layar Siap Bertanding.", 12f, Pal.KREM, 0))
        list.addView(Ui.spacer(ctx, 1, 8))
        for (j in Shop.jamu) {
            val card = Ui.panel(ctx, 12)
            val row = LinearLayout(ctx)
            row.orientation = LinearLayout.HORIZONTAL
            row.gravity = Gravity.CENTER_VERTICAL
            val info = LinearLayout(ctx)
            info.orientation = LinearLayout.VERTICAL
            info.addView(Ui.text(ctx, j.nama, 16f, Pal.KUNYIT, 2))
            info.addView(Ui.text(ctx, j.efek, 12f, Pal.KREM, 0))
            info.addView(Ui.text(ctx, "Punya: ${Save.jamu(j.id)}", 11.5f, Pal.NILA_TERANG, 1))
            row.addView(info, Ui.lp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            row.addView(Ui.button(ctx, "Beli ${j.harga}", Kind.PRIMARY) {
                when (Shop.buyJamu(j)) {
                    BuyResult.Ok -> { Sound.play("coin"); nav.replace(again()) }
                    BuyResult.NotEnough -> nav.toast(Teks.KOIN_KURANG)
                    else -> {}
                }
            })
            card.addView(row)
            card.layoutParams = cardLp()
            list.addView(card)
        }
    }
}
