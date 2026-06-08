package ao.argosidps.ipblock

import ao.argosidps.capture.startCapture

private var isInitialized = false

private fun createBlockingTable(){
    ProcessBuilder("nft", "add", "table", "inet", "argos")
        .start()
        .waitFor()
    ProcessBuilder("nft", "add", "set", "inet", "argos", "blocklist", "{ type ipv4_addr; flags timeout }")
        .start()
        .waitFor()
    ProcessBuilder("nft", "add", "chain", "inet", "argos", "input", "{ type filter hook input priority 0; }")
        .start()
        .waitFor()
    ProcessBuilder("nft", "add", "chain", "inet", "argos", "forward", "{ type filter hook forward priority 0; }")
        .start()
        .waitFor()
    ProcessBuilder("nft", "add", "rule", "inet", "argos", "input", "ip", "saddr", "@blocklist", "drop")
        .start()
        .waitFor()
    ProcessBuilder("nft", "add", "rule", "inet", "argos", "input", "ip", "daddr", "@blocklist", "drop")
        .start()
        .waitFor()
    ProcessBuilder("nft", "add", "rule", "inet", "argos", "forward", "ip", "saddr", "@blocklist", "drop")
        .start()
        .waitFor()
    ProcessBuilder("nft", "add", "rule", "inet", "argos", "forward", "ip", "daddr", "@blocklist", "drop")
        .start()
        .waitFor()
}

fun blockAddr(addr: String){
    if (!isInitialized) {
        createBlockingTable()
        isInitialized = true
    }
    ProcessBuilder("nft", "add", "element", "inet", "argos", "blocklist", "{ $addr }")
        .start()
        .waitFor()
}