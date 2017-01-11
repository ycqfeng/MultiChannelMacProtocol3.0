package han_simulator;

/**
 * Created by ycqfeng on 2017/1/11.
 */
public interface IF_Simulator {
    default void simulatorStart(){
        Hprint.printlnt("IF_simulator的simulatorStart接口未实现");
    }
    default void simulatorEnd(){
        Hprint.printlnt("IF_simulator的simulatorEnd接口未实现");
    }
}
