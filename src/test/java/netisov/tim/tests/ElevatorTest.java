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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static netisov.tim.ElevatorState.DEFAULT_FLOOR;

/**
 * @author Timofey Netisov <tnetisov@amt.ru>
 */
@RunWith(JUnit4.class)
public class ElevatorTest {
  private final int speed = 20;
  private final int floorHeight = 2;
  private final int doorTimeout = 1;


  @Test
  public void createElevatorTest() {
    Elevator elevator = getElevator();
    Assert.assertEquals(DEFAULT_FLOOR, elevator.getState().getCurrentFloor());
  }

  @Test
  public void callElevatorFromOneFloor() throws InterruptedException {
    Elevator elevator = getElevator();
    Integer floorToGo = 3;
    Instant from = Instant.now();
    elevator.callFrom(Stream.of(floorToGo).collect(Collectors.toList()));
    Duration duration = Duration.between(from, Instant.now());
    float expectedTime = (((float) floorHeight * (floorToGo - DEFAULT_FLOOR) / (float) speed)) + 2 * doorTimeout;
    Assert.assertEquals(expectedTime * 1000, (float) duration.toMillis(), 100.0);

    // give 100 milliseconds for runtime expenses because of search nearest floors
    Assert.assertEquals(floorToGo, elevator.getState().getCurrentFloor());
  }

  @Test
  public void callElevatorFromEmptyFloorList() throws InterruptedException {
    Elevator elevator = getElevator();
    elevator.callFrom(new ArrayList<>());
    // give 100 milliseconds for runtime expenses because of search nearest floors
    Assert.assertEquals(ElevatorState.DEFAULT_FLOOR, elevator.getState().getCurrentFloor());
  }

  @Test
  public void callElevatorTestEnterOnFloorObserver() throws InterruptedException {
    Elevator elevator = getElevator();
    elevator.callFrom(Stream.of(1).collect(Collectors.toList()));


    List<Integer> expectedPath = new ArrayList<>();
    expectedPath.add(1);
    expectedPath.add(2);
    expectedPath.add(1);
    expectedPath.add(2);
    expectedPath.add(3);

    List<Integer> actualPath = new ArrayList<>();
    elevator.addPassFloorListener(actualPath::add);

    elevator.setEnterOnFloorObserver(() -> {
      if (elevator.getState().getCurrentFloor().equals(2)) {
        elevator.pressedFloorButtons(Stream.of(1).collect(Collectors.toList()));
        return true;
      } else {
        return false;
      }
    });

    elevator.callFrom(Stream.of(2, 4).collect(Collectors.toList()));


    Assert.assertEquals(new Integer(4), elevator.getState().getCurrentFloor());
    Assert.assertArrayEquals(expectedPath.toArray(), actualPath.toArray());
  }

  @Test
  public void callElevatorFromSeveralFloors() throws InterruptedException {
    Elevator elevator = getElevator();
    Integer first = 3;
    Integer second = 5;
    int initialFloorToCheckDuration = elevator.getState().getCurrentFloor();
    AtomicInteger initialFloor = new AtomicInteger(elevator.getState().getCurrentFloor());
    elevator.addPassFloorListener(f -> Assert.assertEquals((long) initialFloor.getAndIncrement(), (long) f));
    AtomicInteger countOpenDoor = new AtomicInteger(0);

    elevator.addOpenDoorListener(f -> {
      int num = countOpenDoor.incrementAndGet();
      if (num == 1) {
        Assert.assertEquals((long) first, (long) f);
      } else {
        Assert.assertEquals((long) second, (long) f);
      }
    });

    AtomicInteger countCloseDoor = new AtomicInteger(0);

    elevator.addCloseDoorListener(f -> {
      int num = countCloseDoor.incrementAndGet();
      if (num == 1) {
        Assert.assertEquals((long) first, (long) f);
      } else {
        Assert.assertEquals((long) second, (long) f);
      }
    });


    Instant beforeInstant = Instant.now();

    elevator.callFrom(Stream.of(first, second).collect(Collectors.toList()));

    Duration duration = Duration.between(beforeInstant, Instant.now());

    float elevatorShouldArrive = ((float) ((second - initialFloorToCheckDuration) * floorHeight) / (float) speed +
        doorTimeout * countOpenDoor.get() + doorTimeout * countCloseDoor.get());

    Assert.assertEquals(duration.toMillis(), elevatorShouldArrive * 1000, 100.0);
    Assert.assertEquals(second, elevator.getState().getCurrentFloor());
  }

  @Test
  public void callElevatorFromFloorsAndExpectingToComeToNearest() throws InterruptedException {
    Elevator elevator = getElevator();
    Integer first = 3;
    Integer nearest = 4;
    Integer farthest = 1;


    elevator.callFrom(Stream.of(first).collect(Collectors.toList()));

    List<Integer> expected = new ArrayList<>();
    // expecting to go first to nearest because we don't have any floors between , then back to first
    // because farthest is lower than first position and then to farthest through one intermediate floor (2)
    expected.add(first);
    expected.add(nearest);
    expected.add(first);
    expected.add(first - 1);

    List<Integer> actual = new ArrayList<>();

    elevator.addPassFloorListener(actual::add);


    elevator.callFrom(Stream.of(nearest, farthest).collect(Collectors.toList()));


    Assert.assertArrayEquals(expected.toArray(), actual.toArray());

    Assert.assertEquals(farthest, elevator.getState().getCurrentFloor());


  }

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
    Assert.assertEquals(DEFAULT_FLOOR, elevator.getState().getCurrentFloor());
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
    return new Elevator(speed, floorHeight, doorTimeout);
  }
}
