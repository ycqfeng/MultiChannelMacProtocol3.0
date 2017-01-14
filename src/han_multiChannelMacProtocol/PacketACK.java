package han_multiChannelMacProtocol;

/**
 * Created by ycqfeng on 2017/1/14.
 */
public class PacketACK extends Packet{

    public PacketACK(int length){
        this.length = length;
        this.packetType = PacketType.ACK;
    }
}
