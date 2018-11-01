package ch.epfl.daeasy.layers;

import ch.epfl.daeasy.config.Configuration;
import ch.epfl.daeasy.rxlayers.RxFilterLayer;

import java.net.DatagramPacket;
import java.net.SocketAddress;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

public class AddressFilterLayer extends RxFilterLayer<DatagramPacket> {
    public AddressFilterLayer(Configuration cfg) {
        this(cfg.processesByPID.entrySet().stream().filter(entry -> !entry.getKey().equals(cfg.id)).map(Entry::getValue)
                .map(process -> process.address).collect(Collectors.toSet()));
    }

    private AddressFilterLayer(final Set<SocketAddress> peers) {
        super(dpacket -> peers.contains(dpacket.getSocketAddress()));
    }
}