package han_multiChannelMacProtocol;

import han_simulator.IF_HprintNode;
import han_simulator.IF_Simulator;

/**
 * Created by ycqfeng on 2017/1/11.
 */
public class SubChannel implements IF_Simulator, IF_HprintNode{
    private static int uidBase = 0;
    private static SubChannel[] subChannels;
    private int uid;

    private Channel channel;

    public SubChannel(Channel channel){
        this.uid = uidBase++;
        this.channel = channel;
        addSubChannel(this);

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
}
