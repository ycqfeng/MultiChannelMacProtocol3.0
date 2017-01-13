package han_multiChannelMacProtocol;

/**
 * Created by ycqfeng on 2017/1/13.
 */
public class PacketDATA  extends Packet{
    Packet data;
    public PacketDATA(int lengthPacketDATA){
        super(lengthPacketDATA, PacketType.DATA);
        this.data = new Packet(lengthPacketDATA, PacketType.PACKET);
    }
    public PacketDATA(Packet data){
        super(data.length, PacketType.DATA);
        this.data = data;
    }
}
