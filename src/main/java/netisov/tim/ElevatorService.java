package netisov.tim;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ElevatorService {
  private Elevator elevator;


  public void createElevator(String[] args) {
    int speed = Integer.parseInt(args[0]);
    int floorHeight = Integer.parseInt(args[1]);
    int doorTimeout = Integer.parseInt(args[2]);
    ExecutorService executorService = Executors.newSingleThreadExecutor();
    elevator = new Elevator(speed, floorHeight, doorTimeout);
    elevator.addPassFloorListener(i-> System.out.printf("Passing %d floor", i));
    executorService.submit(elevator);
  }

  public void callElevator(List<Integer> callFromFloors) {

  }
}
