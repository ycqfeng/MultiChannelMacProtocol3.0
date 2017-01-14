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

        MacProtocol source = new MacProtocol();
        MacProtocol destination = new MacProtocol();
        source.setChannel(channel);
        destination.setChannel(channel);

        Packet packet = new Packet(200);
        packet.setDestinationUid(destination.getUid());

        source.enQueue(packet);
        source.enQueue(packet);

        Simulator.start();
    }
}
