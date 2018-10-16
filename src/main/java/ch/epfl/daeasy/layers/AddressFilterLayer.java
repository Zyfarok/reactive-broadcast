package ch.epfl.daeasy.layers;

import java.net.DatagramPacket;
import java.net.InetSocketAddress;

import java.util.Set;

import com.google.common.collect.ImmutableSet;

import ch.epfl.daeasy.config.Configuration;
import ch.epfl.daeasy.rxlayers.RxFilterLayer;

public class AddressFilterLayer extends RxFilterLayer<DatagramPacket> {
    public AddressFilterLayer(Configuration cfg) {
        this(cfg.processes.entrySet().stream().filter(entry -> entry.getKey() != cfg.id).map(entry -> entry.getValue())
                .map(process -> process.address).collect(ImmutableSet.toImmutableSet()));
    }

    private AddressFilterLayer(final Set<InetSocketAddress> peers) {
        super(dpacket -> peers.contains(dpacket.getSocketAddress()));
    }
}