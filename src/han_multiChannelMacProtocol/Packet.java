package han_multiChannelMacProtocol;

/**
 * Created by ycqfeng on 2017/1/11.
 */
public class Packet {
    private static int uidBase = 0;
    protected int uid;

    protected PacketType packetType;//类型
    protected int length;//长度
    protected int sourceUid;
    protected int destinationUid;
    protected int generationUid;

    //构造函数
    public Packet(){
        this.uid = uidBase++;
        this.packetType = PacketType.PACKET;
        this.length = 1;
    }
    public Packet(int length){
        this.uid = uidBase++;
        this.packetType = PacketType.PACKET;
        this.length = length;
    }
    public  Packet(int length, PacketType type){
        this.uid = uidBase++;
        this.packetType = type;
        this.length = length;
    }

    //setter and getter
    public void setGenerationUid(int generationUid){
        this.generationUid = generationUid;
    }
    public void setSourceUid(int sourceUid){
        this.sourceUid = sourceUid;
    }
    public void setDestinationUid(int destinationUid){
        this.destinationUid = destinationUid;
    }
    public int getGenerationUid(){
        return this.generationUid;
    }
    public int getSourceUid(){
        return sourceUid;
    }
    public int getDestinationUid(){
        return destinationUid;
    }
    public String getStringDetailUid(){
        String str = "Packet["+packetType+"]";
        str += "("+uid+")";
        str += "{Generation("+generationUid+"),";
        str += "Source("+sourceUid+"),";
        str += "Destination("+destinationUid+")";
        return str;
    }
    public String getStringUid(){
        String str = "";
        str += "Packet["+this.packetType+"](";
        str += this.uid+")";
        return str;
    }
    public void setPacketType(PacketType type){
        this.packetType = type;
    }
    public void setLength(int length){
        this.length = length;
    }
    public PacketType getPacketType(){
        return packetType;
    }
    public int getLength(){
        return length;
    }
    public int getUid(){
        return uid;
    }
}
