package com.company;

import han_multiChannelMacProtocol.Channel;
import han_multiChannelMacProtocol.MacProtocol;
import han_multiChannelMacProtocol.Packet;
import han_multiChannelMacProtocol.PacketType;
import han_simulator.Simulator;

public class Main {

    public static void main(String[] args) {
	// write your code here
        Simulator.init();
        Simulator.setStopTime(10);

        Channel channel = new Channel();
        channel.setSubChannelNum(10);

        MacProtocol macProtocol = new MacProtocol();
        macProtocol.setChannel(channel);

        macProtocol.enQueue(new Packet(300, PacketType.DATA));

        Simulator.start();
    }
}
