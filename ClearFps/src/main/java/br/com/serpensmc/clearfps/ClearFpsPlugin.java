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

    private enum Profile { NONE, LOW, MEDIUM, AFK, RECORDING }

    private final Map<UUID, Profile> activeProfiles = new HashMap<>();
    private final Set<UUID> nightVisionActive = new HashSet<>();
    private final Set<UUID> eternalDayActive = new HashSet<>();
    private final Set<UUID> hideDecorationsActive = new HashSet<>();
    private ItemStack backgroundGlass;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        backgroundGlass = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        ItemMeta meta = backgroundGlass.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.empty());
            backgroundGlass.setItemMeta(meta);
        }
        getServer().getPluginManager().registerEvents(this, this);
        if (getCommand("fps") != null) {
            getCommand("fps").setExecutor(this);
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

        Inventory inv = Bukkit.createInventory(null, 27, translate("&5&lClearFps"));
        
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, backgroundGlass);
        }

        inv.setItem(10, createItem(Material.GREEN_DYE, "&a&lPC Fraco", current == Profile.LOW,
            "&7---------------------------------------",
            "&fConfiguração projetada para hardwares de entrada.",
            "&fReduz agressivamente a carga de renderização,",
            "&focultando todas as entidades não essenciais.",
            "&7---------------------------------------",
            current == Profile.LOW ? "&a● ATIVADO SEU PERFIL" : "&e» Clique para aplicar o perfil"));
            
        inv.setItem(11, createItem(Material.ORANGE_DYE, "&6&lPC Mediano", current == Profile.MEDIUM,
            "&7---------------------------------------",
            "&fConfiguração equilibrada para hardwares médios.",
            "&fOculta monstros e itens caídos no chão para",
            "&fpreservar a estabilidade de quadros por segundo.",
            "&7---------------------------------------",
            current == Profile.MEDIUM ? "&a● ATIVADO SEU PERFIL" : "&e» Clique para aplicar o perfil"));
            
        inv.setItem(12, createItem(Material.RED_DYE, "&c&lAfk", current == Profile.AFK,
            "&7---------------------------------------",
            "&fOtimização extrema de performance de hardware.",
            "&fDestinado estritamente a períodos de inatividade.",
            "&fIsola completamente a visão do jogador do mapa.",
            "&7---------------------------------------",
            current == Profile.AFK ? "&a● ATIVADO SEU PERFIL" : "&e» Clique para aplicar o perfil"));
            
        inv.setItem(13, createItem(Material.PURPLE_DYE, "&5&lGravação", current == Profile.RECORDING,
            "&7---------------------------------------",
            "&fPrioriza a máxima fidelidade visual do ambiente.",
            "&fNível zero de culling de entidades, ideal para",
            "&fcaptura de conteúdo mantendo fluidez nativa.",
            "&7---------------------------------------",
            current == Profile.RECORDING ? "&a● ATIVADO SEU PERFIL" : "&e» Clique para aplicar o perfil"));
            
        inv.setItem(14, createItem(Material.GRAY_DYE, "&8&lDesativar Modos", false,
            "&7---------------------------------------",
            "&fRestaura todas as configurações visuais do",
            "&fcliente para as diretrizes padrões globais.",
            "&7---------------------------------------",
            "&e» Clique para limpar os perfis"));

        inv.setItem(19, createItem(Material.GOLDEN_CARROT, "&e&lVisão Noturna Permanente", nightVisionActive.contains(uuid),
            "&fRemove sombras dinâmicas de cavernas e blocos,",
            "&faliviando o processamento do motor gráfico do cliente.",
            "&7---------------------------------------",
            nightVisionActive.contains(uuid) ? "&a● ATIVADO" : "&c○ DESATIVADO"));

        inv.setItem(20, createItem(Material.SUNFLOWER, "&b&lFixar Dia & Tempo Limpo", eternalDayActive.contains(uuid),
            "&fTranca o seu cliente em clima ensolarado às 12:00.",
            "&fElimina completamente partículas pesadas de chuva.",
            "&7---------------------------------------",
            eternalDayActive.contains(uuid) ? "&a● ATIVADO" : "&c○ DESATIVADO"));

        inv.setItem(21, createItem(Material.ITEM_FRAME, "&d&lOcultar Decorações (FPS+)", hideDecorationsActive.contains(uuid),
            "&fEsconde Armor Stands e Item Frames por perto.",
            "&fGarante um ganho colossal de frames dentro de bases.",
            "&7---------------------------------------",
            hideDecorationsActive.contains(uuid) ? "&a● ATIVADO" : "&c○ DESATIVADO"));

        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(p);
            meta.displayName(translate("&e&lEstatísticas Globais de Uso"));
            meta.lore(Arrays.asList(
                translate("&7Acompanhamento estatístico da rede:"),
                Component.empty(),
                translate("&7PC Fraco utilizado: &f" + getConfig().getInt("stats.green", 0) + " vezes"),
                translate("&7PC Mediano utilizado: &f" + getConfig().getInt("stats.orange", 0) + " vezes"),
                translate("&7Perfil AFK utilizado: &f" + getConfig().getInt("stats.red", 0) + " vezes"),
                translate("&7Modo Gravação utilizado: &f" + getConfig().getInt("stats.purple", 0) + " vezes")
            ));
            head.setItemMeta(meta);
        }
        inv.setItem(16, head);

        p.openInventory(inv);
    }

    private ItemStack createItem(Material mat, String name, boolean glow, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(translate(name));
            List<Component> components = new ArrayList<>(lore.length);
            for (String line : lore) {
                components.add(translate(line));
            }
            meta.lore(components);
            if (glow) {
                meta.setEnchantmentGlintOverride(true);
            }
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
        if (!e.getView().title().equals(translate("&5&lClearFps"))) return;
        e.setCancelled(true);
        
        ItemStack clickedItem = e.getCurrentItem();
        if (clickedItem == null) return;
        
        Material clicked = clickedItem.getType();
        if (clicked == Material.PURPLE_STAINED_GLASS_PANE || clicked == Material.AIR || clicked == Material.PLAYER_HEAD) return;

        Player p = (Player) e.getWhoClicked();
        UUID uuid = p.getUniqueId();

        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_XYLOPHONE, 1.0f, 1.2f);

        boolean profileChanged = false;

        if (clicked == Material.GREEN_DYE) {
            activeProfiles.put(uuid, Profile.LOW);
            incrementStat("green");
            p.sendMessage(translate("&a&l[ClearFps] &fO módulo &a&lPC Fraco &ffoi estabelecido com sucesso."));
            profileChanged = true;
        } else if (clicked == Material.ORANGE_DYE) {
            activeProfiles.put(uuid, Profile.MEDIUM);
            incrementStat("orange");
            p.sendMessage(translate("&6&l[ClearFps] &fO módulo &6&lPC Mediano &ffoi estabelecido com sucesso."));
            profileChanged = true;
        } else if (clicked == Material.RED_DYE) {
            activeProfiles.put(uuid, Profile.AFK);
            incrementStat("red");
            p.sendMessage(translate("&c&l[ClearFps] &fO modo &c&lAFK Supremo &ffoi inicializado. Otimização total ativa."));
            profileChanged = true;
        } else if (clicked == Material.PURPLE_DYE) {
            activeProfiles.put(uuid, Profile.RECORDING);
            incrementStat("purple");
            p.sendMessage(translate("&5&l[ClearFps] &fModo &5&lGravação &fativo. Fidelidade visual aprimorada."));
            profileChanged = true;
        } else if (clicked == Material.GRAY_DYE) {
            activeProfiles.remove(uuid);
            p.sendMessage(translate("&8&l[ClearFps] &fConfigurações de renderização redefinidas."));
            profileChanged = true;
        }

        if (clicked == Material.GOLDEN_CARROT) {
            if (!nightVisionActive.add(uuid)) {
                nightVisionActive.remove(uuid);
                p.removePotionEffect(PotionEffectType.NIGHT_VISION);
            } else {
                p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, false, false));
            }
        } else if (clicked == Material.SUNFLOWER) {
            if (!eternalDayActive.add(uuid)) {
                eternalDayActive.remove(uuid);
                p.resetPlayerTime();
                p.resetPlayerWeather();
            } else {
                p.setPlayerTime(6000, false);
                p.setPlayerWeather(WeatherType.CLEAR);
            }
        } else if (clicked == Material.ITEM_FRAME) {
            if (!hideDecorationsActive.add(uuid)) {
                hideDecorationsActive.remove(uuid);
            }
        }

        if (profileChanged || clicked == Material.ITEM_FRAME || clicked == Material.GREEN_DYE || clicked == Material.ORANGE_DYE || clicked == Material.RED_DYE || clicked == Material.PURPLE_DYE || clicked == Material.GRAY_DYE) {
            Profile prof = activeProfiles.getOrDefault(uuid, Profile.NONE);
            boolean hideDeco = hideDecorationsActive.contains(uuid);

            if (prof == Profile.AFK) {
                p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Integer.MAX_VALUE, 1, false, false));
            } else {
                p.removePotionEffect(PotionEffectType.BLINDNESS);
            }

            for (Entity entity : p.getWorld().getEntities()) {
                boolean targetHide = false;

                if (prof == Profile.LOW && !(entity instanceof Player)) {
                    targetHide = true;
                } else if (prof == Profile.MEDIUM && (entity instanceof Monster || entity instanceof Item)) {
                    targetHide = true;
                } else if (prof == Profile.AFK) {
                    targetHide = true;
                }

                if (!targetHide && hideDeco && (entity instanceof ArmorStand || entity instanceof ItemFrame || entity instanceof Painting)) {
                    targetHide = true;
                }

                if (targetHide) {
                    p.hideEntity(this, entity);
                } else {
                    p.showEntity(this, entity);
                }
            }
        }

        openGUI(p);
    }

    private void incrementStat(String key) {
        int current = getConfig().getInt("stats." + key, 0);
        getConfig().set("stats." + key, current + 1);
        saveConfig();
    }
}