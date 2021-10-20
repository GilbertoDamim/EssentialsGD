package io.github.gilbertodamim.essentials;

import io.github.gilbertodamim.essentials.library.LibChecker;
import io.github.gilbertodamim.essentials.management.DisablePlugin;
import io.github.gilbertodamim.essentials.management.StartPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.JavaPluginLoader;


import java.lang.reflect.Field;
import java.net.URLClassLoader;
import java.util.*;


@SuppressWarnings({"unchecked"})
public class EssentialsMain extends JavaPlugin {
    public static String pluginName = "§f[§bEssentials§cGD§f]";
    public static EssentialsMain instance;

    public static EssentialsMain instance() {
        return instance;
    }

    public static void disableLoggers() {
        try {
            org.apache.logging.log4j.core.LoggerContext ctx = (org.apache.logging.log4j.core.LoggerContext) org.apache.logging.log4j.LogManager.getContext(false);
            org.apache.logging.log4j.core.config.Configuration config = ctx.getConfiguration();
            ArrayList<String> toRemove = new ArrayList<>();
            toRemove.add("com.zaxxer.hikari.pool.PoolBase");
            toRemove.add("com.zaxxer.hikari.pool.HikariPool");
            toRemove.add("com.zaxxer.hikari.HikariDataSource");
            toRemove.add("com.zaxxer.hikari.HikariConfig");
            toRemove.add("com.zaxxer.hikari.util.DriverDataSource");
            toRemove.add("Exposed");
            for (String remove : toRemove) {
                config.getLoggerConfig(remove).setLevel(org.apache.logging.log4j.Level.OFF);
            }
        }
        catch (Exception ignored) {}
    }

    public static void disablePlugin() {
        Plugin plugin = instance.getServer().getPluginManager().getPlugin(instance.getName());
        assert plugin != null;
        Bukkit.getPluginManager().disablePlugin(plugin);
        Bukkit.getScheduler().cancelTasks(plugin);
        Bukkit.getServicesManager().unregisterAll(plugin);
        HandlerList.unregisterAll(plugin);
        try {
            Field commandMapField = Bukkit.getPluginManager().getClass().getDeclaredField("commandMap");
            Field commandsField = SimpleCommandMap.class.getDeclaredField("knownCommands");
            commandMapField.setAccessible(true);
            commandsField.setAccessible(true);
            CommandMap commandMap = (CommandMap) commandMapField.get(Bukkit.getPluginManager());
            Map<String, Command> commands = (Map<String, Command>) commandsField.get(commandMap);
            Set<Map.Entry<String, Command>> removes = new HashSet<>();
            for (Map.Entry<String, Command> entry : commands.entrySet()) {
                if (entry.getValue() instanceof PluginCommand) {
                    PluginCommand command = (PluginCommand) entry.getValue();
                    if (command.getPlugin().equals(plugin)) {
                        removes.add(entry);
                    }
                }
            }
            for (Map.Entry<String, Command> entry : removes) {
                entry.getValue().unregister(commandMap);
                commands.remove(entry.getKey(), entry.getValue());
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        try {
            Field pluginsField = Bukkit.getPluginManager().getClass().getDeclaredField("plugins");
            Field lookupNamesField = Bukkit.getPluginManager().getClass().getDeclaredField("lookupNames");
            pluginsField.setAccessible(true);
            lookupNamesField.setAccessible(true);
            List<Plugin> plugins = (List<Plugin>) pluginsField.get(Bukkit.getPluginManager());
            Map<String, Plugin> names = (Map<String, Plugin>) lookupNamesField.get(Bukkit.getPluginManager());
            plugins.remove(plugin);
            names.remove(plugin.getName());
            names.remove(plugin.getName().toLowerCase());
        } catch (Throwable e) {
            e.printStackTrace();
        }
        ClassLoader cl = plugin.getClass().getClassLoader();
        if (cl != null) {
            try {
                Field pluginField = cl.getClass().getDeclaredField("plugin");
                Field pluginInitField = cl.getClass().getDeclaredField("pluginInit");
                pluginField.setAccessible(true);
                pluginInitField.setAccessible(true);
                pluginField.set(cl, null);
                pluginInitField.set(cl, null);
            } catch (Throwable ignored) {
            }
            try {
                ((URLClassLoader) cl).close();
            } catch (Throwable ignored) {
            }
        }
        try {
            JavaPluginLoader jpl = (JavaPluginLoader) plugin.getPluginLoader();
            Field loadersField = jpl.getClass().getDeclaredField("loaders");
            loadersField.setAccessible(true);
            Map<String, ?> loadersMap = (Map<String, ?>) loadersField.get(jpl);
            loadersMap.remove(plugin.getName());
        } catch (Throwable ignored) {
        }
        System.gc();
    }

    public void onEnable() {
        instance = this;
        disableLoggers();
        LibChecker.checkversion();
        if (LibChecker.update) return;
        new StartPlugin();
        super.onEnable();
    }

    public void onDisable() {
        new DisablePlugin();
        super.onDisable();
    }
}
