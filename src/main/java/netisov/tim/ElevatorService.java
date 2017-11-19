package netisov.tim;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

/**
 * Service class to get elevator kicked off.
 */
class ElevatorService {
  private Elevator elevator;

  /**
   * Create an elevator with specified properties.
   *
   * @param speed       meters per second
   * @param floorHeight height of a floor in meters
   * @param doorTimeout timeout opening/closing door in seconds.
   */
  void createElevator(int speed, int floorHeight, int doorTimeout) {
    ExecutorService executorService = Executors.newSingleThreadExecutor();
    elevator = new Elevator(speed, floorHeight, doorTimeout);
    elevator.addPassFloorListener(i -> System.out.printf("Passing %d floor %s", i, System.lineSeparator()));
    elevator.addOpenDoorListener(i -> System.out.printf("Opening door on %d floor %s", i, System.lineSeparator()));
    elevator.addCloseDoorListener(i -> System.out.printf("Closing door on %d floor %s", i, System.lineSeparator()));
    executorService.submit(elevator);
  }

  /**
   * Calling elevator from floors.
   *
   * @param callFromFloors
   * @throws InterruptedException
   */
  void callElevator(List<Integer> callFromFloors) throws InterruptedException {
    elevator.callFrom(callFromFloors);

  }

  /**
   * Make elevator go from inside of it.
   *
   * @param goToFloors
   * @throws InterruptedException
   */
  void elevatorGoTo(List<Integer> goToFloors) throws InterruptedException {
    elevator.pressedFloorButtons(goToFloors);
  }

  /**
   * Specify action on arrival on floor, somebody'd want to get in,
   * or are there just little kids making some pranks?
   *
   * @param supplier
   */
  void setActionOnFloorArrival(Supplier<Boolean> supplier) {
    elevator.setEnterOnFloorObserver(supplier);
  }

}
