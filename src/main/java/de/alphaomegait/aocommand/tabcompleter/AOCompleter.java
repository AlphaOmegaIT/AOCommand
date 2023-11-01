package de.alphaomegait.aocommand.tabcompleter;

import de.alphaomegait.aocommand.command.AOCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.*;

public class AOCompleter implements TabCompleter {

  private final Map<String, Map.Entry<Method, Object>> reflectCompleterMap = new HashMap<>();

  /**
   * Overrides the onTabComplete method to provide tab completion for a command.
   *
   * @param sender The command sender.
   * @param command The command being executed.
   * @param label The command label.
   * @param args The command arguments.
   * @return A list of tab completions.
   */
  @Override
  @SuppressWarnings("unchecked")
  public @Nullable List<String> onTabComplete(
    @NotNull final CommandSender sender,
    @NotNull final Command command,
    @NotNull final String label,
    final @NotNull String[] args
  ) {
    // Filter the reflectCompleterMap entries to find the one that matches the label
    return this.reflectCompleterMap.entrySet().stream()
                                   .filter(entry -> entry.getKey().equalsIgnoreCase(label.toLowerCase()))
                                   .findFirst()
                                   .map(entry -> {
                                     try {
                                       // Invoke the method associated with the entry
                                       return (List<String>) entry.getValue().getKey().invoke(
                                         entry.getValue(),
                                         new AOCommand(sender, command, label.toLowerCase(), args)
                                       );
                                     } catch (final Exception ignored) {}
                                     return null;
                                   })
                                   .orElse(new ArrayList<>());
  }

  /**
   * Adds a completer to the reflectCompleterMap.
   *
   * @param label          The label associated with the completer.
   * @param method         The method to be called when the completer is invoked.
   * @param completerClazz The class of the completer object.
   */
  public void addCompleter(
    final @NotNull String label,
    final @NotNull Method method,
    final @NotNull Object completerClazz
  ) {
    // Create a new entry with the method and completer class
    Map.Entry<Method, Object> completerEntry = new AbstractMap.SimpleEntry<>(method, completerClazz);

    // Add the entry to the reflectCompleterMap with the label as the key
    this.reflectCompleterMap.put(label, completerEntry);
  }
}