package ao.argosidps.capture

import java.net.InetAddress
import org.pcap4j.core.PcapNetworkInterface
import org.pcap4j.core.Pcaps
import org.pcap4j.packet.IpV4Packet
import org.pcap4j.packet.TcpPacket
import org.pcap4j.packet.UdpPacket
import org.pcap4j.packet.namednumber.IpNumber

private fun getTuple(packet: IpV4Packet): String {
    val srcAddr = packet.header.srcAddr
    val destAddr = packet.header.dstAddr
    val protocol = packet.header.protocol
    val payload = if (protocol == IpNumber.UDP)
        packet.get(UdpPacket::class.java)
    else if (protocol == IpNumber.TCP)
        packet.get(TcpPacket::class.java)
    else
        null // TODO add ICMP support later
    //if (payload == null)
    //    return ""
    return "(Source Addr = $srcAddr, Source Port = ${payload?.header!!.srcPort}, Destination Addr = $destAddr, Destination Port = ${payload?.header!!.dstPort}, Protocol = $protocol)"
}

fun startCapture() {
    val addr = InetAddress.getByName("127.0.0.1")
    val nif: PcapNetworkInterface = Pcaps.getDevByAddress(addr)
    val snapLen = Int.MAX_VALUE
    val mode: PcapNetworkInterface.PromiscuousMode = PcapNetworkInterface.PromiscuousMode.PROMISCUOUS
    val timeout = 10
    val handle = nif.openLive(snapLen, mode, timeout)
    for (x in 1..10) {
        println("==================== Data ====================")
        val packet = handle.nextPacket
        // TODO I'll only work with IPV4 for now
        val ipv4Packet = packet.get<IpV4Packet>(IpV4Packet::class.java)
        println("Timestamp = ${handle.timestamp}: ${getTuple(ipv4Packet)}")
        //println("$ipv4Packet")
    }
    //val inetAddress = ipv4Packet.header.srcAddr
    handle.close()
}