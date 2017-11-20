package netisov.tim.tests;

import netisov.tim.Elevator;
import netisov.tim.ElevatorState;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Timofey Netisov <tnetisov@amt.ru>
 */
@RunWith(JUnit4.class)
public class ElevatorTest {
  ExecutorService executorService = Executors.newSingleThreadExecutor();
  private final int speed = 20;
  private final int floorHeight = 2;
  private final int doorTimeout = 1;

  @Test
  public void testGoToFloor() {
    Elevator elevator = getElevator();
    Integer floor = 3;
    elevator.pressedFloorButtons(Stream.of(floor).collect(Collectors.toList()));
    Assert.assertEquals(floor, elevator.getState().getCurrentFloor());
  }


  @Test
  public void testGoToEmptyList() {
    Elevator elevator = getElevator();
    elevator.pressedFloorButtons(new ArrayList<>());
    Assert.assertEquals(ElevatorState.DEFAULT_FLOOR, elevator.getState().getCurrentFloor());
  }


  @Test
  public void testGoToFloors() {

    Elevator elevator = getElevator();

    elevator.pressedFloorButtons(Stream.of(1).collect(Collectors.toList()));

    List<Integer> floors = Stream.of(2, 5, 2).collect(Collectors.toList());

    List<Integer> expected = Stream.of(1, 2, 3, 4).collect(Collectors.toList());
    List<Integer> passed = new ArrayList<>();
    elevator.addPassFloorListener(passed::add);

    elevator.pressedFloorButtons(floors);
    Assert.assertEquals(expected, passed);
  }

  @Test
  public void testGoToFloorsFirstCalledDownPriority() {
    Elevator elevator = getElevator();
    List<Integer> floorsFirst = Stream.of(3).collect(Collectors.toList());

    elevator.pressedFloorButtons(floorsFirst);

    List<Integer> next = Stream.of(1, 4, 5).collect(Collectors.toList());

    List<Integer> expected = Stream.of(3, 2, 1, 2, 3, 4).collect(Collectors.toList());
    List<Integer> passed = new ArrayList<>();
    elevator.addPassFloorListener(passed::add);

    elevator.pressedFloorButtons(next);
    Assert.assertEquals(expected, passed);
  }

  @Test
  public void testGoToFloorsFirstCalledUpPriority() {
    Elevator elevator = getElevator();
    List<Integer> floorsFirst = Stream.of(3).collect(Collectors.toList());

    elevator.pressedFloorButtons(floorsFirst);

    List<Integer> next = Stream.of(5, 1, 4).collect(Collectors.toList());

    List<Integer> expected = Stream.of(3, 4, 5, 4, 3, 2).collect(Collectors.toList());
    List<Integer> passed = new ArrayList<>();
    elevator.addPassFloorListener(passed::add);

    elevator.pressedFloorButtons(next);
    Assert.assertEquals(expected, passed);
  }

  @Test
  public void testGoToFloorsFirstCalledDown() {
    Elevator elevator = getElevator();
    List<Integer> floorsFirst = Stream.of(3).collect(Collectors.toList());

    elevator.pressedFloorButtons(floorsFirst);

    List<Integer> next = Stream.of(1, 2).collect(Collectors.toList());

    List<Integer> expected = Stream.of(3, 2).collect(Collectors.toList());
    List<Integer> passed = new ArrayList<>();
    elevator.addPassFloorListener(passed::add);

    elevator.pressedFloorButtons(next);
    Assert.assertEquals(expected, passed);
  }

  @Test
  public void testGoToFloorsPassingBy() throws InterruptedException {
    // assume that elevator goes from 1st floor
    Elevator elevator = getElevator();
    List<Integer> floorsFirst = Stream.of(1).collect(Collectors.toList());

    elevator.pressedFloorButtons(floorsFirst);

    List<Integer> expectedOpenDoorFloors = Stream.of(2, 4).collect(Collectors.toList());
    List<Integer> actualOpenDoorFloors = new ArrayList<>();

    elevator.addOpenDoorListener(actualOpenDoorFloors::add);

    List<Integer> expectedCloseDoorFloors = Stream.of(2, 4).collect(Collectors.toList());
    List<Integer> actualCloseDoorFloors = new ArrayList<>();

    elevator.addCloseDoorListener(actualCloseDoorFloors::add);


    List<Integer> next = Stream.of(4, 2).collect(Collectors.toList());

    List<Integer> expected = Stream.of(1, 2, 3).collect(Collectors.toList());
    List<Integer> passed = new ArrayList<>();
    elevator.addPassFloorListener(passed::add);

    Instant from = Instant.now();

    elevator.pressedFloorButtons(next);


    Duration duration = Duration.between(from, Instant.now());
    float expectedTime = (((float) floorHeight / (float) speed) * 3) + 4 * doorTimeout;
    Assert.assertEquals(expectedTime * 1000, (float) duration.toMillis(), 20.0);
    // give 20 milliseconds for runtime expenses


    Assert.assertEquals(expected, passed);
    Assert.assertEquals(expectedOpenDoorFloors, actualOpenDoorFloors);
    Assert.assertEquals(expectedCloseDoorFloors, actualCloseDoorFloors);
  }

  private Elevator getElevator() {
    Elevator elevator = new Elevator(speed, floorHeight, doorTimeout);
    executorService.submit(elevator);
    return elevator;
  }
}
