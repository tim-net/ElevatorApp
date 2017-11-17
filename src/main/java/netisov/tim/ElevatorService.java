package netisov.tim;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;


public class ElevatorService {
  private Elevator elevator;


  public void createElevator(int speed, int floorHeight, int doorTimeout) {
    ExecutorService executorService = Executors.newSingleThreadExecutor();
    elevator = new Elevator(speed, floorHeight, doorTimeout);
    elevator.addPassFloorListener(i -> System.out.printf("Passing %d floor %s", i, System.lineSeparator()));
    elevator.addOpenDoorListener(i -> System.out.printf("Opening door on %d floor %s", i, System.lineSeparator()));
    elevator.addCloseDoorListener(i -> System.out.printf("Closing door on %d floor %s", i, System.lineSeparator()));
    executorService.submit(elevator);
  }

  public void callElevator(List<Integer> callFromFloors) throws InterruptedException {
    elevator.callFrom(callFromFloors);

  }

  public void elevatorGoTo(List<Integer> goToFloors) throws InterruptedException {
    elevator.goToFloors(goToFloors);
  }

  public void setActionOnFloorArrival(Supplier<Boolean> supplier) {
    elevator.setEnterOnFloorObserve(supplier);
  }

}
