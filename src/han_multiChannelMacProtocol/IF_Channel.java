package han_multiChannelMacProtocol;

/**
 * Created by ycqfeng on 2017/1/14.
 */
public interface IF_Channel {
    public void receive(int subChannelUid, Packet packet);
}
