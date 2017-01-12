package han_multiChannelMacProtocol;

import han_simulator.*;

/**
 * Created by ycqfeng on 2017/1/11.
 */
public class MacProtocol implements IF_Simulator, IF_HprintNode{
    //静态参数
    private static int uidBase = 0;
    //私有参数
    private int uid;
    private PacketQueue queue;//待发Packet队列
    private MPChannel mpChannel;

    /**
     * 构造函数
     */
    public MacProtocol() {
        this.uid = uidBase++;
        this.queue = new PacketQueue();
        this.mpChannel = new MPChannel(this);
        Simulator.register(this);
        Hprint.register(this);
    }

    /**
     * 加入队列
     * @param packet 需要加入队列的Packet
     * @return 是否加入成功
     */
    public boolean enQueue(Packet packet){
        String str = getStringPacketQueuePosition()+packet.getStringUid();
        if (queue.pushPacket(packet)){
            str += "加入队列成功";
            Hprint.printlntDebugInfo(this, str);
            return true;
        }
        else {
            str += "加入队列失败";
            Hprint.printlntDebugInfo(this, str);
            return false;
        }
    }

    /**
     * 获取队列位置
     * @return String
     */
    public String getStringPacketQueuePosition(){
        return getStringUid()+"/"+queue.getStringPacketQueueState()+"# ";
    }

    /**
     * 获取uid字符串
     * @return String
     */
    public String getStringUid(){
        return "MacProtocol("+uid+")";
    }

    /**
     * 获取本类位置字符串
     * @return String
     */
    @Override
    public String getStringPosition() {
        return "MacProtocol("+uid+")# ";
    }

    /**
     * 设置信道
     * @param channel 信道
     */
    public void setChannel(Channel channel){
        mpChannel.setChannel(channel);
    }
    public static void main(String[] args) {
        Simulator.init();

        Channel channel = new Channel();
        channel.setSubChannelNum(3);

        MacProtocol macProtocol = new MacProtocol();
        macProtocol.setChannel(channel);

        macProtocol.text();

    }
    public void text(){
        mpChannel.text();
    }
}

class MPChannel implements IF_HprintNode{
    private MacProtocol selfMacProtocol;//协议自身
    private Channel channel;//信道
    private int[] subChannelUids;//子信道uid
    private SubChannelState[] subChannelStates;

    public void text(){

    }
    public void turnToNAV(int index, double duration){
        if (this.subChannelStates[index].stateTransmitter == StateSubChannel.IDLE){
            this.transmitterTurnToNAV(index);
            this.endEventTimeTransmitterNAV = Simulator.getCurTime()+duration;
            this.endEventUidTransmitterNAV = Simulator.addEvent(duration, new IF_Event() {
                @Override
                public void run() {
                    endEventTransmitterNAV();
                }
            });
        }
    }

    //统一状态转移接口
    private void transmitterTurnToIDLE(int index){
        String str = getStringSubChannel(subChannelUids[index]);
        str += " Transmitter State: ";
        str += subChannelStates[index].stateTransmitter+"->"+StateSubChannel.IDLE;
        Hprint.printlntDebugInfo(this, str);
        subChannelStates[index].stateTransmitter = StateSubChannel.IDLE;
    }
    private void transmitterTurnToSEND(int index){
        String str = getStringSubChannel(subChannelUids[index]);
        str += " Transmitter State: ";
        str += subChannelStates[index].stateTransmitter+"->"+StateSubChannel.SEND;
        Hprint.printlntDebugInfo(this, str);
        subChannelStates[index].stateTransmitter = StateSubChannel.SEND;
    }
    private void transmitterTurnToNAV(int index){
        String str = getStringSubChannel(subChannelUids[index]);
        str += " Transmitter State: ";
        str += subChannelStates[index].stateTransmitter+"->"+StateSubChannel.NAV;
        Hprint.printlntDebugInfo(this, str);
        subChannelStates[index].stateTransmitter = StateSubChannel.NAV;
    }
    private void receiverTurnToIDLE(int index){
        String str = getStringSubChannel(subChannelUids[index]);
        str += " Transmitter State: ";
        str += subChannelStates[index].stateReceiver+"->"+StateSubChannel.IDLE;
        Hprint.printlntDebugInfo(this, str);
        subChannelStates[index].stateReceiver = StateSubChannel.IDLE;
    }
    private void receiverTurnToRECEIVE(int index){
        String str = getStringSubChannel(subChannelUids[index]);
        str += " Transmitter State: ";
        str += subChannelStates[index].stateReceiver+"->"+StateSubChannel.RECEIVE;
        Hprint.printlntDebugInfo(this, str);
        subChannelStates[index].stateReceiver = StateSubChannel.RECEIVE;
    }

    public void setChannel(Channel channel){
        this.channel = channel;
        this.subChannelUids = new int[channel.getSumSubChannel()];
        channel.writeSubChannelUid(this.subChannelUids);
        this.subChannelStates = new SubChannelState[subChannelUids.length];
        for (int i = 0 ; i < this.subChannelStates.length ; i++){
            this.subChannelStates[i] = new SubChannelState();
        }
    }

    public String getStringSubChannel(int uid){
        return "SubChannel("+uid+")";
    }

    @Override
    public String getStringPosition() {
        return selfMacProtocol.getStringUid()+"/Channel# ";
    }
    public MPChannel(MacProtocol macProtocol){
        Hprint.register(this);
        this.selfMacProtocol = macProtocol;
    }

    class SubChannelState{
        private StateSubChannel stateTransmitter = StateSubChannel.IDLE;//发射机状态
        private StateSubChannel stateReceiver = StateSubChannel.IDLE;//接收机状态
        private int numReceiving;//正在接收的包数目
        private boolean isReceiveCollision;//接收碰撞

        private int endEventUidTransmitterSENDING;//发送状态结束事件uid
        private double endEventTimeTransmitterSENDING;//发送状态结束时间
        private StateSubChannel endEventStateTransmitterSENDING;//发送状态结束时转入的状态
        private int endEventUidReceiverRECEIVING;//接收状态结束事件uid
        private double endEventTimeReceiverRECEIVING;//接收状态结束时间
        private int endEventUidTransmitterNAV;//NAV状态结束事件uid
        private double endEventTimeTransmitterNAV;//NAV状态结束时间
    }
}
