package github.gilbertokpl.essentialsk.util

import github.gilbertokpl.essentialsk.configs.GeneralLang
import github.gilbertokpl.essentialsk.configs.MainConfig
import github.gilbertokpl.essentialsk.data.KitData
import github.gilbertokpl.essentialsk.data.PlayerData
import github.gilbertokpl.essentialsk.manager.IInstance
import github.gilbertokpl.essentialsk.manager.InternalBukkitObjectInputStream
import github.gilbertokpl.essentialsk.manager.InternalBukkitObjectOutputStream
import org.apache.commons.lang3.exception.ExceptionUtils
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.util.io.BukkitObjectInputStream
import org.bukkit.util.io.BukkitObjectOutputStream
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class ItemUtil {

    fun pickupKit(p: Player, kit: String) {

        // check if player don't have permission
        if (!p.hasPermission("essentialsk.commands.kit.$kit")) {
            p.sendMessage(GeneralLang.getInstance().generalNotPerm)
            return
        }

        //load caches
        val kitCache = KitData(kit).getCache()
        val playerCache = PlayerData(p.name)

        //get all time of kit
        var timeAll = playerCache.getCache()?.let { it.kitsCache[kit] ?: 0L } ?: return
        timeAll += kitCache.time

        // if time is remaining
        if (timeAll >= System.currentTimeMillis() && !p.hasPermission("essentialsk.bypass.kitcatch")) {
            val remainingTime = timeAll - System.currentTimeMillis()
            p.sendMessage(
                GeneralLang.getInstance().kitsCatchMessage.replace(
                    "%time%",
                    PluginUtil.getInstance()
                        .convertMillisToString(remainingTime, MainConfig.getInstance().kitsUseShortTime)
                )
            )
            return
        }

        //send kit to player
        if (ItemUtil.getInstance().giveKit(
                p,
                kitCache.items,
                MainConfig.getInstance().kitsEquipArmorInCatch,
                MainConfig.getInstance().kitsDropItemsInCatch
            )
        ) {
            playerCache.setKitTime(kit, System.currentTimeMillis())
            p.sendMessage(GeneralLang.getInstance().kitsCatchSuccess.replace("%kit%", kitCache.fakeName))
        }
    }

    fun giveKit(p: Player, items: List<ItemStack>, armorAutoEquip: Boolean, drop: Boolean = false): Boolean {
        //bug armored check error
        val inv = p.inventory

        val itemsInternal = ArrayList<ItemStack>()

        val inventorySlotsUsed = inv.filterNotNull().size

        val armor = ArrayList<String>()

        val itemsArmorInternal = HashMap<String, ItemStack>()

        var inventorySpace = (Integer.valueOf(36).minus(inventorySlotsUsed))

        //check if player has space in Armor contents
        if (armorAutoEquip) {
            fun helper(to: ItemStack?, name: String) {
                if (to == null) {
                    armor.add(name)
                }
            }
            helper(inv.helmet, "HELMET")
            helper(inv.chestplate, "CHESTPLATE")
            helper(inv.leggings, "LEGGINGS")
            helper(inv.boots, "BOOTS")
        }

        //check if item is armor
        for (i in items) {
            if (armorAutoEquip) {
                var bolArmor = false
                val split = i.type.name.split("_")
                split.forEach {
                    if (it.contains("HELMET") || it.contains("CHESTPLATE") || it.contains("LEGGINGS") || it.contains("BOOTS")) {
                        if (armor.contains(it)) {
                            armor.remove(it)
                            itemsArmorInternal[it] = i
                            bolArmor = true
                        }
                    }
                }
                if (bolArmor) continue
            }
            itemsInternal.add(i)
        }

        // give armor parts
        fun giveArmour() {
            for (i in itemsArmorInternal) {
                if (i.key == "HELMET") {
                    inv.helmet = i.value
                    continue
                }
                if (i.key == "CHESTPLATE") {
                    inv.chestplate = i.value
                    continue
                }
                if (i.key == "LEGGINGS") {
                    inv.leggings = i.value
                    continue
                }
                if (i.key == "BOOTS") {
                    inv.boots = i.value
                    continue
                }
            }
        }

        //drop itens if full
        if (drop) {
            for (i in itemsInternal) {
                if (inventorySpace > 0) {
                    p.inventory.addItem(i)
                    inventorySpace -= 1
                    continue
                }
                p.world.dropItem(p.location, i)
            }
            giveArmour()
            return true
        }

        //check if inventory is full and add item
        if (inventorySpace >= itemsInternal.size) {
            for (i in itemsInternal) {
                p.inventory.addItem(i)
            }
            giveArmour()
            return true
        }

        //send message if inventory is full
        p.sendMessage(
            GeneralLang.getInstance().kitsCatchNoSpace.replace(
                "%slots%",
                (itemsInternal.size - inventorySpace).toString()
            )
        )
        return false
    }

    fun itemSerializer(items: List<ItemStack>): String {
        if (items.isEmpty()) return ""
        lateinit var toReturn: String
        try {
            val outputStream = ByteArrayOutputStream()
            val dataOutput = try {
                BukkitObjectOutputStream(outputStream)
            } catch (e: NoClassDefFoundError) {
                InternalBukkitObjectOutputStream(outputStream)
            }
            dataOutput.writeInt(items.size)
            for (i in items.indices) {
                dataOutput.writeObject(items[i])
            }
            dataOutput.close()
            toReturn = Base64Coder.encodeLines(outputStream.toByteArray())
        } catch (e: Exception) {
            FileLoggerUtil.getInstance().logError(ExceptionUtils.getStackTrace(e))
        }
        return toReturn
    }

    fun itemSerializer(data: String): List<ItemStack> {
        if (data == "") return emptyList()
        lateinit var toReturn: List<ItemStack>
        try {
            val inputStream = ByteArrayInputStream(Base64Coder.decodeLines(data))
            val dataInput = try {
                BukkitObjectInputStream(inputStream)
            } catch (e: NoClassDefFoundError) {
                InternalBukkitObjectInputStream(inputStream)
            }
            val items = arrayOfNulls<ItemStack>(dataInput.readInt())
            for (i in items.indices) {
                items[i] = dataInput.readObject() as ItemStack?
            }
            dataInput.close()
            toReturn = items.filterNotNull().toList()
        } catch (e: Exception) {
            FileLoggerUtil.getInstance().logError(ExceptionUtils.getStackTrace(e))
        }
        return toReturn
    }

    fun item(material: Material, name: String, lore: List<String>, effect: Boolean = false): ItemStack {
        val item = ItemStack(material)
        if (effect) {
            try {
                item.addUnsafeEnchantment(Enchantment.LUCK, 1)
            } catch (ignored: NoSuchFieldError) {
            }
        }
        val meta = item.itemMeta
        meta?.lore = lore
        meta?.setDisplayName(name)
        if (effect) {
            try {
                meta?.addItemFlags(ItemFlag.HIDE_ENCHANTS)
            } catch (ignored: NoClassDefFoundError) {
            }
        }
        item.itemMeta = meta
        return item
    }

    fun item(material: Material, name: String, effect: Boolean = false): ItemStack {
        val item = ItemStack(material)
        if (effect) {
            try {
                item.addUnsafeEnchantment(Enchantment.LUCK, 1)
            } catch (ignored: NoSuchFieldError) {
            }
        }
        val meta = item.itemMeta
        meta?.setDisplayName(name)
        if (effect) {
            try {
                meta?.addItemFlags(ItemFlag.HIDE_ENCHANTS)
            } catch (ignored: NoClassDefFoundError) {
            }
        }
        item.itemMeta = meta
        return item
    }

    fun item(material: Material, name: String, quant: Int): ItemStack {
        val item = ItemStack(material, quant)
        val meta = item.itemMeta
        meta?.setDisplayName(name)
        item.itemMeta = meta
        return item
    }

    fun item(material: Material, quant: Int): ItemStack {
        return ItemStack(material, quant)
    }

    fun item(material: Material): ItemStack {
        return ItemStack(material)
    }

    companion object : IInstance<ItemUtil> {
        private val instance = createInstance()
        override fun createInstance(): ItemUtil = ItemUtil()
        override fun getInstance(): ItemUtil {
            return instance
        }
    }
}