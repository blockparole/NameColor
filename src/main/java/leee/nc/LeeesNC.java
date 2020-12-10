package leee.nc;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LeeesNC extends JavaPlugin implements Listener {

    private Config config;
    private Map<String, ChatColor> formatterLookup;

    public void onEnable() {
        config = new Config();
        formatterLookup = generateFormatterLookup();
        getServer().getPluginManager().registerEvents(this, this);
    }

    private Map<String, ChatColor> generateFormatterLookup() {

        Stream<ChatColor> colors = Arrays
                .stream(ChatColor.values())
                .filter(ChatColor::isColor);

        Stream<ChatColor> formatters = Arrays
                .stream(ChatColor.values())
                .filter(ChatColor::isFormat)
                .filter(c -> c.equals(ChatColor.BOLD) && config.modifierAllowedBold
                        || c.equals(ChatColor.ITALIC) && config.modifierAllowedItalic
                        || c.equals(ChatColor.MAGIC) && config.modifierAllowedMagic
                        || c.equals(ChatColor.STRIKETHROUGH) && config.modifierAllowedStrikethrough
                        || c.equals(ChatColor.UNDERLINE) && config.modifierAllowedUnderline);

        return Stream.concat(colors, formatters).collect(Collectors.toMap(e -> e.asBungee().getName().toLowerCase(), e -> e));

    }

    @Override
    public boolean onCommand(@Nonnull CommandSender sender, Command command, @Nonnull String commandLabel, @Nonnull String[] args) {

        if (!command.getName().equalsIgnoreCase("nc")) {
            return false;
        }

        if (!(sender instanceof Player)) {
            return false;
        }

        Player player = (Player) sender;

        if (config.commandNeedsPermission && !player.hasPermission("leee.nc")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to perform this command!");
            return false;
        }

        if (args.length == 0) {
            // TODO: player.sendMessage(helpMessage);
            return false;
        }

        String formatters = Arrays.stream(args).map(s -> s.replaceAll("[^a-zA-Z]", "_").toLowerCase())
                .filter(s -> formatterLookup.containsKey(s))
                .map(s -> formatterLookup.get(s).toString())
                .collect(Collectors.joining());

        return changeNameColor(player, formatters, true);

    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {

        String modifiers = this.getConfig().getString(String.valueOf(event.getPlayer().getUniqueId()));
        if (modifiers != null && !modifiers.isEmpty()) {
            changeNameColor(event.getPlayer(), modifiers, false);
        }

    }

    private boolean changeNameColor(Player player, String modifiers, boolean notify) {

        if (modifiers.isEmpty()) {
            return false;
        }

        String newName = modifiers + player.getName() + ChatColor.RESET;
        getLogger().info("Changing name of " + player.getName() + " to: " + newName);
        player.setDisplayName(newName);
        this.getConfig().set(String.valueOf(player.getUniqueId()), modifiers);
        this.saveConfig();

        if (notify) {
            player.sendMessage(ChatColor.GOLD + "Your name is now: " + newName);
        }

        return true;

    }

    class Config {

        final boolean commandNeedsPermission;
        final boolean modifierAllowedBold;
        final boolean modifierAllowedItalic;
        final boolean modifierAllowedMagic;
        final boolean modifierAllowedStrikethrough;
        final boolean modifierAllowedUnderline;

        public Config() {

            // defaults
            getConfig().addDefault("command-needs-permission", false);
            getConfig().addDefault("modifier-allowed-bold", true);
            getConfig().addDefault("modifier-allowed-strikethrough", false);
            getConfig().addDefault("modifier-allowed-underline", false);
            getConfig().addDefault("modifier-allowed-italic", true);
            getConfig().addDefault("modifier-allowed-magic", false);
            getConfig().options().copyDefaults(true);
            saveConfig();

            this.commandNeedsPermission = getConfig().getBoolean("command-needs-permission");

            // TODO: permission based formatter usage
            this.modifierAllowedBold = getConfig().getBoolean("modifier-allowed-bold");
            this.modifierAllowedItalic = getConfig().getBoolean("modifier-allowed-italic");
            this.modifierAllowedMagic = getConfig().getBoolean("modifier-allowed-magic");
            this.modifierAllowedStrikethrough = getConfig().getBoolean("modifier-allowed-strikethrough");
            this.modifierAllowedUnderline = getConfig().getBoolean("modifier-allowed-underline");

        }

    }

}
