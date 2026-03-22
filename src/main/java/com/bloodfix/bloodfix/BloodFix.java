package com.bloodfix.bloodfix;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public final class BloodFix extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("BloodFix активирован! Приоритет на самую сломанную броню включен.");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onExpChange(PlayerExpChangeEvent event) {
        Player player = event.getPlayer();
        int amount = event.getAmount();

        if (amount <= 0) return;

        // Шанс из конфига (например, 80%)
        double priorityChance = getConfig().getDouble("priority-chance-percent", 80.0) / 100.0;

        if (Math.random() <= priorityChance) {
            ItemStack mostDamaged = getMostDamagedItem(player);

            if (mostDamaged != null) {
                // 1 единица опыта чинит 2 единицы прочности
                int repairAmount = amount * 2;

                Damageable meta = (Damageable) mostDamaged.getItemMeta();
                if (meta != null) {
                    int currentDamage = meta.getDamage();
                    int newDamage = Math.max(0, currentDamage - repairAmount);

                    // Вычисляем, сколько опыта реально потрачено на починку
                    int usedExp = (currentDamage - newDamage) / 2;

                    meta.setDamage(newDamage);
                    mostDamaged.setItemMeta(meta);

                    // Вычитаем потраченный опыт, чтобы он не шел в полоску уровня
                    event.setAmount(Math.max(0, amount - usedExp));
                }
            }
        }
    }

    private ItemStack getMostDamagedItem(Player player) {
        ItemStack target = null;
        double maxDamagePercent = 0;

        List<ItemStack> items = new ArrayList<>();
        // Проверяем броню
        ItemStack[] armor = player.getInventory().getArmorContents();
        if (armor != null) {
            for (ItemStack piece : armor) {
                if (piece != null && piece.getType() != Material.AIR) items.add(piece);
            }
        }
        // Проверяем руки (инструменты)
        items.add(player.getInventory().getItemInMainHand());
        items.add(player.getInventory().getItemInOffHand());

        for (ItemStack item : items) {
            if (item == null || !item.containsEnchantment(Enchantment.MENDING)) continue;

            if (item.getItemMeta() instanceof Damageable meta) {
                if (meta.hasDamage()) {
                    // Считаем процент поломки
                    double percent = (double) meta.getDamage() / item.getType().getMaxDurability();
                    if (percent > maxDamagePercent) {
                        maxDamagePercent = percent;
                        target = item;
                    }
                }
            }
        }
        return target;
    }
}