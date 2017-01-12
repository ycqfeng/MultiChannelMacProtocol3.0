package han_multiChannelMacProtocol;

/**
 * Created by ycqfeng on 2017/1/12.
 */
public class PacketCTS extends Packet{
    private int lengthData;

    public PacketCTS(int length, int lengthData){
        this.length = length;
        this.packetType = PacketType.CTS;
        this.lengthData = lengthData;
    }
    public PacketCTS(int length, Packet packet){
        this.length = length;
        this.packetType = PacketType.CTS;
        this.lengthData = packet.getLength();
    }

    public void setLengthData(int lengthData){
        this.lengthData = lengthData;
    }
    public int getLengthData(){
        return lengthData;
    }
}
