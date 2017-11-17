package netisov.tim;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Supplier;

public final class Elevator implements Runnable {
  private int currentFloor = 1;
  private final int millisecondsPerFloor;
  private final int doorTimeout;
  private List<Consumer<Integer>> passFloorListeners = new ArrayList<>();
  private List<Consumer<Integer>> openDoorListeners = new ArrayList<>();
  private List<Consumer<Integer>> closeDoorListeners = new ArrayList<>();
  private Supplier<Boolean> enterOnFloorObserve;
  private List<Integer> floorsCalledFrom = new ArrayList<>();


  public Elevator(int speed, int floorHeight, int doorTimeout) {
    this.millisecondsPerFloor = (floorHeight / speed) * 1000;
    this.doorTimeout = doorTimeout;
  }

  public void callFrom(List<Integer> floors) throws InterruptedException {
    int passedFloors = 0;
    int initialFloorSize = floors.size();
    floors.sort(Comparator.naturalOrder());
    while (passedFloors != initialFloorSize) {
      Integer floor = findNearestFloor(currentFloor, floors.toArray(new Integer[floors.size()]));
      goTo(floor);
      passedFloors++;
      floors.remove(floor);
      openDoor();
      if (enterOnFloorObserve != null && enterOnFloorObserve.get()) {
        // user requested to get in elevator on floor
        floorsCalledFrom = floors;
        closeDoor();
        break;
      }
    }
  }

  public void goToFloors(List<Integer> floors) throws InterruptedException {

    int firstPressed = floors.get(0);
    floors.sort(Comparator.naturalOrder());

    List<Integer> sub = floors.subList(Collections.binarySearch(floors, firstPressed), floors.size());

    List<Integer> upper = new ArrayList<>(sub);
    sub.clear();


    floors.sort(Comparator.reverseOrder());

    Deque<Integer> upperDeque = new ArrayDeque<>(upper);
    Deque<Integer> lowerDeque = new ArrayDeque<>(floors);

    Deque<Integer> deque = new ArrayDeque<>();
    if ((Math.abs(upperDeque.peekFirst() - currentFloor) - Math.abs(lowerDeque.peekFirst() - currentFloor)) > 0) {
      deque.addAll(lowerDeque);
      deque.addAll(upperDeque);
    } else {
      deque.addAll(upperDeque);
      deque.addAll(lowerDeque);
    }
    Integer floorToGo;
    while ((floorToGo = deque.poll()) != null) {
      goTo(floorToGo);
      openDoor();//todo use case when entered on floor and pressed another floors buttons, add to this list, may be even reconsider the list
      closeDoor();
    }

    if (!floorsCalledFrom.isEmpty()) {
      // there are not served calls from floors left
      callFrom(floorsCalledFrom);
    }

  }

  private void openDoor() throws InterruptedException {
    fireOpenDoorEvent(currentFloor);
    Thread.sleep(doorTimeout * 1000);
  }

  private void closeDoor() throws InterruptedException {
    fireCloseDoorEvent(currentFloor);
    Thread.sleep(doorTimeout * 1000);
  }

  private void goTo(int floor) throws InterruptedException {
    IntFunction<Integer> operationResult = i -> floor < currentFloor ? --i : ++i;
    while (currentFloor != floor) {
      firePassFloorEvent(currentFloor);
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


  public void addOpenDoorListener(Consumer<Integer> listener) {
    openDoorListeners.add(listener);
  }

  private void fireOpenDoorEvent(int floor) {
    openDoorListeners.forEach(l -> l.accept(floor));
  }


  public void addCloseDoorListener(Consumer<Integer> listener) {
    closeDoorListeners.add(listener);
  }

  private void fireCloseDoorEvent(int floor) {
    closeDoorListeners.forEach(l -> l.accept(floor));
  }

  @Override
  public void run() {
    //idling
  }

  private int findNearestFloor(int value, Integer[] a) {
    if (value < a[0]) {
      return a[0];
    }
    if (value > a[a.length - 1]) {
      return a[a.length - 1];
    }

    int lo = 0;
    int hi = a.length - 1;

    while (lo <= hi) {
      int mid = (hi + lo) / 2;

      if (value < a[mid]) {
        hi = mid - 1;
      } else if (value > a[mid]) {
        lo = mid + 1;
      } else {
        return a[mid];
      }
    }
    return (a[lo] - value) < (value - a[hi]) ? a[lo] : a[hi];
  }

  public void setEnterOnFloorObserve(Supplier<Boolean> enterOnFloorObserve) {
    this.enterOnFloorObserve = enterOnFloorObserve;
  }
}
