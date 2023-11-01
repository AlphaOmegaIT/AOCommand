package de.alphaomegait.aocommand.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class AOCommand {

  private @NotNull final CommandSender commandSender;
  private @NotNull final Command command;
  private @NotNull final String label;
  private @NotNull final String[] arguments;

  public AOCommand(
    final @NotNull CommandSender commandSender,
    final @NotNull Command command,
    final @NotNull String label,
    final @NotNull String[] arguments
  ) {
    this.commandSender = commandSender;
    this.command = command;
    this.label = label;
    this.arguments = arguments;
  }

  public Player getPlayer() {
    return this.commandSender instanceof Player player ? player : null;
  }

  public ConsoleCommandSender getConsole() {
    return this.commandSender instanceof ConsoleCommandSender console ? console : null;
  }

  public String[] getArguments() {
    return this.arguments;
  }

  public @Nullable String getArgument(
    final @NotNull Integer index
  ) {
    return this.arguments.length > index ? this.arguments[index] : null;
  }

  public Boolean areArgumentsEmpty() {
    return this.arguments.length == 0;
  }

  public @NotNull String getLabel() {
    return this.label;
  }

  public @NotNull Command getCommand() {
    return this.command;
  }
}