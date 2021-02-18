package not.hub.namecolor;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Mod extends JavaPlugin implements Listener {

    private static final String DARK = "dark";
    private static final String LIGHT = "light";
    private static final String RESET = "reset";
    private static final String MAGIC = "magic";
    private static final String OBFUSCATED = "obfuscated";

    private static final Map<String, Set<String>> colorPrefix = new HashMap<String, Set<String>>() {
        {
            put(DARK, new HashSet<>());
            put(LIGHT, new HashSet<>());
            Arrays
                    .stream(ChatColor.values())
                    .filter(ChatColor::isColor)
                    .map(ChatColor::asBungee)
                    .map(net.md_5.bungee.api.ChatColor::getName)
                    .forEach(name -> {
                        if (name.startsWith(DARK)) {
                            get(DARK).add(name.split("_")[1]);
                        } else if (name.startsWith(LIGHT)) {
                            get(LIGHT).add(name.split("_")[1]);
                        }
                    });
        }
    };

    private Map<String, ChatColor> modifierLookup;
    private String helpMessage;

    public void onEnable() {

        config();

        modifierLookup = generateModifierLookup();
        helpMessage = generateHelpMessage();

        getServer().getPluginManager().registerEvents(this, this);

    }

    private void config() {

        getConfig().addDefault("permission-global", "namecolor.global");
        getConfig().addDefault("permission-global-required", false);

        getConfig().addDefault("permission-reset", "namecolor.reset");
        getConfig().addDefault("permission-reset-required", false);

        // TODO?: permission based formatter usage ->
        // (users can define permissions in config to allow certain formatters for certain permissions)
        // modifier-bold-require-permission: true
        // modifier-bold-permissions: foo.bar.bold, foo.bar.rab.oof, bar.foo

        getConfig().addDefault("save-modifiers", true);
        getConfig().addDefault("load-modifiers", true);

        getConfig().addDefault("modifier-bold-allow", true);
        getConfig().addDefault("modifier-italic-allow", true);
        getConfig().addDefault("modifier-magic-allow", false);
        getConfig().addDefault("modifier-strikethrough-allow", false);
        getConfig().addDefault("modifier-underline-allow", false);

        getConfig().options().copyDefaults(true);
        saveConfig();

    }

    private boolean isAllowed(ChatColor c) {
        return (!c.equals(ChatColor.BOLD) || getConfig().getBoolean("modifier-bold-allow"))
                && (!c.equals(ChatColor.ITALIC) || getConfig().getBoolean("modifier-italic-allow"))
                && (!c.equals(ChatColor.MAGIC) || getConfig().getBoolean("modifier-magic-allow"))
                && (!c.equals(ChatColor.STRIKETHROUGH) || getConfig().getBoolean("modifier-strikethrough-allow"))
                && (!c.equals(ChatColor.UNDERLINE) || getConfig().getBoolean("modifier-underline-allow"));
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {

        if (!getConfig().getBoolean("load-modifiers")) {
            return;
        }

        String modifiers = loadModifiers(event.getPlayer().getUniqueId());
        if (!modifiers.isEmpty()) {
            changeNameColor(event.getPlayer(), modifiers, false);
        }

    }

    @Override
    public boolean onCommand(@Nonnull CommandSender sender, Command command, @Nonnull String commandLabel, @Nonnull String[] args) {

        if (!(sender instanceof Player)) {
            return false;
        }

        Player player = (Player) sender;

        if (getConfig().getBoolean("permission-global-required") && !player.hasPermission(getConfig().getString("permission-global"))) {
            player.sendMessage(ChatColor.RED + "You do not have sufficient permissions to use this command!");
            return false;
        }

        List<String> params = Arrays
                .stream(args)
                .map(String::toLowerCase)
                .map(s -> s.replaceAll("[^a-z]", "_"))
                .map(s -> s.replaceAll(MAGIC, OBFUSCATED))
                .collect(Collectors.toList());

        if (params.contains(RESET) && (!getConfig().getBoolean("permission-reset-required") || player.hasPermission(getConfig().getString("permission-reset")))) {
            resetNameColor(player);
            return true;
        }

        // convert "dark red" to "dark_red", "light purple" to "light_purple" etc.
        IntStream.range(0, params.size() - 1).forEach(i -> {
            if (colorPrefix.getOrDefault(params.get(i).toLowerCase(), new HashSet<>()).contains(params.get(i + 1))) {
                params.set(i, params.get(i) + '_' + params.get(i + 1));
                params.set(i + 1, "");
            }
        });

        String modifiers = params.stream()
                .map(s -> modifierLookup.getOrDefault(s, null))
                .filter(Objects::nonNull)
                .filter(this::isAllowed)
                .map(ChatColor::toString)
                .sorted(String::compareTo)
                .collect(Collectors.joining());

        return changeNameColor(player, modifiers, true);

    }

    private boolean changeNameColor(Player player, String modifiers, boolean notify) {
        if (modifiers.isEmpty()) {
            player.sendMessage(helpMessage);
            return false;
        }
        player.setDisplayName(modifiers + player.getName() + ChatColor.RESET);
        if (notify) {
            player.sendMessage(ChatColor.GOLD + "Name changed to: " + ChatColor.RESET + player.getDisplayName());
        }
        if (getConfig().getBoolean("save-modifiers")) {
            saveModifiers(player.getUniqueId(), modifiers);
        }
        return true;
    }

    private void resetNameColor(Player player) {
        player.setDisplayName(player.getName());
        player.sendMessage(ChatColor.GOLD + "Name reset to default: " + ChatColor.RESET + player.getDisplayName());
        if (getConfig().getBoolean("save-modifiers")) {
            saveModifiers(player.getUniqueId(), null);
        }
    }

    private Map<String, ChatColor> generateModifierLookup() {
        return Stream
                .concat(Arrays
                                .stream(ChatColor.values())
                                .filter(ChatColor::isColor),
                        Arrays
                                .stream(ChatColor.values())
                                .filter(ChatColor::isFormat))
                .collect(Collectors.toMap(e -> e.asBungee().getName().toLowerCase(), e -> e));
    }

    private String generateHelpMessage() {
        return ChatColor.GOLD + "Please specify at least one valid modifier: "
                + String.join(" ", modifierLookup
                .entrySet()
                .stream()
                .filter(c -> isAllowed(c.getValue()))
                .map(entry -> entry.getValue() + entry.getKey() + ChatColor.RESET)
                .collect(Collectors.toSet())) + " reset";
    }

    public void saveModifiers(UUID uuid, String modifiers) {
        getConfig().set(String.valueOf(uuid), modifiers);
        saveConfig();
    }

    public String loadModifiers(UUID uuid) {
        return Optional.ofNullable(getConfig().getString(String.valueOf(uuid))).orElse("");
    }

}
