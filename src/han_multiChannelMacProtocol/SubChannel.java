package han_multiChannelMacProtocol;

import han_simulator.*;

/**
 * Created by ycqfeng on 2017/1/11.
 */
public class SubChannel implements IF_Simulator, IF_HprintNode{
    private static int uidBase = 0;
    private static SubChannel[] subChannels;
    private int uid;

    private Channel channel;
    private IF_Channel[] devices;

    private double bps = 1000000;
    private double delay = 50*TimeUnitValue.us;

    public SubChannel(Channel channel){
        Hprint.register(this);
        this.uid = uidBase++;
        this.channel = channel;
        addSubChannel(this);
    }

    public double send(Packet packet){
        String str = "发送"+packet.getStringDetailUid();
        Hprint.printlntDebugInfo(this,str);
        Simulator.addEvent(0, new IF_Event() {
            @Override
            public void run() {
                for (int i = 0 ; i < devices.length ; i++){
                    IF_Channel device = devices[i];
                    Simulator.addEvent(delay, new IF_Event() {
                        @Override
                        public void run() {
                            device.receive(getUid(), packet);
                        }
                    });
                }
            }
        });
        return packet.getLength()/bps;
    }

    public double getTransTime(Packet packet){
        return packet.getLength()/bps;
    }

    public int getUid(){
        return uid;
    }

    public String getStringUid(){
        return "SubChannel("+uid+")";
    }

    @Override
    public String getStringPosition() {
        return channel.getStringUid()+"/"+getStringUid()+"# ";
    }
    //静态函数
    private static void addSubChannel(SubChannel subChannel){
        if (subChannels == null){
            subChannels = new SubChannel[1];
            subChannels[0] = subChannel;
            return;
        }
        else {
            for (int i = 0 ; i < subChannels.length ; i++){
                if (subChannels[i].uid == subChannel.uid){
                    return;
                }
            }
            SubChannel[] temp = new SubChannel[subChannels.length+1];
            System.arraycopy(subChannels, 0, temp, 0, subChannels.length);
            temp[subChannels.length] = subChannel;
            subChannels = temp;
        }
    }
    public static SubChannel getSubChannel(int uid){
        for (int i = 0 ; i < subChannels.length ; i++){
            if (subChannels[i].uid == uid){
                return subChannels[i];
            }
        }
        return null;
    }
    public static void addDeviceInterface(int subChannelUid, IF_Channel device){
        SubChannel subChannel = getSubChannel(subChannelUid);
        if (subChannel.devices == null){
            subChannel.devices = new IF_Channel[1];
            subChannel.devices[0] = device;
        }
        else {
            for (int i = 0 ; i < subChannel.devices.length ; i++){
                if (subChannel.devices[i] == device){
                    return;
                }
            }
            IF_Channel[] temp = new IF_Channel[subChannel.devices.length+1];
            System.arraycopy(subChannel.devices, 0, temp, 0, subChannel.devices.length);
            temp[subChannel.devices.length] = device;
            subChannel.devices = temp;
        }
    }
}
