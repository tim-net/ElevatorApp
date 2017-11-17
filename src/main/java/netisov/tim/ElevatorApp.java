package netisov.tim;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.UnrecognizedOptionException;

import java.io.Console;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ElevatorApp {
  private static final int DEFAULT_SPEED = 2;
  private static final int DEFAULT_FLOOR_HEIGHT = 4;
  private static final int DEFAULT_DOOR_TIMEOUT = 2;
  private CommandLine cmdLine;
  private final Options options;
  private Option printHelpOption;
  private Option speedOption;
  private Option floorHeightOption;
  private Option doorTimeoutOption;
  private volatile String choice;
  private volatile boolean finished;

  public static void main(String[] args) throws InterruptedException {

    ElevatorApp app = new ElevatorApp();
    int exitCode = app.run(args);
    System.exit(exitCode);
  }

  private int run(String[] args) throws InterruptedException {

    // check if there are no command-line arguments
    if (args.length == 0) {
      printHelp();
      return 0;
    }
    if (!initCmdLine(args)) return 1;

    // check if help is requested
    if (cmdLine.hasOption(printHelpOption.getLongOpt())) {
      printHelp();
      return 0;
    }

    Integer speed = getOptionValue(speedOption) != null ? Integer.parseInt(getOptionValue(speedOption)) : DEFAULT_SPEED;
    Integer floorHeight = getOptionValue(floorHeightOption) != null ? Integer.parseInt(getOptionValue(floorHeightOption)) : DEFAULT_FLOOR_HEIGHT;
    Integer doorTimeout = getOptionValue(doorTimeoutOption) != null ? Integer.parseInt(getOptionValue(doorTimeoutOption)) : DEFAULT_DOOR_TIMEOUT;

    ElevatorService service = new ElevatorService();
    service.createElevator(speed, floorHeight, doorTimeout);

    Console console = System.console();

    Runtime.getRuntime().addShutdownHook(new Thread(() -> finished = true));

//todo refactor this shit
    Supplier<Boolean> actionOnFloorArrival = () -> {
      String response = console.readLine("Enter y if you want to get in elevator or n to make " +
          "elevator continue its tour, default is n" + System.lineSeparator());
      if ("y".equals(response)) {
        choice = "2";
        return true;
      }
      return false;
    };
    service.setActionOnFloorArrival(actionOnFloorArrival);

    while (!finished) {
      if (choice == null) {
        choice = console.readLine("Enter 1 to call elevator and 2 to go to floors " + System.lineSeparator());
      }

      String errorMessage = "Unable to get any floors, try once more time";
      switch (choice) {
        case "1":

          List<Integer> floorsCall = readInputInts(console,
              "Enter floor numbers from where elevator is called separated with a space then press Enter key" + System.lineSeparator());
          if (floorsCall.isEmpty()) {
            console.writer().print(errorMessage);
          } else {
            service.callElevator(floorsCall);
          }
          resetChoice();
          break;
        case "2":
          List<Integer> floorsToGo = readInputInts(console,
              "Enter floor numbers  where elevator will go separated with a space then press Enter key" + System.lineSeparator());
          if (floorsToGo.isEmpty()) {
            console.writer().print(errorMessage);
          } else {
            service.elevatorGoTo(floorsToGo);
          }
          resetChoice();
          break;
        default:
          console.writer().print(errorMessage);
          resetChoice();
          break;
      }
    }

    return 0;
  }

  private void resetChoice() {
    choice = null;
  }

  private boolean initCmdLine(String[] args) {
    // parse command-line options
    CommandLineParser parser = new DefaultParser();

    try {
      cmdLine = parser.parse(options, args);
    } catch (MissingOptionException e) {
      System.err.println("Missing required options:");
      for (Object option : e.getMissingOptions()) {
        System.out.println(option);
      }
      printHelp();
      return true;
    } catch (MissingArgumentException e) {
      System.err.printf("Option requires an argument: %s%s", e.getOption(), System.lineSeparator());
      printHelp();
      return true;
    } catch (UnrecognizedOptionException e) {
      System.err.printf("Unrecognized option: %s%s", e.getOption(), System.lineSeparator());
      printHelp();
      return true;
    } catch (ParseException e) {
      System.err.printf("Failed to parse command-line options: %s%s", e.getMessage(), System.lineSeparator());
      printHelp();
      return true;
    }
    if (!cmdLine.getArgList().isEmpty()) {
      System.err.println("The following command-line options could not be parsed:");
      for (Object option : cmdLine.getArgList()) {
        System.out.println(option);
      }
      printHelp();
      return false;
    }
    return true;
  }

  private List<Integer> readInputInts(Console console, String description) {
    return Arrays.stream(console.readLine(description)
        .split(" "))
        .map(String::trim)
        .map(Integer::parseInt)
        .collect(Collectors.toList());
  }


  public ElevatorApp() {
    options = initOptions();
  }

  /**
   * Initializes command options.
   *
   * @return initializes {@link Options} object
   */
  private Options initOptions() {
    Options opts = new Options();

    // print help
    printHelpOption = Option.builder("h").longOpt("help")
        .desc("print this help message").build();
    opts.addOption(printHelpOption);

    //speed option
    speedOption = Option.builder("s").longOpt("speed")
        .desc("Speed as meters per second, defaults to " + DEFAULT_SPEED)
        .hasArg()
        .build();

    opts.addOption(speedOption);

    //floor height option
    floorHeightOption = Option.builder("fh").longOpt("floor-height")
        .desc("Floor height as meters, defaults to " + DEFAULT_FLOOR_HEIGHT)
        .hasArg()
        .build();

    opts.addOption(floorHeightOption);


    //doors timeout option
    doorTimeoutOption = Option.builder("dt").longOpt("door-timeout")
        .desc("Doors timeout as seconds, defaults to " + DEFAULT_DOOR_TIMEOUT)
        .hasArg()
        .build();

    opts.addOption(doorTimeoutOption);

    return opts;
  }

  private String getOptionValue(Option option) {
    return cmdLine.getOptionValue(getOptionKey(option));
  }

  private String getOptionKey(Option option) {
    return option.getLongOpt() != null ? option.getLongOpt() : option.getOpt();
  }

  /**
   * Prints help for current command.
   */
  private void printHelp() {
    HelpFormatter helpFormatter = new HelpFormatter();
    helpFormatter.printHelp("Elevator [<options>]",
        "Elevator app simulates an elevator", options, null);
  }
}
