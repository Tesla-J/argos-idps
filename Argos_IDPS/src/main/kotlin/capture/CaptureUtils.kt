package ao.argosidps.capture

import ao.argosidps.configurations.Configuration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetAddress
import org.pcap4j.core.PcapNetworkInterface
import org.pcap4j.core.Pcaps
import org.pcap4j.packet.IpV4Packet
import org.pcap4j.packet.TcpPacket
import org.pcap4j.packet.UdpPacket
import org.pcap4j.packet.namednumber.IpNumber
import java.sql.Timestamp


suspend fun startCapture() {
    //val addr = InetAddress.getByName("127.0.0.1")
    val nif: PcapNetworkInterface = Pcaps.getDevByAddress(InetAddress.getByName(Configuration.nifAddr)) //Pcaps.getDevByAddress(addr)
    val snapLen = Int.MAX_VALUE
    val mode: PcapNetworkInterface.PromiscuousMode = PcapNetworkInterface.PromiscuousMode.PROMISCUOUS
    val timeout = 10
    val handle = nif.openLive(snapLen, mode, timeout)
    var beforeCaptureTimestamp: Long
    var afterCaptureTimestamp: Long

    println("Capturing from ${nif.name}")
    for (x in 1..1000) {
        println("==================== Data ====================")
        val packet = handle.nextPacket
        // TODO I'll only work with IPV4 for now
        beforeCaptureTimestamp = handle.timestamp.time
        val ipv4Packet = packet.get<IpV4Packet>(IpV4Packet::class.java)
        afterCaptureTimestamp = handle.timestamp.time
        withContext(Dispatchers.IO){
            launch{
                updateFlowStats(ipv4Packet, beforeCaptureTimestamp, afterCaptureTimestamp)
            }
        }
        //println("$ipv4Packet")
    }
    //val inetAddress = ipv4Packet.header.srcAddr
    handle.close()
}