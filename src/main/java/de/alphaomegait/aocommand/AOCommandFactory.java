package de.alphaomegait.aocommand;

import de.alphaomegait.aocommand.command.AOCommand;
import de.alphaomegait.aocommand.command.ICommand;
import de.alphaomegait.aocommand.tabcompleter.ITabCompleter;
import enums.SenderType;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Logger;

public class AOCommandFactory implements CommandExecutor, TabCompleter {

  private @NotNull final JavaPlugin javaPlugin;
  
  private @NotNull final Map<ICommand, Map.Entry<Method, Object>> reflectCommandMap = new HashMap<>();

  private @NotNull final Map<ITabCompleter, Map.Entry<Method, Object>> reflectCommandCompleterMap = new HashMap<>();

  private @NotNull final Map<CommandSender, Map<ICommand, Long>> cooldowns = new HashMap<>();
  
  private @Nullable Function<AOCommand, Boolean> matchFunction = (aoCommand) -> false;

  protected @NotNull CommandMap commandMap;

  public AOCommandFactory(
    final @NotNull JavaPlugin javaPlugin
  ) {
    this.javaPlugin = javaPlugin;
    this.commandMap = this.javaPlugin.getServer().getCommandMap();
  }


  /**
   * Sets the match function for the AOCommand.
   *
   * @param matchFunction The function that determines if the command matches.
   */
  public void setMatchFunction(
    final @NotNull Function<AOCommand, Boolean> matchFunction
  ) {
    this.matchFunction = matchFunction;
  }

  /**
   * Executes the command when it is triggered.
   *
   * @param commandSender The sender of the command.
   * @param command The command that was executed.
   * @param label The label used for the command.
   * @param arguments The arguments passed with the command.
   * @return True if the command was executed successfully, false otherwise.
   */
  @Override
  public boolean onCommand(
    final @NotNull CommandSender commandSender,
    final @NotNull Command command,
    final @NotNull String label,
    final @NotNull String[] arguments
  ) {
    // Get the lowercase command name
    final String commandName = command.getName().toLowerCase();

    // Get the associated command entry
    final Map.Entry<ICommand, Map.Entry<Method, Object>> entry = this.getAssociatedCommands(commandName);

    // If no associated command found, execute the match function if available
    if (entry == null) {
      if (this.matchFunction != null) {
        this.matchFunction.apply(new AOCommand(commandSender, command, label, arguments));
      }
      return true;
    }

    // Get the associated ICommand and its permission
    final ICommand iCommand = entry.getKey();
    final String commandPermission = iCommand.permission().toLowerCase();

    // Check the sender type and send appropriate message based on config
    final SenderType senderType = iCommand.allowedSender();
    if (senderType == SenderType.CONSOLE && !(commandSender instanceof ConsoleCommandSender)) {
      //TODO SEND MESSAGE BASED ON CONFIG 'ONLY FOR CONSOLE'
      return true;
    }

    if (senderType == SenderType.PLAYER && !(commandSender instanceof Player)) {
      //TODO SEND MESSAGE BASED ON CONFIG 'ONLY FOR PLAYERS'
      return true;
    }

    // Check if the sender has the required permission
    if (!commandPermission.isEmpty() && !commandPermission.isBlank() && !commandSender.hasPermission(commandPermission)) {
      //TODO SEND MESSAGE BASED ON CONFIG 'NO PERMISSION'
      return true;
    }

    // Check if the command has a cooldown
    if (this.hasCooldown(commandSender, iCommand)) {
      return true;
    }

    // Check if the command has too few arguments and send appropriate message based on config
    if (arguments.length < iCommand.minArgs()) {
      //TODO SEND MESSAGE BASED ON CONFIG 'TOO FEW ARGS'
      return true;
    }

    // Check if the command has too many arguments and send appropriate message based on config
    if (iCommand.maxArgs() != -1 && arguments.length > iCommand.maxArgs()) {
      //TODO SEND MESSAGE BASED ON CONFIG 'TOO MANY ARGS'
      return true;
    }

    try {
      // Invoke the command method
      entry.getValue().getKey().invoke(entry.getValue().getValue(), new AOCommand(commandSender, command, label, arguments));
    } catch (final Exception ignored) {}
    return true;
  }

  /**
   * This method is called when tab completion is requested for a command.
   *
   * @param sender     The sender of the command.
   * @param command    The command that was executed.
   * @param label      The alias used for the command.
   * @param arguments  The arguments passed to the command.
   * @return           A list of possible completions for the command.
   */
  @Override
  @SuppressWarnings("unchecked")
  public List<String> onTabComplete(
    final @NotNull CommandSender sender,
    final @NotNull Command command,
    final @NotNull String label,
    final @NotNull String[] arguments
  ) {
    // Get the associated tab completer for the command
    Optional<Map.Entry<ITabCompleter, Map.Entry<Method, Object>>> tabCompleterEntry = Optional.ofNullable(getAssociatedCompleter(command.getName().toLowerCase()));

    // If there is no associated tab completer, return an empty list
    if (tabCompleterEntry.isEmpty())
      return new ArrayList<>();

    // Check if the sender has the required permission
    String permission = tabCompleterEntry.get().getKey().permission();
    if (!permission.isEmpty() && !permission.isBlank() && !sender.hasPermission(permission))
      return new ArrayList<>();

    try {
      // Invoke the tab completer method with the appropriate arguments
      return (List<String>) tabCompleterEntry.get().getValue().getKey().invoke(
        tabCompleterEntry.get().getValue().getValue(),
        new AOCommand(sender, command, label, arguments)
      );
    } catch (final Exception ignored) {
      // If an exception occurs, return an empty list
      return new ArrayList<>();
    }
  }

  /**
   * Register commands and command completer of a given class.
   *
   * @param commandClazz The class containing the commands and command completer.
   */
  public void registerCommandsOfClazz(final @NotNull Object commandClazz) {
    final Logger logger = this.javaPlugin.getLogger();
    final String commandClassName = commandClazz.getClass().getName();

    // Register commands
    logger.finest("Registering commands of class: " + commandClassName + ".");
    final List<Method> commandMethods = Arrays.stream(commandClazz.getClass().getDeclaredMethods())
                                              .filter(method -> method.isAnnotationPresent(ICommand.class))
                                              .toList();

    commandMethods.forEach(method -> {
      if (Arrays.stream(method.getParameterTypes()).noneMatch(type -> type == AOCommand.class)) {
        logger.severe("Method: " + method.getName() + " has no parameter of type " + ICommand.class.getName() + ".");
        return;
      }

      final ICommand iCommand = method.getAnnotation(ICommand.class);
      this.registerCommand(iCommand, method, commandClazz);
    });

    logger.finest("Registered " + commandMethods.size() + " commands.");

    // Register command completers
    logger.finest("Registering command completer of class: " + commandClassName + ".");
    final List<Method> commandCompleterMethods = Arrays.stream(commandClazz.getClass().getDeclaredMethods())
                                                       .filter(method -> method.isAnnotationPresent(ITabCompleter.class))
                                                       .toList();

    commandCompleterMethods.forEach(method -> {
      if (Arrays.stream(method.getParameterTypes()).noneMatch(type -> type == AOCommand.class)) {
        logger.severe("Method: " + method.getName() + " has no parameter of type " + AOCommand.class.getName() + ".");
        return;
      }

      if (!method.getReturnType().isAssignableFrom(List.class)) {
        logger.severe("Method: " + method.getName() + " has return type " + method.getReturnType().getName()
                      + " which is not a List from type String.");
        return;
      }

      final ITabCompleter iTabCompleter = method.getAnnotation(ITabCompleter.class);
      this.reflectCommandCompleterMap.put(iTabCompleter, new AbstractMap.SimpleEntry<>(method, commandClazz));
    });
  }

  /**
   * Get the associated command and its method and object based on the command name.
   *
   * @param commandName The name of the command.
   * @return The associated command and its method and object, or null if not found.
   */
  private @Nullable Map.Entry<ICommand, Map.Entry<Method, Object>> getAssociatedCommands(
    final @NotNull String commandName
  ) {
    // Filter the command map based on the command name
    return this.reflectCommandMap.entrySet().stream()
                                 .filter(entry -> commandName.equalsIgnoreCase(entry.getKey().name()))
                                 .findFirst()
                                 .map(entry -> new AbstractMap.SimpleEntry<>(entry.getKey(), entry.getValue()))
                                 .orElse(null);
  }

  /**
   * Returns the associated tab completer for a given command name.
   *
   * @param commandName The name of the command.
   * @return The tab completer associated with the command, or null if not found.
   */
  private @Nullable Map.Entry<ITabCompleter, Map.Entry<Method, Object>> getAssociatedCompleter(final @NotNull String commandName) {
    return this.reflectCommandCompleterMap.entrySet().stream()
                                          .filter(entry -> {
                                            ITabCompleter iTabCompleter = entry.getKey();
                                            String completerName = iTabCompleter.name();
                                            return commandName.equalsIgnoreCase(completerName) || Arrays.stream(iTabCompleter.aliases()).anyMatch(commandName::equalsIgnoreCase);
                                          })
                                          .findFirst()
                                          .orElse(null);
  }

  /**
   * Checks if the given command has a cooldown for the specified command sender.
   *
   * @param commandSender The command sender.
   * @param iCommand      The command to check.
   * @return True if the command has a cooldown and is still in cooldown, false otherwise.
   */
  private boolean hasCooldown(final @NotNull CommandSender commandSender, final @NotNull ICommand iCommand) {
    if (iCommand.cooldown() < 1) {
      return false;
    }

    // Get the cooldown map for the command sender
    final Map<ICommand, Long> cooldownMap = this.cooldowns.computeIfAbsent(commandSender, keyMap -> new HashMap<>());

    // Get the previous time the command was used
    final long currentTime = System.currentTimeMillis();
    final long previousTime = cooldownMap.getOrDefault(iCommand, 0L);

    // Calculate the remaining time before the command can be used again
    final int remainingTime = (int) ((currentTime - previousTime) / 1000) % 60;
    if (remainingTime <= iCommand.cooldown()) {
      // TODO: SEND MESSAGE BASED ON CONFIG 'WAIT BEFORE USING AGAIN' command.cooldown() - remainingTime
      return true;
    }

    // Update the cooldown map with the current time
    cooldownMap.put(iCommand, currentTime);
    return false;
  }

  /**
   * Registers a command with the provided ICommand, method, and command class.
   *
   * @param iCommand      The ICommand instance representing the command.
   * @param method        The Method object representing the method to be executed when the command is called.
   * @param commandClazz  The Object representing the command class.
   */
  private void registerCommand(final @NotNull ICommand iCommand, final @NotNull Method method, final @NotNull Object commandClazz) {
    final String commandName = iCommand.name().toLowerCase();

    // Add the ICommand, method, and command class to the reflectCommandMap.
    this.reflectCommandMap.put(iCommand, new AbstractMap.SimpleEntry<>(method, commandClazz));

    try {
      // Get the declared constructor of PluginCommand and make it accessible.
      Constructor<PluginCommand> pluginCommandConstructor = PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
      pluginCommandConstructor.setAccessible(true);

      // Create a new instance of PluginCommand using the constructor.
      PluginCommand pluginCommand = pluginCommandConstructor.newInstance(commandName, javaPlugin);

      // Set the tab completer, executor, usage, permission, label, description, and aliases of the plugin command.
      pluginCommand.setTabCompleter(this);
      pluginCommand.setExecutor(this);
      pluginCommand.setUsage(iCommand.usage());
      pluginCommand.setPermission(iCommand.permission());
      pluginCommand.setLabel(commandName);
      pluginCommand.setPermission(iCommand.permission());
      pluginCommand.setDescription(iCommand.description());
      pluginCommand.setAliases(Arrays.stream(iCommand.aliases()).map(String::toLowerCase).toList());

      // Register the command with the command map.
      this.commandMap.register(commandName, commandName, pluginCommand);
    } catch (Exception exception) {
      // Log an error message if the command registration fails.
      this.javaPlugin.getLogger().severe("Failed to register command: " + commandName + ". Exception: " + exception.getMessage());
    }
  }
}