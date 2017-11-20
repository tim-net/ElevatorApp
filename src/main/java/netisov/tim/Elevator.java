package netisov.tim;

import lombok.Getter;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Class representing an elevator.
 */
public final class Elevator {
  @Getter
  private final ElevatorState state = new ElevatorState();
  private final int millisecondsPerFloor;
  private final int doorTimeout;
  private List<Consumer<Integer>> passFloorListeners = new ArrayList<>();
  private List<Consumer<Integer>> openDoorListeners = new ArrayList<>();
  private List<Consumer<Integer>> closeDoorListeners = new ArrayList<>();
  private Supplier<Boolean> enterOnFloorObserve;


  public Elevator(int speed, int floorHeight, int doorTimeout) {
    this.millisecondsPerFloor = (int) Math.floor(((double) floorHeight / (double) speed) * 1000);
    this.doorTimeout = doorTimeout;

  }

  /**
   * Elevator is called from floors
   * going for to get those people from troubles.
   *
   * @param floors list of floors where from elevator is called.
   * @throws InterruptedException
   */
  public void callFrom(List<Integer> floors) throws InterruptedException {
    int passedFloors = 0;
    int initialFloorSize = floors.size();
    if (floors.isEmpty()) {
      return;
    }
    floors = floors.stream().distinct().collect(Collectors.toList());
    floors.sort(Comparator.naturalOrder());
    while (passedFloors != initialFloorSize) {
      Integer floor = findNearestFloor(state.getCurrentFloor(), floors.toArray(new Integer[floors.size()]));
      goTo(floor);
      passedFloors++;
      floors.remove(floor);
      openDoor();
      if (enterOnFloorObserve != null && enterOnFloorObserve.get()) {
        // user requested to get in elevator on floor
        callFrom(floors);
        break;
      }
      closeDoor();
    }
  }

  /**
   * Go to floors in particular order.
   *
   * @param floors
   */
  private void goToFloors(List<Integer> floors) {
    floors.forEach(f -> {
      goTo(f);
      openDoor();
      closeDoor();
    });
  }

  /**
   * Someone pressed the buttons inside of elevator
   * and we're going to get him where he wants
   *
   * @param floors
   */
  public void pressedFloorButtons(List<Integer> floors) {
    floors = floors.stream().distinct().collect(Collectors.toList());
    if (state.isDoorOpened()) {
      closeDoor();
    } else if (floors.contains(state.getCurrentFloor())) {
      openDoor();
      closeDoor();
    }
    floors.remove(state.getCurrentFloor());
    if (floors.isEmpty()) {
      return;
    }

    int firstPressed = floors.get(0);
    if (floors.size() == 1) {
      goTo(firstPressed);
      openDoor();
      closeDoor();
      return;
    }


    floors.sort(Comparator.naturalOrder());
    if (state.getCurrentFloor() > floors.get(floors.size() - 1)) {
      floors.sort(Comparator.reverseOrder());
      goToFloors(floors);
    } else if (state.getCurrentFloor() < floors.get(0)) {
      goToFloors(floors);
    } else {
      List<Integer> sub = floors.subList(-Collections.binarySearch(floors, state.getCurrentFloor()) - 1, floors.size());

      List<Integer> upper = new ArrayList<>(sub);
      sub.clear();


      floors.sort(Comparator.reverseOrder());

      Deque<Integer> upperDeque = new ArrayDeque<>(upper);
      Deque<Integer> lowerDeque = new ArrayDeque<>(floors);

      Deque<Integer> deque = new ArrayDeque<>();
      if (lowerDeque.contains(firstPressed)) {
        deque.addAll(lowerDeque);
        deque.addAll(upperDeque);
      } else {
        deque.addAll(upperDeque);
        deque.addAll(lowerDeque);
      }
      Integer floorToGo;
      while ((floorToGo = deque.poll()) != null) {
        goTo(floorToGo);
        openDoor();
        closeDoor();
      }
    }
  }


  /**
   * Open door.
   */
  private void openDoor() {
    try {
      Thread.sleep(doorTimeout * 1000);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    state.setDoorOpened(true);
    fireOpenDoorEvent(state.getCurrentFloor());
  }

  /**
   * Close door.
   */
  private void closeDoor() {
    try {
      Thread.sleep(doorTimeout * 1000);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    state.setDoorOpened(false);
    fireCloseDoorEvent(state.getCurrentFloor());
  }

  /**
   * Go to one particular floor.
   *
   * @param floor number.
   */
  private void goTo(int floor) {
    IntFunction<Integer> operationResult = i -> floor < state.getCurrentFloor() ? --i : ++i;
    while (state.getCurrentFloor() != floor) {
      try {
        Thread.sleep(millisecondsPerFloor);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
      firePassFloorEvent(state.getCurrentFloor());
      state.setCurrentFloor(operationResult.apply(state.getCurrentFloor()));
    }
  }

  /**
   * Add listeners for passing floor event.
   *
   * @param listener
   */
  public void addPassFloorListener(Consumer<Integer> listener) {
    passFloorListeners.add(listener);
  }

  /**
   * Fire passing floor event to listeners.
   *
   * @param floor
   */
  private void firePassFloorEvent(int floor) {
    passFloorListeners.forEach(l -> l.accept(floor));
  }


  /**
   * Add listener for opening door event.
   *
   * @param listener instance
   */
  public void addOpenDoorListener(Consumer<Integer> listener) {
    openDoorListeners.add(listener);
  }

  /**
   * Fire opening door event to listeners.
   *
   * @param floor number
   */
  private void fireOpenDoorEvent(int floor) {
    openDoorListeners.forEach(l -> l.accept(floor));
  }

  /**
   * Add listener for closing door event.
   *
   * @param listener instance
   */
  public void addCloseDoorListener(Consumer<Integer> listener) {
    closeDoorListeners.add(listener);
  }

  /**
   * Fire closing door event to listeners.
   *
   * @param floor number
   */
  private void fireCloseDoorEvent(int floor) {
    closeDoorListeners.forEach(l -> l.accept(floor));
  }

  /**
   * Help method to find nearest element in array.
   *
   * @param value search value
   * @param a     array to search in
   * @return nearest value
   */
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

  /**
   * Set function to execute when elevator arrives
   * at floor, from which it has been called, so
   * a person who called it can decide what to do next.
   * If function returns true it means it's executed and
   * elevator can continue to go to other calls,
   * otherwise function did nothing.
   *
   * @param enterOnFloorObserve function to execute
   */
  public void setEnterOnFloorObserver(Supplier<Boolean> enterOnFloorObserve) {
    this.enterOnFloorObserve = enterOnFloorObserve;
  }

}
