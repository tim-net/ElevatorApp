package netisov.tim;

import java.io.Console;
import java.util.List;

public class Main {

  public static void main(String[] args) {

    Console console = System.console();
    ElevatorService elevatorService = new ElevatorService();
    elevatorService.createElevator(args);
    if (console != null) {


      String cmd;
      while (true) {
        cmd = console.readLine("Enter Command a or b");
        if ("a".equals(cmd) || "b".equals(cmd)) break;
        else
          console.readLine("Command is wrong");
      }
      if ("a".equals(cmd)) {
        List<Integer> callFromFloors = callElevator(console);
        elevatorService.callElevator(callFromFloors);
      }

    }
  }

  private static List<Integer> callElevator(Console console) {

    return null;
  }
}
