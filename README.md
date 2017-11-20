# Elevator simulation Application
Call it like this: 
java -jar Elevator-1.0-SNAPSHOT-jar-with-dependencies.jar -s 2 -fh 3 -dt 1 -nf 10
Where s - speed of elevator in meters per second, fh - floor height in meters, 
dt - timeout of door opening/closing in seconds, -nf - number of floors.
For more information call with flag -h or --help
**Description:**
When elevator is called from floors, it goes to the nearest floor, 
when elevator arrives at floor, you can enter in elevator and go to desired floor by pressing buttons. 
Floor list is a sequence of numbers separated with a space.
When buttons of floors are pressed inside of elevator, it is at bigger priority than those pressed on floors.
Be careful with such machinery and have a nice day :)  