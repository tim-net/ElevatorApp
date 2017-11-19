package netisov.tim;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ElevatorState {

  public static final int DEFAULT_FLOOR = 1;
  private int currentFloor = DEFAULT_FLOOR;

}
