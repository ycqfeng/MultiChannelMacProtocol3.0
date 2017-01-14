package han_multiChannelMacProtocol;

import han_simulator.IF_HprintNode;
import han_simulator.IF_Simulator;

/**
 * Created by ycqfeng on 2017/1/11.
 */
public class Channel implements IF_Simulator, IF_HprintNode{
    private static int uidBase = 0;
    private int uid;

    private SubChannel[] subChannels;

    public void setSubChannelNum(int num){
        SubChannel subChannel;
        for (int i = 0 ; i < num ; i++){
            subChannel = new SubChannel(this);
            addSubChannel(subChannel);
        }
    }

    public void attachTo(IF_Channel device){
        for (int i = 0 ; i < subChannels.length ; i++){
            SubChannel.addDeviceInterface(subChannels[i].getUid(), device);
        }
    }

    public int getSumSubChannel(){
        return subChannels.length;
    }

    public void writeSubChannelUid(int[] uids){
        for (int i = 0 ; i < subChannels.length ; i++){
            uids[i] = subChannels[i].getUid();
        }
    }

    private void addSubChannel(SubChannel subChannel){
        if (subChannels == null){
            subChannels = new SubChannel[1];
            subChannels[0] = subChannel;
        }
        else {
            for (int i = 0 ; i < subChannels.length ; i++){
                if (subChannels[i] == subChannel){
                    return;
                }
            }
            SubChannel[] temp = new SubChannel[subChannels.length+1];
            System.arraycopy(subChannels, 0, temp, 0, subChannels.length);
            temp[subChannels.length] = subChannel;
            subChannels = temp;
        }
    }

    public String getStringUid(){
        return "Channel("+uid+")";
    }
    @Override
    public String getStringPosition() {
        return getStringUid()+"# ";
    }
}
