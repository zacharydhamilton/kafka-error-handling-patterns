// package com.example.pattern3producer.util;

// import java.io.PrintWriter;

// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.stereotype.Component;

// import jline.console.ConsoleReader;
// import picocli.CommandLine.Command;
// import picocli.CommandLine.ParentCommand;
// import picocli.CommandLine.Spec;
// import picocli.CommandLine.Model.CommandSpec;

// @Component
// @Command(name = "", subcommands = RootCommand.MakeCommand.class)
// public class RootCommand implements Runnable {
//     final ConsoleReader consoleReader;
//     final PrintWriter printWriter;

//     @Spec
//     private CommandSpec commandSpec;

//     @Autowired
//     RootCommand(ConsoleReader consoleReader) {
//         this.consoleReader = consoleReader;
//         this.printWriter = new PrintWriter(consoleReader.getOutput());
//     }

//     @Override
//     public void run() {
//         printWriter.println(commandSpec.usageMessage());
//     }

//     @Component
//     @Command(name = "make", subcommands = RootCommand.MakeCommand.PizzasCommand.class)
//     public static class MakeCommand implements Runnable {
//         @ParentCommand RootCommand root;

//         @Override
//         public void run() {
//             root.printWriter.println("MakeCommand");
//         }

//         @Component
//         @Command(name = "pizzas")
//         public static class PizzasCommand implements Runnable {
//             @ParentCommand MakeCommand make;

//             @Override
//             public void run() {
//                 make.root.printWriter.println("PizzasCommand");
//             }
//         }
//     }
// }
