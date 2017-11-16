package netisov.tim;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntFunction;

public final class Elevator implements Runnable {
  private int currentFloor = 1;
  private final int millisecondsPerFloor;
  private final int doorTimeout;
  private List<Consumer<Integer>> passFloorListeners = new ArrayList<>();


  public Elevator(int speed, int floorHeight, int doorTimeout) {
    this.millisecondsPerFloor = floorHeight / speed;
    this.doorTimeout = doorTimeout;
  }

  private void openDoor() {

  }

  private void closeDoor() {
  }

  private void goTo(int floor) throws InterruptedException {
    IntFunction<Integer> operationResult = i -> floor < currentFloor ? i-- : i++;
    while (currentFloor != floor) {
      firePassFloorEvent(floor);
      Thread.sleep(millisecondsPerFloor);
      currentFloor = operationResult.apply(currentFloor);
    }
  }

  public void addPassFloorListener(Consumer<Integer> listener) {
    passFloorListeners.add(listener);
  }

  private void firePassFloorEvent(int floor) {
    passFloorListeners.forEach(l -> l.accept(floor));
  }

  @Override
  public void run() {

  }
}
