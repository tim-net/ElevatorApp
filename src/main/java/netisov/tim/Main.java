package netisov.tim;

import java.io.Console;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {

  public static void main(String[] args) {
    int speed = Integer.parseInt(args[0]);
    int floorHeight = Integer.parseInt(args[1]);
    int doorTimeout = Integer.parseInt(args[2]);
    Console console = System.console();
    if (console != null) {

      ExecutorService executorService = Executors.newSingleThreadExecutor();
      executorService.submit(new Elevator(speed, floorHeight, doorTimeout));
      String cmd;
      while (true) {
        cmd = console.readLine("Enter Command a or b");
        if ("a".equals(cmd) || "b".equals(cmd)) break;
        else
          console.readLine("Command is wrong");
      }
        if("a".equals(cmd)) {
        callElevator();
        }

    }
  }

  private static void callElevator() {

  }
}
