package ao.argosidps.capture

import java.net.InetAddress
import org.pcap4j.core.PcapNetworkInterface
import org.pcap4j.core.Pcaps
import org.pcap4j.packet.IpV4Packet

fun startCapture() {
    val addr = InetAddress.getByName("127.0.0.1")
    val nif: PcapNetworkInterface = Pcaps.getDevByAddress(addr)
    val snapLen = Int.MAX_VALUE
    val mode: PcapNetworkInterface.PromiscuousMode = PcapNetworkInterface.PromiscuousMode.PROMISCUOUS
    val timeout = 10
    val handle = nif.openLive(snapLen, mode, timeout)
    val packet = handle.nextPacket
    handle.close()
    val ipv4Packet = packet.get<IpV4Packet>(IpV4Packet::class.java)
    val inetAddress = ipv4Packet.header.srcAddr
    println("Source Addr: $inetAddress")
}