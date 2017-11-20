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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Main entry point class.
 */
public class ElevatorApp {
  public static final int DEFAULT_SPEED = 2;
  public static final int DEFAULT_FLOOR_HEIGHT = 4;
  public static final int DEFAULT_DOOR_TIMEOUT = 2;
  private CommandLine cmdLine;
  private final Options options;
  private Option printHelpOption;
  private Option speedOption;
  private Option floorHeightOption;
  private Option doorTimeoutOption;
  private volatile boolean finished;
  private final ElevatorService service;
  private Console console = System.console();

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

    service.createElevator(speed, floorHeight, doorTimeout);


    Runtime.getRuntime().addShutdownHook(new Thread(() -> finished = true));


    UserActions userActions = new UserActions();

    Supplier<Boolean> actionOnFloorArrival = () -> {
      String response = console.readLine("Enter y if you want to get in elevator or n to make " +
          "elevator continue its tour, default is n" + System.lineSeparator());
      if ("y".equals(response)) {
        try {
          userActions.proceedUserAction(Action.GOTO_FLOORS);
        } catch (InterruptedException e) {
          return false;
        }
        return true;
      }
      return false;
    };
    service.setActionOnFloorArrival(actionOnFloorArrival);
    while (!finished) {
      String choice = console.readLine("Enter c to call elevator or g to go to floors if you are inside or q to quit" + System.lineSeparator());
      userActions.proceedUserAction(Action.getByShortcut(choice));
    }

    return 0;
  }

  /**
   * Representing user commands in cli
   */
  enum Action {
    UNKNOWN(null),
    QUIT("q"),
    CALL_ELEVATOR("c"),
    GOTO_FLOORS("g");
    private String shortcut;

    Action(String shortcut) {
      this.shortcut = shortcut;
    }

    static Action getByShortcut(String shortcut) {
      Optional<Action> optional = Arrays.stream(Action.values())
          .filter(a -> a.shortcut != null && a.shortcut.equals(shortcut))
          .findAny();
      return optional.orElse(UNKNOWN);
    }
  }

  /**
   * Call service for each user action
   * beside of unknown action.
   */
  class UserActions {

    String errorMessage = "Unable to get any floors, try once more time";

    void proceedUserAction(Action action) throws InterruptedException {
      switch (action) {
        case CALL_ELEVATOR:
          List<Integer> floorsCall = readInputInts(console,
              "Enter floor numbers from where elevator is called separated with a space then press Enter key" + System.lineSeparator());
          if (floorsCall.isEmpty()) {
            System.out.println(errorMessage);
            proceedUserAction(Action.CALL_ELEVATOR);
          } else {
            service.callElevator(floorsCall);
          }
          break;
        case GOTO_FLOORS:
          List<Integer> floorsToGo = readInputInts(console,
              "Enter floor numbers  where elevator will go separated with a space then press Enter key" + System.lineSeparator());
          if (floorsToGo.isEmpty()) {
            System.out.println(errorMessage);
            proceedUserAction(Action.GOTO_FLOORS);
          } else {
            service.elevatorGoTo(floorsToGo);
          }
          break;
        case QUIT:
          System.exit(0);
          break;
        case UNKNOWN:
        default:
          System.out.println(errorMessage);
      }
    }
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

  /**
   * Read user input of floor numbers and parse it into list of integers.
   *
   * @param console
   * @param description
   * @return
   */
  private List<Integer> readInputInts(Console console, String description) {
    try {
      return Arrays.stream(console.readLine(description)
          .split(" "))
          .map(String::trim)
          .map(Integer::parseInt)
          .collect(Collectors.toList());
    } catch (NumberFormatException e) {
      return new ArrayList<>();
    }
  }


  public ElevatorApp() {
    this.service = new ElevatorService();
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
