package ao.argosidps.capture

import ao.argosidps.configurations.Configuration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import java.net.InetAddress
import org.pcap4j.core.PcapNetworkInterface
import org.pcap4j.core.Pcaps
import org.pcap4j.packet.IpV4Packet
import org.pcap4j.packet.TcpPacket
import org.pcap4j.packet.UdpPacket
import org.pcap4j.packet.namednumber.IpNumber
import java.io.File
import java.sql.Timestamp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


suspend fun startCapture() {
    //val addr = InetAddress.getByName("127.0.0.1")
    val nif: PcapNetworkInterface = Pcaps.getDevByAddress(InetAddress.getByName(Configuration.nifAddr)) //Pcaps.getDevByAddress(addr)
    val snapLen = Int.MAX_VALUE
    val mode: PcapNetworkInterface.PromiscuousMode = PcapNetworkInterface.PromiscuousMode.PROMISCUOUS
    val timeout = 10
    val handle = nif.openLive(snapLen, mode, timeout)
    var beforeCaptureTimestamp: Long
    var afterCaptureTimestamp: Long
    val dumpPath = "/var/log/argos"
    File(dumpPath).mkdirs() // creates /var/log/argos if the folder does not exist
    val dateFormater = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss")
    val dumpFile = "${dumpPath}/captures_${LocalDateTime.now().format(dateFormater)}.pcap"
    val dumper = handle.dumpOpen(dumpFile)

    println("Capturing from ${nif.name}")
    supervisorScope{
        while (!false) {
            //println("==================== Data ====================")
            val packet = handle.nextPacket
            if (packet == null) continue
            // I'll only work with IPV4 for now
            beforeCaptureTimestamp = handle.timestamp.time
            val ipv4Packet = packet.get<IpV4Packet>(IpV4Packet::class.java)
            afterCaptureTimestamp = handle.timestamp.time
            if (ipv4Packet != null)
                launch(Dispatchers.IO){
                    updateFlowStats(ipv4Packet, beforeCaptureTimestamp, afterCaptureTimestamp)
                }
            val packetTimestamp = handle.timestamp
            launch (Dispatchers.IO) {
                dumper.dump(packet, packetTimestamp)
            }
        }
        //println("$ipv4Packet")
    }
    //val inetAddress = ipv4Packet.header.srcAddr
    dumper.close()
    handle.close()
}