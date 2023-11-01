package de.alphaomegait.aocommand.command;

import enums.SenderType;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ICommand {

  @NotNull String name();
  @NotNull String permission() default "";
  @NotNull String[] aliases() default {};
  @NotNull String usage() default "";
  @NotNull String description() default "";

  int minArgs() default 0;
  int maxArgs() default -1;
  int cooldown() default -1;

  SenderType allowedSender() default SenderType.ALL;
}