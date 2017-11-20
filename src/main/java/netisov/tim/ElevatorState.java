package netisov.tim;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ElevatorState {

  public static final Integer DEFAULT_FLOOR = 1;
  private Integer currentFloor = DEFAULT_FLOOR;
  private boolean doorOpened = false;

}
