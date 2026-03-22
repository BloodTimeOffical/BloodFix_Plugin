package com.bloodfix.bloodfix;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class BloodFix extends JavaPlugin implements Listener, CommandExecutor {

    private final Pattern hexPattern = Pattern.compile("&#[a-fA-F0-9]{6}");

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
        if (getCommand("bloodfix") != null) {
            getCommand("bloodfix").setExecutor(this);
        }
        getLogger().info("BloodFix запущен! (v1.3 | 1.16-1.21)");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onExpChange(PlayerExpChangeEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission("bloodfix.use")) return;

        int expAmount = event.getAmount();
        if (expAmount <= 0) return;

        List<ItemStack> mendingItems = getMendingItems(player);
        if (mendingItems.isEmpty()) return;

        Map<ItemStack, Double> weights = new HashMap<>();
        double totalWeight = 0;
        double power = getConfig().getDouble("priority-power", 2.5);

        for (ItemStack item : mendingItems) {
            ItemMeta meta = item.getItemMeta();
            if (meta instanceof Damageable dmg) {
                if (dmg.hasDamage()) {
                    // Рассчитываем приоритет на основе % поломки
                    double percent = (double) dmg.getDamage() / item.getType().getMaxDurability();
                    double weight = Math.pow(percent, power);
                    weights.put(item, weight);
                    totalWeight += weight;
                }
            }
        }

        if (totalWeight <= 0) return;

        int repairPotential = expAmount * 2; // 1 опыт = 2 прочности
        int usedExp = 0;

        for (Map.Entry<ItemStack, Double> entry : weights.entrySet()) {
            ItemStack item = entry.getKey();
            double share = entry.getValue() / totalWeight;
            int repairAmount = (int) (repairPotential * share);

            if (repairAmount > 0) {
                Damageable meta = (Damageable) item.getItemMeta();
                if (meta != null) {
                    int currentDmg = meta.getDamage();
                    int toRepair = Math.min(currentDmg, repairAmount);
                    meta.setDamage(currentDmg - toRepair);
                    item.setItemMeta(meta);
                    usedExp += (int) Math.ceil(toRepair / 2.0);
                }
            }
        }
        // Остаток опыта идет в уровень игрока
        event.setAmount(Math.max(0, expAmount - usedExp));
    }

    private List<ItemStack> getMendingItems(Player player) {
        List<ItemStack> items = new ArrayList<>();
        // Броня
        ItemStack[] armor = player.getInventory().getArmorContents();
        if (armor != null) {
            for (ItemStack i : armor) if (isMending(i)) items.add(i);
        }
        // Руки
        if (isMending(player.getInventory().getItemInMainHand())) items.add(player.getInventory().getItemInMainHand());
        if (isMending(player.getInventory().getItemInOffHand())) items.add(player.getInventory().getItemInOffHand());
        return items;
    }

    private boolean isMending(ItemStack item) {
        return item != null && item.getType() != Material.AIR && item.containsEnchantment(Enchantment.MENDING);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("bloodfix.admin")) {
                sender.sendMessage(translate(getConfig().getString("messages.no-permission")));
                return true;
            }
            reloadConfig();
            sender.sendMessage(translate(getConfig().getString("messages.reload-success")));
            return true;
        }
        return false;
    }

    private String translate(String message) {
        if (message == null) return "";
        Matcher matcher = hexPattern.matcher(message);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String color = message.substring(matcher.start(), matcher.end());
            matcher.appendReplacement(buffer, ChatColor.of(color.substring(1)).toString());
        }
        matcher.appendTail(buffer);
        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }
}