package han_multiChannelMacProtocol;

import han_simulator.*;

/**
 * 目前需要将DATA的收发完善
 * 添加ACK数据确认，基本可以成形发送了，但是还是单信道
 * 信道选择目前一点没有做呢。
 * Created by ycqfeng on 2017/1/11.
 */
public class MacProtocol implements IF_Simulator, IF_HprintNode, IF_Channel{
    //静态参数
    private static int uidBase = 0;
    //私有参数
    private int uid;
    protected PacketQueue queue;//待发Packet队列
    protected MPChannel mpChannel;
    protected MPSendPacket mpSendPacket;
    protected MPReceivePacket mpReceivePacket;

    /**
     * 构造函数
     */
    public MacProtocol() {
        this.uid = uidBase++;
        this.queue = new PacketQueue();
        this.mpChannel = new MPChannel(this);
        this.mpSendPacket = new MPSendPacket(this);
        this.mpReceivePacket = new MPReceivePacket(this);
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
            if (!this.queue.isEmpty()){
                if (this.mpSendPacket.isSendable()) {
                    this.mpSendPacket.addDataPacket(this.queue.popPacket());
                }
            }
            return true;
        }
        else {
            str += "加入队列失败";
            Hprint.printlntDebugInfo(this, str);
            return false;
        }


    }

    @Override
    public void receive(int subChannelUid, Packet packet){
        this.mpReceivePacket.receive(subChannelUid, packet);
    }

    /**
     * 获取队列位置
     * @return String
     */
    public String getStringPacketQueuePosition(){
        return getStringUid()+"/"+queue.getStringPacketQueueState()+"# ";
    }

    /**
     * 获取uid
     * @return uid
     */
    public int getUid(){
        return uid;
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
        channel.attachTo(this);
        mpChannel.setChannel(channel);
        mpSendPacket.setChannel(channel);
        mpReceivePacket.setChannel(channel);
    }

    boolean isSendable(int uidSubChannel){
        return this.mpChannel.isSendable(uidSubChannel);
    }

    boolean isReceiveCollision(int indexSubChannel){
        return this.mpChannel.isReceiveCollision(indexSubChannel);
    }

    boolean isSending(int indexSubChannel){
        return this.mpChannel.isSending(indexSubChannel);
    }
    public StateSubChannel getTransmitterState(int uid){
        return this.mpChannel.getTransmitterState(uid);
    }
    public double getEndTimeNAV(int uid){
        return this.mpChannel.getEndTimeNAV(uid);
    }

    public int getSumSubChannel(){
        return mpChannel.getSumSubChannel();
    }
    public static void main(String[] args) {
        Simulator.init();
        Simulator.setStopTime(10);

        Channel channel = new Channel();
        channel.setSubChannelNum(3);

        MacProtocol macProtocol = new MacProtocol();
        macProtocol.setChannel(channel);

        macProtocol.mpSendPacket.sendDATA(0,1);
//        macProtocol.mpSendPacket.sendRTS(0,1);


        Simulator.start();
    }
    public void text(){
        //mpReceivePacket.test();
        //mpSendPacket.test();
    }
}
class MPReceivePacket implements IF_HprintNode{
    private MacProtocol selfMacProtocol;
    private MPReceivePacket self;

    private int[] subChannelUids;//子信道uid
    private ReceivePacketEvent[] receivePacketEvent;//接收Packet事件
    private int[] waitForCTSFromUid;//等待的CTS
    private int[] waitForDATAFromUid;//等待的DATA
    private int[] waitForACKFromUid;//等待的ACK

    public MPReceivePacket(MacProtocol macProtocol){
        Hprint.register(this);
        this.selfMacProtocol = macProtocol;
        this.self = this;
    }
    public void test(){
        PacketCTS cts = new PacketCTS(200, 200);
        cts.setDestinationUid(0);
        receive(0, cts);
    }
    /**
     * 设置信道
     * @param channel 信道
     */
    public void setChannel(Channel channel){
        this.subChannelUids = new int[channel.getSumSubChannel()];
        this.receivePacketEvent = new ReceivePacketEvent[channel.getSumSubChannel()];
        this.waitForCTSFromUid = new int[channel.getSumSubChannel()];
        this.waitForDATAFromUid = new int[channel.getSumSubChannel()];
        this.waitForACKFromUid = new int [channel.getSumSubChannel()];
        channel.writeSubChannelUid(this.subChannelUids);
        for (int i = 0 ; i < channel.getSumSubChannel() ; i++){
            this.waitForCTSFromUid[i] = -1;
            this.waitForDATAFromUid[i] = -1;
            this.waitForACKFromUid[i] = -1;
        }
    }

    public void setWaitForCTSFromUid(int subChannelUid, int destinationUid){
        String str = "wait for CTS Packet from MacProtocol("+destinationUid+") on ";
        str += "SubChannel("+subChannelUid+")";
        Hprint.printlntDebugInfo(self, str);
        int index = getIndexSubChannel(subChannelUid);
        this.waitForCTSFromUid[index] = destinationUid;
    }
    public void deleteWaitForCTSFromUid(int subChannelUid){
        int index = getIndexSubChannel(subChannelUid);
        String str = "finish waiting for CTS Packet from MacProtocol("+this.waitForCTSFromUid[index]+") on ";
        str += "SubChannel("+subChannelUid+")";
        Hprint.printlntDebugInfo(self, str);
        this.waitForCTSFromUid[index] = -1;
    }
    public void setWaitForDATAFromUid(int subChannelUid, int destinationUid){
        String str = "wait for DATA Packet from MacProtocol("+destinationUid+") on";
        str += "SubChannel("+subChannelUid+")";
        Hprint.printlntDebugInfo(self, str);
        int index = getIndexSubChannel(subChannelUid);
        this.waitForDATAFromUid[index] = destinationUid;
    }
    public void deleteWaitForDATAFromUid(int subChannelUid){
        int index = getIndexSubChannel(subChannelUid);
        String str = "finish waiting for DATA Packet form MacProtocol("+this.waitForDATAFromUid[index]+") on";
        str += "SubChannel("+subChannelUid+")";
        Hprint.printlntDebugInfo(self, str);
        this.waitForDATAFromUid[index] = -1;
    }
    public void setWaitForACKFromUid(int subChannelUid, int destinationUid){
        String str = "wait for ACK Packet from MacProtocol("+destinationUid+") on";
        str += "SubChannel("+subChannelUid+")";
        Hprint.printlntDebugInfo(self, str);
        int index = getIndexSubChannel(subChannelUid);
        this.waitForACKFromUid[index] = destinationUid;
    }
    public void deleteWaitForACKFromUid(int subChannelUid){
        int index = getIndexSubChannel(subChannelUid);
        String str = "finish waiting for ACK Packet form MacProtocol("+this.waitForDATAFromUid[index]+") on";
        str += "SubChannel("+subChannelUid+")";
        Hprint.printlntDebugInfo(self, str);
        this.waitForACKFromUid[index] = -1;
    }

    /**
     * 接收
     * @param subChannelUid 所在信道
     * @param packet 包
     */
    public void receive(int subChannelUid, Packet packet){
        int index = getIndexSubChannel(subChannelUid);
        if (packet.getSourceUid() == selfMacProtocol.getUid()){
            return;
        }
        IF_Event beginEvent = new IF_Event() {
            @Override
            public void run() {

            }
        };
        IF_Event endEvent = new IF_Event() {
            @Override
            public void run() {
                if (receivePacketEvent[index].isCollision()){
                    System.out.println("发生碰撞");
                }
                else {
                    switch (packet.getPacketType()){
                        case RTS:
                            receiveRTS(subChannelUid, packet);
                            break;
                        case CTS:
                            receiveCTS(subChannelUid, packet);
                            break;
                        case DATA:
                            receiveDATA(subChannelUid, packet);
                            break;
                        case ACK:
                            receiveACK(subChannelUid, packet);
                            break;
                    }
                }
                receivePacketEvent[index] = null;
            }
        };
        this.receivePacketEvent[index] = new ReceivePacketEvent(subChannelUid, packet, beginEvent, endEvent);
    }

    private void receiveACK(int subChannelUid, Packet packet){
        final PacketACK ack;
        int index = getIndexSubChannel(subChannelUid);
        if (packet.getPacketType() == PacketType.ACK){
            ack = (PacketACK) packet;
        }
        else {
            String error = "传入的Packet不是ACK包";
            Hprint.printlntErrorInfo(self, error);
            return;
        }
        if (ack.getDestinationUid() != selfMacProtocol.getUid()){
            //目标不是自己
            return;
        }
        else if (this.waitForACKFromUid[index] == -1){
            String str = "未等待ACK,忽略"+ack.getStringDetailUid();
            Hprint.printlntDebugInfo(self, str);
            //没有等待DATA
            return;
        }
        //执行相应动作
        selfMacProtocol.mpSendPacket.deleteDATAReTransEvent(index);
        selfMacProtocol.mpChannel.deleteEndEventTransmitterNAV(subChannelUid);
        //System.out.println("成功收到ACK，并执行相应动作");
        Simulator.addEvent(TimeUnitValue.ps, new IF_Event() {
            @Override
            public void run() {
                selfMacProtocol.mpSendPacket.finishSendDataPacket();
            }
        });
    }

    /**
     * 收到DATA
     * @param subChannelUid 信道
     * @param packet 数据包
     */
    private void receiveDATA(int subChannelUid, Packet packet){
        final PacketDATA data;
        int index = getIndexSubChannel(subChannelUid);
        if (packet.getPacketType() == PacketType.DATA){
            data = (PacketDATA) packet;
        }
        else {
            String error = "传入的Packet不是DATA包";
            Hprint.printlntErrorInfo(self, error);
            return;
        }
        if (data.getDestinationUid() != selfMacProtocol.getUid()){
            //目标不是自己
            return;
        }
        else if (this.waitForDATAFromUid[index] == -1){
            String str = "未等待DATA,忽略"+data.getStringDetailUid();
            Hprint.printlntDebugInfo(self, str);
            //没有等待DATA
            return;
        }
        //执行相应动作
        deleteWaitForDATAFromUid(index);
        selfMacProtocol.mpChannel.deleteEndEventTransmitterNAV(subChannelUid);
        System.out.println("成功收到DATA，并执行相应动作");
        //
        Simulator.addEvent(TimeUnitValue.ps, new IF_Event() {
            @Override
            public void run() {
                selfMacProtocol.mpSendPacket.sendACK(subChannelUid, data.getSourceUid());
            }
        });
    }
    /**
     * 收到CTS
     * @param subChannelUid 信道
     * @param packet CTS包
     */
    private void receiveCTS(int subChannelUid, Packet packet){
        final PacketCTS cts;
        int index = getIndexSubChannel(subChannelUid);
        if (packet.getPacketType() == PacketType.CTS){
            cts = (PacketCTS)packet;
        }
        else {
            String error = "传入的Packet不是CTS包";
            Hprint.printlntErrorInfo(self, error);
            return;
        }
        if (cts.getDestinationUid() != selfMacProtocol.getUid()){
            //目标不是自己
            return;
        }
        else if (this.waitForCTSFromUid[index] == -1){
            String str = "未等待CTS,忽略"+cts.getStringDetailUid();
            Hprint.printlntDebugInfo(self, str);
            //没有等待CTS
            return;
        }
        //执行相应动作
        selfMacProtocol.mpSendPacket.deleteRTSReTransEvent(index);
        selfMacProtocol.mpReceivePacket.deleteWaitForCTSFromUid(subChannelUid);
        selfMacProtocol.mpChannel.deleteEndEventTransmitterNAV(subChannelUid);
        System.out.println("成功收到CTS，并执行相应动作");
        Simulator.addEvent(TimeUnitValue.ps, new IF_Event() {
            @Override
            public void run() {
                selfMacProtocol.mpSendPacket.sendDATA(subChannelUid, cts.getSourceUid());
            }
        });
    }

    /**
     * 接收到RTS
     * @param subChannelUid 信道
     * @param packet RTS包
     */
    private void receiveRTS(int subChannelUid, Packet packet){
        final PacketRTS rts;
        if (packet.getPacketType() == PacketType.RTS){
            rts = (PacketRTS)packet;
        }
        else {
            String error = "传入的Packet不是RTS包";
            Hprint.printlntErrorInfo(self, error);
            return;
        }
        if (rts.getDestinationUid() != selfMacProtocol.getUid()){
            //目标不是自己
            return;
        }
        //执行相应动作
        int index = getIndexSubChannel(subChannelUid);
        //setWaitForDATAFromUid(subChannelUid, rts.getSourceUid());
        Simulator.addEvent(TimeUnitValue.ps, new IF_Event() {
            @Override
            public void run() {
                selfMacProtocol.mpSendPacket.sendCTS(subChannelUid, rts.getSourceUid(), rts.getLengthData());
            }
        });
    }

    /**
     * 获取对应子信道的uid
     * @param index 序号
     * @return 子信道的uid
     */
    public int getSubChannelUid(int index){
        return this.subChannelUids[index];
    }

    /**
     * 获取对应uid子信道的index
     * @param uid 子信道的uid
     * @return index
     */
    public int getIndexSubChannel(int uid){
        for (int i = 0 ; i < subChannelUids.length ; i++){
            if (subChannelUids[i] == uid){
                return i;
            }
        }
        return -1;
    }
    /**
     * 获取位置字符串
     * @return 字符串
     */
    @Override
    public String getStringPosition() {
        return selfMacProtocol.getStringUid()+"/MPSendPacket# ";
    }
    class ReceivePacketEvent{
        IF_Event beginEvent;
        IF_Event endEvent;
        int subChannelUid;
        Packet packet;
        boolean isCollision;
        public ReceivePacketEvent(int subChannelUid, Packet packet, IF_Event beginEvent, IF_Event endEvent){
            this.subChannelUid = subChannelUid;
            this.packet = packet;
            this.beginEvent = beginEvent;
            this.endEvent = endEvent;
            this.isCollision = false;
            Simulator.addEvent(0, new ReceivePacketBegin());
            double t = SubChannel.getSubChannel(subChannelUid).getTransTime(packet);
            selfMacProtocol.mpChannel.turnToREVEIVE(subChannelUid, t);
        }
        public boolean isCollision(){
            return isCollision;
        }
        class ReceivePacketBegin implements IF_Event{
            @Override
            public void run() {
                double t = SubChannel.getSubChannel(subChannelUid).getTransTime(packet);
                String str = "begin receive "+packet.getStringDetailUid()+" on ";
                str += SubChannel.getSubChannel(subChannelUid).getStringUid();
                Hprint.printlntDebugInfo(self, str);
                Simulator.addEvent(t, new ReceivePacketEnd());
                if (beginEvent != null){
                    beginEvent.run();
                }

            }
        }
        class ReceivePacketEnd implements IF_Event{
            @Override
            public void run() {
                String str = "finish receive "+packet.getStringDetailUid()+" on ";
                str += SubChannel.getSubChannel(subChannelUid).getStringUid();
                Hprint.printlntDebugInfo(self, str);
                isCollision = selfMacProtocol.mpChannel.isReceiveCollision(subChannelUid);
                if (isCollision){
                    str = packet.getStringUid()+" come into collision on ";
                    str += SubChannel.getSubChannel(subChannelUid).getStringUid();
                    Hprint.printlntDebugInfo(self, str);
                }
                else {
                    str = packet.getStringUid()+" is received successfully on ";
                    str += SubChannel.getSubChannel(subChannelUid).getStringUid();
                    Hprint.printlntDebugInfo(self, str);
                }
                if (endEvent != null){
                    endEvent.run();
                }
            }
        }
    }

}
/**
 * 信道状态记录
 */
class MPChannel implements IF_HprintNode{
    private MacProtocol selfMacProtocol;//协议自身
    private Channel channel;//信道
    private int[] subChannelUids;//子信道uid
    private SubChannelState[] subChannelStates;

    public void turnToNAV(int subChannelUid, double duration){
        int index = getIndexSubChannel(subChannelUid);
        //处于IDLE
        if (this.subChannelStates[index].stateTransmitter == StateSubChannel.IDLE){
            this.transmitterTurnToNAV(index);
            this.subChannelStates[index].endEventTimeTransmitterNAV = Simulator.getCurTime()+duration;
            this.subChannelStates[index].endEventUidTransmitterNAV = Simulator.addEvent(duration, new IF_Event() {
                @Override
                public void run() {
                    endEventTransmitterNAV(index);
                }
            });
            return;
        }
        //处于NAV
        else if (this.subChannelStates[index].stateTransmitter == StateSubChannel.NAV){
            if (this.subChannelStates[index].endEventTimeTransmitterNAV >= Simulator.getCurTime()+duration){
                return;
            }
            else {
                this.deleteEndEventTransmitterNAV(subChannelUid);
                this.subChannelStates[index].endEventTimeTransmitterNAV = Simulator.getCurTime()+duration;
                this.subChannelStates[index].endEventUidTransmitterNAV = Simulator.addEvent(duration, new IF_Event() {
                    @Override
                    public void run() {
                        endEventTransmitterNAV(index);
                    }
                });
                return;
            }
        }
        //处于SEND
        else if (this.subChannelStates[index].stateTransmitter == StateSubChannel.SEND){
            //NAV结束先于SEND，忽略
            if (this.subChannelStates[index].endEventTimeTransmitterSEND >= Simulator.getCurTime()+duration){
                return;
            }
            //NAV结束后于SEND，判断
            else {
                //已经有了NAV
                if (this.subChannelStates[index].endEventUidTransmitterNAV != -1){
                    //比已经存在的NAV更靠后
                    if (this.subChannelStates[index].endEventTimeTransmitterNAV < Simulator.getCurTime()+duration){
                        this.deleteEndEventTransmitterNAV(subChannelUid);
                        this.subChannelStates[index].endEventTimeTransmitterNAV = Simulator.getCurTime()+duration;
                        this.subChannelStates[index].endEventUidTransmitterNAV = Simulator.addEvent(duration, new IF_Event() {
                            @Override
                            public void run() {
                                endEventTransmitterNAV(index);
                            }
                        });
                    }
                    return;
                }
                //没有NAV
                this.subChannelStates[index].endEventTimeTransmitterNAV = Simulator.getCurTime()+duration;
                this.subChannelStates[index].endEventUidTransmitterNAV = Simulator.addEvent(duration, new IF_Event() {
                    @Override
                    public void run() {
                        endEventTransmitterNAV(index);
                    }
                });
                return;
            }

        }
        //处于其他状态则出错
        else {
            String str = getStringSubChannel(subChannelUids[index]);
            str += "接收机进入NAV状态出错";
            Hprint.printlntDebugInfo(this ,str);
        }
    }
    public void turnToSEND(int subChannelUid, double duration){
        int index = getIndexSubChannel(subChannelUid);
        //处于IDLE
        if (this.subChannelStates[index].stateTransmitter == StateSubChannel.IDLE){
            this.transmitterTurnToSEND(index);
            this.subChannelStates[index].endEventTimeTransmitterSEND = Simulator.getCurTime()+duration;
            this.subChannelStates[index].endEventUidTransmitterSEND = Simulator.addEvent(duration, new IF_Event() {
                @Override
                public void run() {
                    endEventTransmitterSEND(index);
                }
            });
            return;
        }
        //处于SEND
        else if (this.subChannelStates[index].stateTransmitter == StateSubChannel.SEND){
            String str = getStringSubChannel(subChannelUids[index]);
            str += "接收机SEND状态不允许再次发送状态";
            Hprint.printlntDebugInfo(this ,str);
            return;
        }
        //处于NAV
        else if (this.subChannelStates[index].stateTransmitter == StateSubChannel.NAV){
            String str = getStringSubChannel(subChannelUids[index]);
            str += "接收机NAV状态不允许进入发送状态";
            Hprint.printlntDebugInfo(this ,str);
            return;
        }
        //处于其他状态则出错
        else {
            String str = getStringSubChannel(subChannelUids[index]);
            str += "接收机进入SEND状态出错";
            Hprint.printlntDebugInfo(this ,str);
        }
    }
    public void turnToREVEIVE(int subChannelUid, double duration){
        int index = getIndexSubChannel(subChannelUid);
        //处于IDLE
        if (this.subChannelStates[index].stateReceiver == StateSubChannel.IDLE){
            this.receiverTurnToRECEIVE(index);
            this.subChannelStates[index].numReceive = 1;
            this.subChannelStates[index].isReceiveCollision = false;
            this.subChannelStates[index].endEventTimeReceiverRECEIVE = Simulator.getCurTime()+duration;
            this.subChannelStates[index].endEventUidReceiverRECEIVE = Simulator.addEvent(duration, new IF_Event() {
                @Override
                public void run() {
                    endEventReceiverRECEIVE(index);
                }
            });
            return;
        }
        //处于RECEIVE
        else if (this.subChannelStates[index].stateReceiver == StateSubChannel.RECEIVE){
            this.subChannelStates[index].isReceiveCollision = true;
            if (this.subChannelStates[index].endEventTimeReceiverRECEIVE >= Simulator.getCurTime()+duration){
                return;
            }
            else {
                this.subChannelStates[index].numReceive++;
                this.subChannelStates[index].endEventTimeReceiverRECEIVE = Simulator.getCurTime()+duration;
                this.subChannelStates[index].endEventUidReceiverRECEIVE = Simulator.addEvent(duration, new IF_Event() {
                    @Override
                    public void run() {
                        endEventReceiverRECEIVE(index);
                    }
                });
                return;
            }
        }
        //处于其他状态则出错
        else {
            String str = getStringSubChannel(subChannelUids[index]);
            str += "接收机状态出错";
            Hprint.printlntDebugInfo(this ,str);
        }
    }

    //统一结束状态
    private void endEventTransmitterNAV(int index){
        String str = getStringSubChannel(subChannelUids[index]);
        str += " Transmitter State: ";
        str += "NAV is ending";
        Hprint.printlntDebugInfo(this ,str);
        transmitterTurnToIDLE(index);
        clearEndEventTransmitterNAV(index);
    }
    private void endEventTransmitterSEND(int index){
        String str = getStringSubChannel(subChannelUids[index]);
        str += " Transmitter State: ";
        str += "SEND is ending";
        Hprint.printlntDebugInfo(this ,str);
        if (this.subChannelStates[index].endEventUidTransmitterNAV != -1){
            transmitterTurnToNAV(index);
        }
        else {
            transmitterTurnToIDLE(index);
        }
        clearEndEventTransmitterSEND(index);
    }
    private void endEventReceiverRECEIVE(int index){
        if (this.subChannelStates[index].numReceive == 1){
            String str = getStringSubChannel(subChannelUids[index]);
            str += " Receiver State: ";
            str += "RECEIVE is ending";
            Hprint.printlntDebugInfo(this ,str);
            receiverTurnToIDLE(index);
        }
        this.subChannelStates[index].numReceive--;
    }
    //删除结束事件
    protected void deleteEndEventTransmitterNAV(int subChannelUid){
        int index = getIndexSubChannel(subChannelUid);
        if (Simulator.deleteEvent(this.subChannelStates[index].endEventUidTransmitterNAV)){
            String str = "结束NAV成功";
            Hprint.printlntDebugInfo(this, str);
            endEventTransmitterNAV(index);
        }
        else {
            String str = "结束NAV失败";
            Hprint.printlntDebugInfo(this, str);
        }
        clearEndEventTransmitterNAV(index);
    }
    //清除结束状态
    private void clearEndEventTransmitterNAV(int index){
        this.subChannelStates[index].endEventTimeTransmitterNAV = -1;
        this.subChannelStates[index].endEventUidTransmitterNAV = -1;
    }
    private void clearEndEventTransmitterSEND(int index){
        this.subChannelStates[index].endEventTimeTransmitterSEND = -1;
        this.subChannelStates[index].endEventUidTransmitterSEND = -1;
    }
    private void clearEndEventReceiverRECEIVE(int index){
        this.subChannelStates[index].endEventTimeReceiverRECEIVE = -1;
        this.subChannelStates[index].endEventUidReceiverRECEIVE = -1;
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
        str += " Receiver State: ";
        str += subChannelStates[index].stateReceiver+"->"+StateSubChannel.IDLE;
        Hprint.printlntDebugInfo(this, str);
        subChannelStates[index].stateReceiver = StateSubChannel.IDLE;
    }
    private void receiverTurnToRECEIVE(int index){
        String str = getStringSubChannel(subChannelUids[index]);
        str += " Receiver State: ";
        str += subChannelStates[index].stateReceiver+"->"+StateSubChannel.RECEIVE;
        Hprint.printlntDebugInfo(this, str);
        subChannelStates[index].stateReceiver = StateSubChannel.RECEIVE;
    }
    //检测是否可发送
    public boolean isSending(int uid){
        int index = getIndexSubChannel(uid);
        if (this.subChannelStates[index].stateTransmitter == StateSubChannel.SEND){
            return true;
        }
        return false;
    }
    public boolean isSendable(int uid){
        int index = getIndexSubChannel(uid);
        //非空闲则不可发送
        if (this.subChannelStates[index].stateTransmitter != StateSubChannel.IDLE){
            return false;
        }
        //接收状态则不可发
        else if (this.subChannelStates[index].stateReceiver == StateSubChannel.RECEIVE){
            return false;
        }
        else {
            return true;
        }
    }
    public boolean isReceiveCollision(int uid){
        int index = getIndexSubChannel(uid);
        return this.subChannelStates[index].isReceiveCollision;
    }
    //获取收发机状态
    public StateSubChannel getTransmitterState(int uid){
        int index = getIndexSubChannel(uid);
        return this.subChannelStates[index].stateTransmitter;
    }
    public StateSubChannel getReceiverState(int uid){
        int index = getIndexSubChannel(uid);
        return this.subChannelStates[index].stateReceiver;
    }
    public double getEndTimeNAV(int uid){
        int index = getIndexSubChannel(uid);
        return this.subChannelStates[index].endEventTimeTransmitterNAV;
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

    public int getSumSubChannel(){
        return this.subChannelUids.length;
    }
    /**
     * 获取对应子信道的uid
     * @param index 序号
     * @return 子信道的uid
     */
    public int getSubChannelUid(int index){
        return this.subChannelUids[index];
    }

    /**
     * 获取对应uid子信道的index
     * @param uid 子信道的uid
     * @return index
     */
    public int getIndexSubChannel(int uid){
        for (int i = 0 ; i < subChannelUids.length ; i++){
            if (subChannelUids[i] == uid){
                return i;
            }
        }
        return -1;
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
        private int numReceive = 0;//正在接收的包数目
        private boolean isReceiveCollision = false;//接收碰撞

        private int endEventUidTransmitterSEND = -1;//发送状态结束事件uid
        private double endEventTimeTransmitterSEND = -1;//发送状态结束时间
        //private StateSubChannel endEventStateTransmitterSEND;//发送状态结束时转入的状态
        private int endEventUidReceiverRECEIVE = -1;//接收状态结束事件uid
        private double endEventTimeReceiverRECEIVE = -1;//接收状态结束时间
        private int endEventUidTransmitterNAV = -1;//NAV状态结束事件uid
        private double endEventTimeTransmitterNAV = -1;//NAV状态结束时间
    }
}

/**
 * 发送数据包
 */
class MPSendPacket implements IF_HprintNode{
    private MacProtocol selfMacProtocol;
    private MPSendPacket self;
    private DIFS[] difss;
    private SIFS[] sifss;
    private int[] subChannelUids;//子信道uid

    private BackOff[] backOffs;//退避
    private double backoffBaseTime = 0.1;//退避基准时间
    private double timeDIFS = 0.1;
    private double timeSIFS = 0.1;
    //ACK参数
    private int lengthACK = 20*8;//CTS bit数
    private int[] ACKBackoffTime;//RTS退避次数
    private int ACKBackoffTimeLimit = 16;//RTS退避次数限制
    //DATA参数
    private PacketDATA dataPacket;
    private int[] DATABackoffTime;//DATA退避次数
    private int DATABackoffTimeLimit = 16;//DATA退避次数限制
    private int[] difsReTryDATA;//DIFS重试次数
    private int difsReTryDATALimit = 10;//DIFS重试次数限制
    private double dataReTransTimeLimit = 2;//DATA重传时间限制
    private int[] dataReTrans;//DATA重传次数
    private int dataReTransLimit = 0;//DATA重传次数限制
    private int[] dataReTransEventUid;//DATA重传事件UID
    private double waitForDataTimeLimit = 1;//等待数据包持续时间
    //CTS参数
    private int lengthCTS = 20*8;//CTS bit数
    //RTS参数
    private int[] RTSBackoffTime;//RTS退避次数
    private int RTSBackoffTimeLimit = 16;//RTS退避次数限制
    private int[] difsReTryRTS;//DIFS重试次数
    private int difsReTryRTSLimit = 10;//DIFS重试次数限制
    private double rtsReTransTimeLimit = 2;//RTS重传时间限制
    private int[] rtsReTrans;//RTS重传次数
    private int rtsReTransLimit = 3;//RTS重传次数限制
    private int[] rtsReTransEventUid;//RTS重传事件UID
    private int lengthRTS = 20*8;//RTS bit数

    public MPSendPacket(MacProtocol macProtocol){
        Hprint.register(this);
        this.self = this;
        this.selfMacProtocol = macProtocol;
        this.difss = null;
        this.sifss = null;
    }

    public boolean isSendable(){
        if (dataPacket == null){
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * 设置数据包
     * @param packet 数据包
     */
    public void addDataPacket(Packet packet){
        String str = "加入一个新数据包将被发送";
        Hprint.printlntDebugInfo(this,str);
        this.dataPacket = new PacketDATA(packet);
        this.dataPacket.setSourceUid(this.selfMacProtocol.getUid());
        this.dataPacket.setDestinationUid(packet.destinationUid);
        this.sendRTS(0, dataPacket.getDestinationUid());
    }

    public void finishSendDataPacket(){
        String str = "完成发送一个数据包";
        Hprint.printlntDebugInfo(this, str);
        this.dataPacket = null;
        if (!selfMacProtocol.queue.isEmpty()){
            addDataPacket(selfMacProtocol.queue.popPacket());
        }
    }

    /**
     * 发送RTS
     * @param subChannelUid 使用的子信道
     * @param destinationUid 目标
     */
    public void sendRTS(int subChannelUid, int destinationUid){
        //若不可发送
        int index = getIndexSubChannel(subChannelUid);
        if (!selfMacProtocol.isSendable(subChannelUid)){
            //若处于NAV状态，等待NAV结束
            if (selfMacProtocol.getTransmitterState(subChannelUid) == StateSubChannel.NAV){
                String str = getStringPosition();
                str += "处于NAV状态，RTS需要退避";
                Hprint.printlntDebugInfo(this, str);
                Simulator.addEvent(selfMacProtocol.getEndTimeNAV(subChannelUid) - Simulator.getCurTime()+TimeUnitValue.ps,
                        new IF_Event() {
                            @Override
                            public void run() {
                                sendRTS(subChannelUid, destinationUid);
                            }
                        });
                return;
            }
            //若不处于NAV状态，则退避
            else {
                if (this.RTSBackoffTime[index]++ < this.RTSBackoffTimeLimit){
                    String str = getStringPosition();
                    str += "发送RTS时，"+SubChannel.getSubChannel(subChannelUid).getStringUid();
                    str += "被占用，执行第"+this.RTSBackoffTime[index]+"次退避";
                    Hprint.printlntDebugInfo(this, str);
                    Simulator.addEvent(this.backOffs[index].getBackOffTime(),
                            new IF_Event() {
                                @Override
                                public void run() {
                                    sendRTS(subChannelUid, destinationUid);
                                }
                            });
                    return;
                }
                else {
                    String str = getStringPosition();
                    str += "在"+SubChannel.getSubChannel(subChannelUid).getStringUid();
                    str += "发送RTS到达退避上限，丢弃数据包";
                    Hprint.printlntDebugInfo(this, str);
                    this.RTSBackoffTime[index] = 0;
                    return;
                }
            }
        }
        //若可发送
        else {
            this.RTSBackoffTime[index] = 0;
            PacketRTS rts = new PacketRTS(lengthRTS, dataPacket);
            rts.setSourceUid(selfMacProtocol.getUid());
            rts.setDestinationUid(destinationUid);
            IF_Event DIFSbeginEvent = new IF_Event() {
                @Override
                public void run() {
                    Hprint.printlntDebugInfo(self, "DIFS开始");
                }
            };
            IF_Event DIFSendEvent = new IF_Event() {
                @Override
                public void run() {
                    Hprint.printlntDebugInfo(self, "DIFS结束");
                    //被中断
                    if (difss[index].isDisturb){
                        deleteDIFS(index);
                        if (difsReTryRTS[index]++ < difsReTryRTSLimit){
                            String str = getStringPosition();
                            str += "DIFS未成功，第"+difsReTryRTS+"次重试";
                            Hprint.printlntDebugInfo(self, str);
                            deleteDIFS(index);//被中断，删除DIFS重来
                            sendRTS(subChannelUid, destinationUid);
                            return;
                        }
                        else {
                            difsReTryRTS[index] = 0;
                            String str = getStringPosition();
                            str += "DIFS连续"+difsReTryRTSLimit+"次失败，发送RTS失败，丢弃正在发送的数据包";
                            Hprint.printlntDebugInfo(self, str);
                            deleteDIFS(index);//重试失败，删除
                            //丢弃数据包
                        }

                    }
                    //未被中断,开始发送RTS
                    else {
                        deleteDIFS(index);
                        double timeRetransWatch = rtsReTransTimeLimit+SubChannel.getSubChannel(subChannelUid).getTransTime(rts);
                        //设置重传监控
                        if (rtsReTransLimit > 0){//rtsReTransLimit>0启用重传机制
                            if (rtsReTrans[index]++ < rtsReTransLimit){
                                //到时间，需要重传
                                rtsReTransEventUid[index] = Simulator.addEvent(timeRetransWatch,
                                        new IF_Event() {
                                            @Override
                                            public void run() {
                                                String str = getStringPosition();
                                                str += "启动第"+rtsReTrans[index]+"次RTS重传";
                                                Hprint.printlntDebugInfo(self, str);
                                                deleteDIFS(index);
                                                selfMacProtocol.mpReceivePacket.deleteWaitForCTSFromUid(subChannelUid);
                                                sendRTS(subChannelUid, destinationUid);
                                            }
                                        });

                            }
                            else {
                                //到时间，不需要重传，需要善后
                                rtsReTransEventUid[index] = Simulator.addEvent(timeRetransWatch,
                                        new IF_Event() {
                                            @Override
                                            public void run() {
                                                String str = getStringPosition();
                                                str += "重传失败，丢弃"+dataPacket.getStringUid();
                                                Hprint.printlntDebugInfo(self,str);
                                                rtsReTrans[index] = 0;
                                                rtsReTransEventUid[index] = -1;
                                                selfMacProtocol.mpReceivePacket.deleteWaitForCTSFromUid(subChannelUid);
                                                deleteDIFS(index);
                                                //此处不完善，需要处理被丢弃的数据包dataPacket
                                            }
                                        });
                            }
                        }
                        else {//rtsReTransLimit=0 不启用重传机制
                            rtsReTransEventUid[index] = Simulator.addEvent(timeRetransWatch,
                                    new IF_Event() {
                                        @Override
                                        public void run() {
                                            String str = getStringPosition();
                                            str += "未收到CTS，丢弃数据包"+dataPacket.getStringUid();
                                            Hprint.printlntDebugInfo(self,str);
                                            selfMacProtocol.mpReceivePacket.deleteWaitForCTSFromUid(subChannelUid);
                                            deleteDIFS(index);
                                            //此处不完善，需要处理被丢弃的数据包dataPacket
                                        }
                                    });
                        }

                        //开始RTS包
                        IF_Event endSendPacketEvent = new IF_Event() {
                            @Override
                            public void run() {
                                selfMacProtocol.mpChannel.turnToNAV(subChannelUid, rtsReTransTimeLimit);
                                selfMacProtocol.mpReceivePacket.setWaitForCTSFromUid(subChannelUid, destinationUid);
                            }
                        };
                        SendPacket sendPacket = new SendPacket(getSubChannelUid(index), rts, null, endSendPacketEvent);
                    }
                }
            };
            addDIFS(index, timeDIFS, DIFSbeginEvent, DIFSendEvent);
            return;
        }
    }

    /**
     * 发送CTS
     * @param subChannelUid 信道
     * @param destinationUid 目标
     * @param dataPacketLength 数据包长度确认
     */
    public void sendCTS(int subChannelUid, int destinationUid, int dataPacketLength){
        int index = getIndexSubChannel(subChannelUid);
        if (!selfMacProtocol.isSendable(subChannelUid)){
            //若处于NAV状态，等待NAV结束
            if (selfMacProtocol.getTransmitterState(subChannelUid) == StateSubChannel.NAV){
                String str = getStringPosition();
                str += "处于NAV状态，CTS需要退避";
                Hprint.printlntDebugInfo(this, str);
                Simulator.addEvent(selfMacProtocol.getEndTimeNAV(subChannelUid) - Simulator.getCurTime()+TimeUnitValue.ps,
                        new IF_Event() {
                            @Override
                            public void run() {
                                sendCTS(subChannelUid, destinationUid, dataPacketLength);
                            }
                        });
                return;
            }
            //若不处于NAV状态，则退避
            else {
                if (this.RTSBackoffTime[index]++ < this.RTSBackoffTimeLimit){
                    String str = "发送CTS时，"+SubChannel.getSubChannel(subChannelUid).getStringUid();
                    str += "被占用，执行第"+this.RTSBackoffTime[index]+"次退避";
                    Hprint.printlntDebugInfo(this, str);
                    Simulator.addEvent(this.backOffs[index].getBackOffTime(),
                            new IF_Event() {
                                @Override
                                public void run() {
                                    sendCTS(subChannelUid, destinationUid, dataPacketLength);
                                }
                            });
                    return;
                }
                else {
                    String str = "在"+SubChannel.getSubChannel(subChannelUid).getStringUid();
                    str += "发送CTS到达退避上限，发送失败";
                    Hprint.printlntDebugInfo(this, str);
                    this.RTSBackoffTime[index] = 0;
                    return;
                }
            }
        }
        //开始发送CTS
        PacketCTS cts = new PacketCTS(lengthCTS, dataPacketLength);
        cts.setSourceUid(selfMacProtocol.getUid());
        cts.setDestinationUid(destinationUid);
        IF_Event SIFSBeginEvent = new IF_Event() {
            @Override
            public void run() {
                Hprint.printlntDebugInfo(self, "SIFS开始");
            }
        };
        IF_Event SIFSEndEvent = new IF_Event() {
            @Override
            public void run() {
                Hprint.printlntDebugInfo(self, "SIFS结束");
                deleteSIFS(getIndexSubChannel(subChannelUid));
                //添加等待DATA
                /*IF_Event waitForDataEvent = new IF_Event() {
                    @Override
                    public void run() {
                        selfMacProtocol.mpReceivePacket.setWaitForDATAFromUid(subChannelUid, destinationUid);
                    }
                };*/
                IF_Event waitForDataEvent = new IF_Event() {
                    @Override
                    public void run() {
                        selfMacProtocol.mpChannel.turnToNAV(subChannelUid, waitForDataTimeLimit);
                        selfMacProtocol.mpReceivePacket.setWaitForDATAFromUid(subChannelUid, destinationUid);
                    }
                };
                SendPacket sendPacket = new SendPacket(subChannelUid, cts, null, waitForDataEvent);
            }
        };
        addSIFS(subChannelUid, timeSIFS, SIFSBeginEvent, SIFSEndEvent);
    }

    /**
     * 发送数据
     * @param subChannelUid 子信道
     * @param destinationUid 目标
     */
    public void sendDATA(int subChannelUid, int destinationUid){
        //若不可发送
        int index = getIndexSubChannel(subChannelUid);
        if (!selfMacProtocol.isSendable(subChannelUid)){
            //若处于NAV状态，等待NAV结束
            if (selfMacProtocol.getTransmitterState(subChannelUid) == StateSubChannel.NAV){
                String str = getStringPosition();
                str += "处于NAV状态，DATA需要退避";
                Hprint.printlntDebugInfo(this, str);
                Simulator.addEvent(selfMacProtocol.getEndTimeNAV(subChannelUid) - Simulator.getCurTime()+TimeUnitValue.ps,
                        new IF_Event() {
                            @Override
                            public void run() {
                                sendDATA(subChannelUid, destinationUid);
                            }
                        });
                return;
            }
            //若不处于NAV状态，则退避
            else {
                if (this.DATABackoffTime[index]++ < this.DATABackoffTimeLimit){
                    String str = "发送DATA时，"+SubChannel.getSubChannel(subChannelUid).getStringUid();
                    str += "被占用，执行第"+this.DATABackoffTime[index]+"次退避";
                    Hprint.printlntDebugInfo(this, str);
                    Simulator.addEvent(this.backOffs[index].getBackOffTime(),
                            new IF_Event() {
                                @Override
                                public void run() {
                                    sendDATA(subChannelUid, destinationUid);
                                }
                            });
                    return;
                }
                else {
                    String str = "在"+SubChannel.getSubChannel(subChannelUid).getStringUid();
                    str += "发送DATA到达退避上限，丢弃数据包";
                    Hprint.printlntDebugInfo(this, str);
                    this.DATABackoffTime[index] = 0;
                    return;
                }
            }
        }
        //若可发送
        else {
            this.DATABackoffTime[index] = 0;
            IF_Event SIFSbeginEvent = new IF_Event() {
                @Override
                public void run() {
                    Hprint.printlntDebugInfo(self, "SIFS开始");
                }
            };
            IF_Event SIFSendEvent = new IF_Event() {
                @Override
                public void run() {
                    Hprint.printlntDebugInfo(self, "SIFS结束");
                    deleteSIFS(index);
                    double timeRetransWatch = dataReTransTimeLimit+SubChannel.getSubChannel(subChannelUid).getTransTime(dataPacket);
                    //设置重传监控
                    if (dataReTransLimit > 0){//dataReTransLimit>0启用重传机制
                        if (dataReTrans[index]++ < dataReTransLimit){
                            //到时间，需要重传
                            dataReTransEventUid[index] = Simulator.addEvent(timeRetransWatch,
                                    new IF_Event() {
                                        @Override
                                        public void run() {
                                            String str = "启动第"+dataReTrans[index]+"次DATA重传";
                                            Hprint.printlntDebugInfo(self, str);
                                            deleteDIFS(index);
                                            selfMacProtocol.mpReceivePacket.deleteWaitForACKFromUid(subChannelUid);
                                            sendDATA(subChannelUid, destinationUid);
                                        }
                                    });

                        }
                        else {
                            //到时间，不需要重传，需要善后
                            dataReTransEventUid[index] = Simulator.addEvent(timeRetransWatch,
                                    new IF_Event() {
                                        @Override
                                        public void run() {
                                            String str = "重传失败，丢弃"+dataPacket.getStringUid();
                                            Hprint.printlntDebugInfo(self,str);
                                            dataReTrans[index] = 0;
                                            dataReTransEventUid[index] = -1;
                                            selfMacProtocol.mpReceivePacket.deleteWaitForACKFromUid(subChannelUid);
                                            deleteDIFS(index);
                                            //此处不完善，需要处理被丢弃的数据包dataPacket
                                        }
                                    });
                        }
                    }
                    else {//dataReTransLimit=0 不启用重传机制
                        dataReTransEventUid[index] = Simulator.addEvent(timeRetransWatch,
                                new IF_Event() {
                                    @Override
                                    public void run() {
                                        String str = "未收到ACK，丢弃数据包"+dataPacket.getStringUid();
                                        Hprint.printlntDebugInfo(self,str);
                                        selfMacProtocol.mpReceivePacket.deleteWaitForACKFromUid(subChannelUid);
                                        deleteDIFS(index);
                                        //此处不完善，需要处理被丢弃的数据包dataPacket
                                    }
                                });
                    }

                    //开始DATA包
                    IF_Event endSendPacketEvent = new IF_Event() {
                        @Override
                        public void run() {
                            selfMacProtocol.mpChannel.turnToNAV(subChannelUid, dataReTransTimeLimit);
                            selfMacProtocol.mpReceivePacket.setWaitForACKFromUid(subChannelUid, destinationUid);
                            //selfMacProtocol.mpReceivePacket.setWaitForCTSFromUid(subChannelUid, destinationUid);
                        }
                    };
                    SendPacket sendPacket = new SendPacket(getSubChannelUid(index), dataPacket, null, endSendPacketEvent);
                }
            };
            addSIFS(index, timeDIFS, SIFSbeginEvent, SIFSendEvent);
            return;
        }
    }

    public void sendACK(int subChannelUid, int destinationUid){
        int index = getIndexSubChannel(subChannelUid);
        if (!selfMacProtocol.isSendable(subChannelUid)){
            //若处于NAV状态，等待NAV结束
            if (selfMacProtocol.getTransmitterState(subChannelUid) == StateSubChannel.NAV){
                String str = getStringPosition();
                str += "处于NAV状态，ACK需要退避";
                Hprint.printlntDebugInfo(this, str);
                Simulator.addEvent(selfMacProtocol.getEndTimeNAV(subChannelUid) - Simulator.getCurTime()+TimeUnitValue.ps,
                        new IF_Event() {
                            @Override
                            public void run() {
                                sendACK(subChannelUid, destinationUid);
                            }
                        });
                return;
            }
            //若不处于NAV状态，则退避
            else {
                if (this.ACKBackoffTime[index]++ < this.ACKBackoffTimeLimit){
                    String str = "发送ACK时，"+SubChannel.getSubChannel(subChannelUid).getStringUid();
                    str += "被占用，执行第"+this.ACKBackoffTime[index]+"次退避";
                    Hprint.printlntDebugInfo(this, str);
                    Simulator.addEvent(this.backOffs[index].getBackOffTime(),
                            new IF_Event() {
                                @Override
                                public void run() {
                                    sendACK(subChannelUid, destinationUid);
                                }
                            });
                    return;
                }
                else {
                    String str = "在"+SubChannel.getSubChannel(subChannelUid).getStringUid();
                    str += "发送ACK到达退避上限，发送失败";
                    Hprint.printlntDebugInfo(this, str);
                    this.ACKBackoffTime[index] = 0;
                    return;
                }
            }
        }
        //若可发送
        PacketACK ack = new PacketACK(lengthACK);
        ack.setSourceUid(selfMacProtocol.getUid());
        ack.setDestinationUid(destinationUid);
        IF_Event SIFSBeginEvent = new IF_Event() {
            @Override
            public void run() {
                Hprint.printlntDebugInfo(self, "SIFS开始");
            }
        };
        IF_Event SIFSEndEvent = new IF_Event() {
            @Override
            public void run() {
                Hprint.printlntDebugInfo(self, "SIFS结束");
                deleteSIFS(getIndexSubChannel(subChannelUid));
                SendPacket sendPacket = new SendPacket(subChannelUid, ack, null, null);
            }
        };
        addSIFS(subChannelUid, timeSIFS, SIFSBeginEvent, SIFSEndEvent);
    }

    /**
     * 设置信道
     * @param channel 信道
     */
    public void setChannel(Channel channel){
        this.difss = new DIFS[channel.getSumSubChannel()];
        this.sifss = new SIFS[channel.getSumSubChannel()];
        this.backOffs = new BackOff[channel.getSumSubChannel()];
        this.subChannelUids = new int[channel.getSumSubChannel()];
        //DATA
        this.DATABackoffTime = new int[channel.getSumSubChannel()];
        this.difsReTryDATA = new int[channel.getSumSubChannel()];
        this.dataReTrans = new int[channel.getSumSubChannel()];
        this.dataReTransEventUid = new int[channel.getSumSubChannel()];
        //RTS
        this.RTSBackoffTime = new int[channel.getSumSubChannel()];
        this.difsReTryRTS = new int[channel.getSumSubChannel()];
        this.rtsReTrans = new int[channel.getSumSubChannel()];
        this.rtsReTransEventUid = new int[channel.getSumSubChannel()];
        channel.writeSubChannelUid(this.subChannelUids);
        for (int i = 0 ; i < channel.getSumSubChannel() ; i ++){
            this.backOffs[i] = new BackOff(this.backoffBaseTime);
            this.dataReTransEventUid[i] = -1;
            this.rtsReTransEventUid[i] = -1;
        }
    }

    public void deleteRTSReTransEvent(int index){
        String str = "删除RTS重发"+Simulator.deleteEvent(this.rtsReTransEventUid[index]);
        Hprint.printlntDebugInfo(this, str);
    }
    public void deleteDATAReTransEvent(int index){
        String str = "删除DATA重发"+Simulator.deleteEvent(this.dataReTransEventUid[index]);
        Hprint.printlntDebugInfo(this, str);
    }
    /**
     * 获取对应子信道的uid
     * @param index 序号
     * @return 子信道的uid
     */
    public int getSubChannelUid(int index){
        return this.subChannelUids[index];
    }

    /**
     * 获取对应uid子信道的index
     * @param uid 子信道的uid
     * @return index
     */
    public int getIndexSubChannel(int uid){
        for (int i = 0 ; i < subChannelUids.length ; i++){
            if (subChannelUids[i] == uid){
                return i;
            }
        }
        return -1;
    }

    /**
     * 获取位置字符串
     * @return 字符串
     */
    @Override
    public String getStringPosition() {
        return selfMacProtocol.getStringUid()+"/MPSendPacket# ";
    }

    /**
     * 添加一个DIFS
     * @param indexSubChannel 所在信道
     * @param duration 持续时间
     * @param beginEvent 开始时执行内容
     * @param endEvent 结束时执行内容
     * @return 返回是否添加成功
     */
    public boolean addDIFS(int indexSubChannel, double duration, IF_Event beginEvent, IF_Event endEvent){
        if (this.difss[indexSubChannel] != null){
            Hprint.printlntErrorInfo(this, getStringPosition()+"添加DIFS错误，已经存在");
            return false;
        }
        this.difss[indexSubChannel] = new DIFS(duration, beginEvent, endEvent);
        return true;
    }

    /**
     * 删除DIFS
     * @param indexSubChannel 信道
     */
    public void deleteDIFS(int indexSubChannel){
        this.difss[indexSubChannel] = null;
    }

    /**
     * 添加一个SIFS
     * @param indexSubChannel 所在信道
     * @param duration 持续时间
     * @param beginEvent 开始执行事件
     * @param endEvent 结束执行事件
     * @return 是否添加成功
     */
    public boolean addSIFS(int indexSubChannel, double duration, IF_Event beginEvent, IF_Event endEvent){
        if (this.sifss[indexSubChannel] != null){
            Hprint.printlntErrorInfo(this, getStringPosition()+"添加SIFS错误，已经存在");
            return false;
        }
        this.sifss[indexSubChannel] = new SIFS(duration, beginEvent, endEvent);
        return true;
    }
    /**
     * 删除SIFS
     * @param indexSubChannel 信道
     */
    public void deleteSIFS(int indexSubChannel){
        this.sifss[indexSubChannel] = null;
    }

    /**
     * 打断DIFS
     * @param indexSubChannel 所在信道
     */
    public void disturbDIFS(int indexSubChannel){
        this.difss[indexSubChannel].disturb();
    }

    /**
     * 清理DIFS数组
     * @param indexSubChannel 所在信道
     */
    public void clearDIFS(int indexSubChannel){
        this.difss[indexSubChannel] = null;
    }
    private class DIFS extends InterFrameSpacing{
        private boolean isDisturb;

        private DIFS(double timeIFS, IF_Event beginEvent, IF_Event endEvent) {
            super(InterFrameSpacingType.DIFS, timeIFS, beginEvent, endEvent);
            this.isDisturb = false;
        }
        private void disturb(){
            this.isDisturb = true;
        }
    }
    private class SIFS extends InterFrameSpacing{

        private SIFS(double timeIFS, IF_Event beginEvent, IF_Event endEvent) {
            super(InterFrameSpacingType.SIFS, timeIFS, beginEvent, endEvent);
        }
    }
    private class SendPacket{
        IF_Event beginEvent;
        IF_Event endEvent;
        int subChannelUid;
        Packet packet;
        public SendPacket(int subChannelUid, Packet packet, IF_Event beginEvent, IF_Event endEvent){
            this.subChannelUid = subChannelUid;
            this.packet = packet;
            this.beginEvent = beginEvent;
            this.endEvent = endEvent;
            Simulator.addEvent(0, new SendPacketBegin());
        }
        class SendPacketBegin implements IF_Event{
            @Override
            public void run() {
                String str = "begin sending "+packet.getStringUid()+" on ";
                str += SubChannel.getSubChannel(subChannelUid).getStringUid();
                Hprint.printlntDebugInfo(self, str);
                double transTime = SubChannel.getSubChannel(subChannelUid).send(packet);
                Simulator.addEvent(transTime, new SendPacketEnd());
                selfMacProtocol.mpChannel.turnToSEND(subChannelUid, transTime);
                if (beginEvent != null){
                    beginEvent.run();
                }
            }
        }
        class SendPacketEnd implements IF_Event{
            @Override
            public void run() {
                String str = "finish sending "+packet.getStringUid()+" on ";
                str += SubChannel.getSubChannel(subChannelUid).getStringUid();
                Hprint.printlntDebugInfo(self, str);
                if (endEvent != null){
                    endEvent.run();
                }
            }
        }
    }
}
/**
 * 帧间隔
 */
class InterFrameSpacing{
    InterFrameSpacingType type;
    double timeIFS;
    IF_Event beginEvent;
    IF_Event endEvent;
    InterFrameSpacing(InterFrameSpacingType type, double timeIFS, IF_Event beginEvent, IF_Event endEvent){
        this.type = type;
        this.timeIFS = timeIFS;
        this.beginEvent = beginEvent;
        this.endEvent = endEvent;
        Simulator.addEvent(0, new IFSBegin());
    }
    class IFSBegin implements IF_Event{
        @Override
        public void run(){
            if (beginEvent != null){
                beginEvent.run();
            }
            Simulator.addEvent(timeIFS, new IFSEnd());
        }
    }
    class IFSEnd implements IF_Event{
        @Override
        public void run(){
            if (endEvent != null){
                endEvent.run();
            }
        }
    }
}