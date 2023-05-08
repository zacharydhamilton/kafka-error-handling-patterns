package com.example.pattern3producer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

// import com.example.pattern3producer.util.RootCommand;

import picocli.CommandLine;

@SpringBootApplication
public class Pattern3producerApplication {
// public class Pattern3producerApplication implements CommandLineRunner {
	// private RootCommand rootCommand;
	public static void main(String[] args)  {
		new SpringApplicationBuilder(Pattern3producerApplication.class).logStartupInfo(false).build().run(args);		
	}

	// @Autowired
	// public Pattern3producerApplication(RootCommand rootCommand) {
	// 	this.rootCommand = rootCommand;
	// }
	// @Override
	// public void run(String... args) {
	// 	CommandLine commandLine = new CommandLine(rootCommand);
	// 	commandLine.execute(args);
	// }
}

		// System.out.println("What's up Denny's?");
		// String usage = """
		// 	| To start this application in a simulated way, use: "> simulation start". To stop a silumation, "> simulation stop".
		// 	| Otherwise, use: "> make pizzas", which will provide additional prompts. 
		// 	| At any time, use: "> quit" to exit the application.
		// """;
		// Scanner scanner = new Scanner(System.in);
		// System.out.println(usage);
		// System.out.print("> ");
		// while (scanner.hasNextLine()) {
		// 	String input = scanner.nextLine();
		// 	if (input.isEmpty() || input.equals("\\n")) {
		// 		input = "empty";
		// 	}
		// 	StringTokenizer tokens = new StringTokenizer(input);
		// 	String command = tokens.nextToken();
		// 	if (command.equals("quit")) {
		// 		System.out.println("Bye bye!");
		// 		System.exit(0);
		// 	} else if (command.equals("simulation")) {
		// 		if (tokens.hasMoreTokens()) {
		// 			String subcommand = tokens.nextToken();
		// 			if (subcommand.equals("start")) {
		// 				simulation.start();
		// 				System.out.print("> ");
		// 			} else if (subcommand.equals("stop")) {
		// 				simulation.stop();
		// 				System.out.print("> ");
		// 			} else {
		// 				System.out.println(String.format("Unknown combination of inputs: '%s'. Refer to the following usage and try again:", input));
		// 				System.out.println(usage);
		// 				System.out.print("> ");
		// 			}
		// 		} else {
		// 			System.out.println(String.format("Unknown combination of inputs: '%s'. Refer to the following usage and try again:", input));
		// 			System.out.println(usage);
		// 			System.out.print("> ");
		// 		}
		// 	} else if (command.equals("make")) {
		// 		if (tokens.hasMoreTokens()) {
		// 			String subcommand = tokens.nextToken();
		// 			if (subcommand.equals("pizzas")) {
		// 				System.out.println("Entering pizza mode...");
		// 				String pizzaModeUsage = """
		// 					| You're in the interactive pizza making mode. Pressing return will advance you through creating a randomly generated pizza.
		// 					| At any time if you wish to exit, use: "\uD83C\uDF55 exit" to exit pizza making mode. That's all you can do in this mode.
		// 					""";
		// 				System.out.println(pizzaModeUsage);
		// 				System.out.print("\uD83C\uDF55 ");
		// 				boolean hungry = true;
		// 				while (hungry) {
		// 					hungry = scanner.hasNextLine();
		// 					String advance = scanner.nextLine();
		// 					if (advance.isEmpty() || advance.equals("\\n")) {
		// 						// Make a pizza
		// 						System.out.println("ZAAA");
		// 						System.out.print("\uD83C\uDF55 ");
		// 					} else {
		// 						StringTokenizer pizzaTokens = new StringTokenizer(advance);
		// 						String pizzaCommand = pizzaTokens.nextToken();
		// 						if (pizzaCommand.equals("exit")) {
		// 							hungry = false;
		// 							System.out.println("Exiting pizza mode...");
		// 							System.out.print("> ");
		// 						} else {
		// 							System.out.println(String.format("Unknown combination of inputs: '%s'. Refer to the following usage and try again:", advance));
		// 							System.out.println(pizzaModeUsage);
		// 							System.out.print("\uD83C\uDF55 ");
		// 						}
		// 					}
		// 				}
						
		// 			} else {
		// 				System.out.println(String.format("Unknown combination of inputs: '%s'. Refer to the following usage and try again:", input));
		// 				System.out.println(usage);
		// 				System.out.print("> ");
		// 			}
		// 		} else {
		// 			System.out.println(String.format("Unknown combination of inputs: '%s'. Refer to the following usage and try again:", input));
		// 			System.out.println(usage);
		// 			System.out.print("> ");
		// 		}
		// 	} else if (command.equals("empty")) {
		// 		System.out.println(String.format("Unknown combination of inputs: '%s'. Refer to the following usage and try again:", ""));
		// 		System.out.println(usage);
		// 		System.out.print("> ");
		// 	}
		// 	else {
		// 		System.out.println(String.format("Unknown combination of inputs: '%s'. Refer to the following usage and try again:", input));
		// 		System.out.println(usage);
		// 		System.out.print("> ");
		// 	}
		// }
		// Runtime.getRuntime().addShutdownHook(new Thread(() -> {
		// 	System.out.println("Rocked that set");
		// 	scanner.close();
		// }));