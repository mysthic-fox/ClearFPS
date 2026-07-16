package br.com.serpensmc.clearfps;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.WeatherType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class ClearFpsPlugin extends JavaPlugin implements Listener, CommandExecutor {

    private enum Profile { NONE, LOW, MEDIUM, AFK, POTATO, RECORDING }

    private final Map<UUID, Profile> activeProfiles = new HashMap<>();
    private final Set<UUID> nightVisionActive = new HashSet<>();
    private final Set<UUID> eternalDayActive = new HashSet<>();
    private final Set<UUID> hideDecorationsActive = new HashSet<>();
    private final Set<UUID> hidePlayersActive = new HashSet<>();
    private final Set<UUID> lowRenderDistanceActive = new HashSet<>();
    private ItemStack backgroundGlass;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        backgroundGlass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = backgroundGlass.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.empty());
            backgroundGlass.setItemMeta(meta);
        }
        getServer().getPluginManager().registerEvents(this, this);
        if (getCommand("fps") != null) {
            getCommand("fps").setExecutor(this);
        }
        
        Bukkit.getScheduler().runTaskTimer(this, this::updateVisibilityForAll, 100L, 100L);
    }

    @Override
    public void onDisable() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            resetPlayer(p);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        openGUI((Player) sender);
        return true;
    }

    private void openGUI(Player p) {
        UUID uuid = p.getUniqueId();
        Profile current = activeProfiles.getOrDefault(uuid, Profile.NONE);

        Inventory inv = Bukkit.createInventory(null, 54, translate("&5&lClearFps &8- &7Otimização Máxima"));
        
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, backgroundGlass);
        }

        inv.setItem(10, createItem(Material.GREEN_DYE, "&a&lPC Fraco", current == Profile.LOW,
            "&7---------------------------------------",
            "&fConfiguração básica para hardwares de entrada.",
            "&fOculta entidades não essenciais e reduz a",
            "&fdistância de renderização para 4 chunks.",
            "&7---------------------------------------",
            (current == Profile.LOW ? "&a● ATIVADO SEU PERFIL" : "&e» Clique para aplicar o perfil")));
            
        inv.setItem(11, createItem(Material.ORANGE_DYE, "&6&lPC Mediano", current == Profile.MEDIUM,
            "&7---------------------------------------",
            "&fConfiguração equilibrada.",
            "&fOculta monstros e drops.",
            "&fRenderização limitada a 6 chunks.",
            "&7---------------------------------------",
            (current == Profile.MEDIUM ? "&a● ATIVADO SEU PERFIL" : "&e» Clique para aplicar o perfil")));
            
        inv.setItem(12, createItem(Material.RED_DYE, "&c&lAfk", current == Profile.AFK,
            "&7---------------------------------------",
            "&fInatividade total do cliente.",
            "&fAplica cegueira, isola o jogador e",
            "&frenderiza apenas 2 chunks nativos.",
            "&7---------------------------------------",
            (current == Profile.AFK ? "&a● ATIVADO SEU PERFIL" : "&e» Clique para aplicar o perfil")));
            
        inv.setItem(13, createItem(Material.BROWN_DYE, "&6&lUltra Batata", current == Profile.POTATO,
            "&7---------------------------------------",
            "&fOtimização Agressiva Absoluta.",
            "&fForça clima limpo, visão noturna,",
            "&foculta TODOS os jogadores e entidades,",
            "&fe trava a renderização no mínimo (2).",
            "&7---------------------------------------",
            (current == Profile.POTATO ? "&a● ATIVADO SEU PERFIL" : "&e» Clique para aplicar o perfil")));

        inv.setItem(14, createItem(Material.PURPLE_DYE, "&5&lGravação", current == Profile.RECORDING,
            "&7---------------------------------------",
            "&fFidelidade visual extrema.",
            "&fNível zero de culling de entidades.",
            "&7---------------------------------------",
            (current == Profile.RECORDING ? "&a● ATIVADO SEU PERFIL" : "&e» Clique para aplicar o perfil")));
            
        inv.setItem(16, createItem(Material.BARRIER, "&c&lDesativar Modos", false,
            "&7---------------------------------------",
            "&fRestaura completamente as configurações",
            "&fglobais de renderização do servidor.",
            "&7---------------------------------------",
            "&e» Clique para limpar perfis"));

        inv.setItem(28, createItem(Material.GOLDEN_CARROT, "&e&lVisão Noturna Permanente", nightVisionActive.contains(uuid),
            "&fRemove processamento de sombras dinâmicas.",
            "&7---------------------------------------",
            (nightVisionActive.contains(uuid) ? "&a● ATIVADO" : "&c○ DESATIVADO")));

        inv.setItem(29, createItem(Material.SUNFLOWER, "&b&lFixar Dia & Tempo Limpo", eternalDayActive.contains(uuid),
            "&fElimina partículas de chuva e tranca o clima.",
            "&7---------------------------------------",
            (eternalDayActive.contains(uuid) ? "&a● ATIVADO" : "&c○ DESATIVADO")));

        inv.setItem(30, createItem(Material.ITEM_FRAME, "&d&lOcultar Decorações (FPS+)", hideDecorationsActive.contains(uuid),
            "&fEsconde Armor Stands, Molduras e Pinturas.",
            "&7---------------------------------------",
            (hideDecorationsActive.contains(uuid) ? "&a● ATIVADO" : "&c○ DESATIVADO")));

        inv.setItem(31, createItem(Material.ENDER_PEARL, "&3&lOcultar Jogadores (FPS++)", hidePlayersActive.contains(uuid),
            "&fTorna todos os outros jogadores invisíveis.",
            "&fGanho massivo de FPS em áreas lotadas.",
            "&7---------------------------------------",
            (hidePlayersActive.contains(uuid) ? "&a● ATIVADO" : "&c○ DESATIVADO")));
            
        inv.setItem(32, createItem(Material.SPYGLASS, "&9&lForçar Renderização Mínima", lowRenderDistanceActive.contains(uuid),
            "&fSobrescreve a renderização do seu cliente",
            "&fpara carregar apenas 2 chunks em volta.",
            "&7---------------------------------------",
            (lowRenderDistanceActive.contains(uuid) ? "&a● ATIVADO" : "&c○ DESATIVADO")));

        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(p);
            meta.displayName(translate("&e&lEstatísticas Globais de Uso"));
            meta.lore(Arrays.asList(
                translate("&7Acompanhamento estatístico da rede:"),
                Component.empty(),
                translate("&7PC Fraco: &f" + getConfig().getInt("stats.low", 0) + " vezes"),
                translate("&7PC Mediano: &f" + getConfig().getInt("stats.medium", 0) + " vezes"),
                translate("&7Perfil AFK: &f" + getConfig().getInt("stats.afk", 0) + " vezes"),
                translate("&7Ultra Batata: &f" + getConfig().getInt("stats.potato", 0) + " vezes"),
                translate("&7Modo Gravação: &f" + getConfig().getInt("stats.recording", 0) + " vezes")
            ));
            head.setItemMeta(meta);
        }
        inv.setItem(34, head);

        p.openInventory(inv);
    }

    private ItemStack createItem(Material mat, String name, boolean glow, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(translate(name));
            List<Component> components = new ArrayList<>();
            for (String line : lore) {
                components.add(translate(line));
            }
            meta.lore(components);
            if (glow) meta.setEnchantmentGlintOverride(true);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    private Component translate(String text) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!e.getView().title().equals(translate("&5&lClearFps &8- &7Otimização Máxima"))) return;
        e.setCancelled(true);
        
        ItemStack clickedItem = e.getCurrentItem();
        if (clickedItem == null) return;
        Material clicked = clickedItem.getType();
        if (clicked == Material.BLACK_STAINED_GLASS_PANE || clicked == Material.AIR || clicked == Material.PLAYER_HEAD) return;

        Player p = (Player) e.getWhoClicked();
        UUID uuid = p.getUniqueId();
        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);

        boolean profileChanged = false;

        if (clicked == Material.GREEN_DYE) {
            activeProfiles.put(uuid, Profile.LOW); incrementStat("low"); profileChanged = true;
        } else if (clicked == Material.ORANGE_DYE) {
            activeProfiles.put(uuid, Profile.MEDIUM); incrementStat("medium"); profileChanged = true;
        } else if (clicked == Material.RED_DYE) {
            activeProfiles.put(uuid, Profile.AFK); incrementStat("afk"); profileChanged = true;
        } else if (clicked == Material.BROWN_DYE) {
            activeProfiles.put(uuid, Profile.POTATO); incrementStat("potato"); profileChanged = true;
        } else if (clicked == Material.PURPLE_DYE) {
            activeProfiles.put(uuid, Profile.RECORDING); incrementStat("recording"); profileChanged = true;
        } else if (clicked == Material.BARRIER) {
            activeProfiles.remove(uuid); profileChanged = true; resetPlayer(p);
        } else if (clicked == Material.GOLDEN_CARROT) {
            toggleSet(nightVisionActive, uuid); applyToggles(p);
        } else if (clicked == Material.SUNFLOWER) {
            toggleSet(eternalDayActive, uuid); applyToggles(p);
        } else if (clicked == Material.ITEM_FRAME) {
            toggleSet(hideDecorationsActive, uuid); profileChanged = true;
        } else if (clicked == Material.ENDER_PEARL) {
            toggleSet(hidePlayersActive, uuid); profileChanged = true;
        } else if (clicked == Material.SPYGLASS) {
            toggleSet(lowRenderDistanceActive, uuid); applyToggles(p);
        }

        if (profileChanged) {
            applyProfileEffects(p);
            updateVisibilityForPlayer(p);
        }

        openGUI(p);
    }

    private void toggleSet(Set<UUID> set, UUID uuid) {
        if (!set.add(uuid)) set.remove(uuid);
    }

    private void resetPlayer(Player p) {
        UUID uuid = p.getUniqueId();
        nightVisionActive.remove(uuid);
        eternalDayActive.remove(uuid);
        hideDecorationsActive.remove(uuid);
        hidePlayersActive.remove(uuid);
        lowRenderDistanceActive.remove(uuid);
        
        p.removePotionEffect(PotionEffectType.NIGHT_VISION);
        p.removePotionEffect(PotionEffectType.BLINDNESS);
        p.resetPlayerTime();
        p.resetPlayerWeather();
        p.setSendViewDistance(Bukkit.getServer().getViewDistance());
        
        for (Entity entity : p.getWorld().getEntities()) {
            p.showEntity(this, entity);
        }
        for (Player target : Bukkit.getOnlinePlayers()) {
            p.showPlayer(this, target);
        }
    }

    private void applyToggles(Player p) {
        UUID uuid = p.getUniqueId();
        Profile prof = activeProfiles.getOrDefault(uuid, Profile.NONE);
        
        if (nightVisionActive.contains(uuid) || prof == Profile.POTATO) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, false, false));
        } else {
            p.removePotionEffect(PotionEffectType.NIGHT_VISION);
        }

        if (eternalDayActive.contains(uuid) || prof == Profile.POTATO) {
            p.setPlayerTime(6000, false);
            p.setPlayerWeather(WeatherType.CLEAR);
        } else {
            p.resetPlayerTime();
            p.resetPlayerWeather();
        }

        if (lowRenderDistanceActive.contains(uuid) || prof == Profile.POTATO || prof == Profile.AFK) {
            p.setSendViewDistance(2);
        } else if (prof == Profile.LOW) {
            p.setSendViewDistance(4);
        } else if (prof == Profile.MEDIUM) {
            p.setSendViewDistance(6);
        } else {
            p.setSendViewDistance(Bukkit.getServer().getViewDistance());
        }
    }

    private void applyProfileEffects(Player p) {
        Profile prof = activeProfiles.getOrDefault(p.getUniqueId(), Profile.NONE);
        if (prof == Profile.AFK) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Integer.MAX_VALUE, 1, false, false));
        } else {
            p.removePotionEffect(PotionEffectType.BLINDNESS);
        }
        applyToggles(p);
    }

    private void updateVisibilityForAll() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            updateVisibilityForPlayer(p);
        }
    }

    private void updateVisibilityForPlayer(Player p) {
        UUID uuid = p.getUniqueId();
        Profile prof = activeProfiles.getOrDefault(uuid, Profile.NONE);
        boolean hideDeco = hideDecorationsActive.contains(uuid) || prof == Profile.POTATO || prof == Profile.LOW;
        boolean hidePlayers = hidePlayersActive.contains(uuid) || prof == Profile.POTATO || prof == Profile.AFK;

        for (Player target : Bukkit.getOnlinePlayers()) {
            if (target.equals(p)) continue;
            if (hidePlayers && !target.hasPermission("clearfps.admin")) {
                p.hidePlayer(this, target);
            } else {
                p.showPlayer(this, target);
            }
        }

        for (Entity entity : p.getWorld().getEntities()) {
            if (entity instanceof Player) continue;
            
            boolean hide = false;
            if (prof == Profile.POTATO || prof == Profile.AFK) {
                hide = true;
            } else if (prof == Profile.LOW && !(entity instanceof Animals)) {
                hide = true;
            } else if (prof == Profile.MEDIUM && (entity instanceof Monster || entity instanceof Item)) {
                hide = true;
            }

            if (!hide && hideDeco && (entity instanceof ArmorStand || entity instanceof ItemFrame || entity instanceof Painting)) {
                hide = true;
            }

            if (hide) {
                p.hideEntity(this, entity);
            } else {
                p.showEntity(this, entity);
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        updateVisibilityForAll();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        activeProfiles.remove(e.getPlayer().getUniqueId());
        resetPlayer(e.getPlayer());
    }

    private void incrementStat(String key) {
        int current = getConfig().getInt("stats." + key, 0);
        getConfig().set("stats." + key, current + 1);
        saveConfig();
    }
}