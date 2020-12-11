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

    private Config config;
    private Map<String, String> modifierLookup;
    private String helpMessage;

    public void onEnable() {

        config = new Config();
        modifierLookup = generateModifierLookup();
        helpMessage = generateHelpMessage();

        getServer().getPluginManager().registerEvents(this, this);

    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {

        if (!config.loadModifiers) {
            return;
        }

        String modifiers = config.loadModifiers(event.getPlayer().getUniqueId());
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

        if (config.permissionGlobalRequired && !player.hasPermission(config.permissionGlobal)) {
            player.sendMessage(ChatColor.RED + "You do not have sufficient permissions to use this command!");
            return false;
        }

        List<String> params = Arrays
                .stream(args)
                .map(String::toLowerCase)
                .map(s -> s.replaceAll("[^a-z]", "_"))
                .map(s -> s.replaceAll(MAGIC, OBFUSCATED))
                .collect(Collectors.toList());

        if (params.contains(RESET) && (!config.permissionResetRequired || player.hasPermission(config.permissionReset))) {
            resetNameColor(player, true);
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
                .map(s -> modifierLookup.getOrDefault(s, ""))
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

        if (config.saveModifiers) {
            config.saveModifiers(player.getUniqueId(), modifiers);
        }

        return true;

    }

    private void resetNameColor(Player player, boolean notify) {

        player.setDisplayName(player.getName());
        if (notify) {
            player.sendMessage(ChatColor.GOLD + "Name reset to default: " + ChatColor.RESET + player.getDisplayName());
        }

        if (config.saveModifiers) {
            config.saveModifiers(player.getUniqueId(), null);
        }

    }

    private Map<String, String> generateModifierLookup() {

        Stream<ChatColor> colors = Arrays
                .stream(ChatColor.values())
                .filter(ChatColor::isColor);

        Stream<ChatColor> formatters = Arrays
                .stream(ChatColor.values())
                .filter(ChatColor::isFormat)
                .filter(c -> c.equals(ChatColor.BOLD) && config.modifierBoldAllow
                        || c.equals(ChatColor.ITALIC) && config.modifierItalicAllow
                        || c.equals(ChatColor.MAGIC) && config.modifierMagicAllow
                        || c.equals(ChatColor.STRIKETHROUGH) && config.modifierStrikethroughAllow
                        || c.equals(ChatColor.UNDERLINE) && config.modifierUnderlineAllow);

        return Stream
                .concat(colors, formatters)
                .collect(Collectors.toMap(e -> e.asBungee().getName().toLowerCase(), ChatColor::toString));

    }

    private String generateHelpMessage() {

        return ChatColor.GOLD + "Please specify at least one valid modifier: "
                + String.join(" ", modifierLookup
                .entrySet()
                .stream()
                .map(entry -> entry.getValue() + entry.getKey() + ChatColor.RESET)
                .collect(Collectors.toSet()));

    }

    class Config {

        final String permissionGlobal;
        final boolean permissionGlobalRequired;
        final String permissionReset;
        final boolean permissionResetRequired;

        final boolean saveModifiers;
        final boolean loadModifiers;

        final boolean modifierBoldAllow;
        final boolean modifierItalicAllow;
        final boolean modifierMagicAllow;
        final boolean modifierStrikethroughAllow;
        final boolean modifierUnderlineAllow;

        public Config() {

            // TODO: better names for path, default values and config fields
            getConfig().addDefault("permission-global", "namecolor.global");
            getConfig().addDefault("permission-global-required", false);

            getConfig().addDefault("permission-reset", "namecolor.reset");
            getConfig().addDefault("permission-reset-required", false);

            // TODO: permission based formatter usage ->
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

            this.permissionGlobal = getConfig().getString("permission-global");
            this.permissionGlobalRequired = getConfig().getBoolean("permission-global-required");

            this.permissionReset = getConfig().getString("permission-reset");
            this.permissionResetRequired = getConfig().getBoolean("permission-reset-required");

            this.saveModifiers = getConfig().getBoolean("save-modifiers");
            this.loadModifiers = getConfig().getBoolean("load-modifiers");

            this.modifierBoldAllow = getConfig().getBoolean("modifier-bold-allow");
            this.modifierItalicAllow = getConfig().getBoolean("modifier-italic-allow");
            this.modifierMagicAllow = getConfig().getBoolean("modifier-magic-allow");
            this.modifierStrikethroughAllow = getConfig().getBoolean("modifier-strikethrough-allow");
            this.modifierUnderlineAllow = getConfig().getBoolean("modifier-underline-allow");

        }

        public void saveModifiers(UUID uuid, String modifiers) {
            getConfig().set(String.valueOf(uuid), modifiers);
            saveConfig();
        }

        public String loadModifiers(UUID uuid) {
            return Optional.ofNullable(getConfig().getString(String.valueOf(uuid))).orElse("");
        }

    }

}
