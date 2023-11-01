Initialize the command factory in the main Java plugin class like the following: 

```
public class Example extends JavaPlugin {

  @Override
  public void onEnable() {
    // initialization of the factory
    AOCommandFactory commandFactory = new AOCommandFactory(this);

    // registration of the command class to register tab completer and command.
    commandFactory.registerCommandsOfClazz(new ExampleCommand());
  }
}
```

Declare a command like the following:

```
  @ICommand(
    name = "example",
    allowedSender = SenderType.PLAYER, //SenderType.CONSOLE, ALL
    aliases = {"example1"},
    maxArgs = 1,
    minArgs = 1,
    description = "This is a test command.",
    usage = "This is a test command."
  )
  public void execute(
    final AOCommand command
  ) {
    if (command.getPlayer() != null)
      command.getPlayer().sendMessage("This is a test command by player.");
  }
``` 

Declare a tab completer like the following: 

```
  @ITabCompleter(
    name = "example",
    aliases = {"", "", ""},
    permission = "example.permission"
  )
  public List<String> onTabComplete(
    final AOCommand command
  ) {
    return List.of("test", "test2", "test3");
  }
```
