package ao.argosidps.display

import ao.argosidps.colors.*

// Column widths (characters)
private const val W_NO    =  5
private const val W_PROTO =  6
private const val W_DUR   = 11
private const val W_BPS   = 13
private const val W_PPS   = 11
private const val W_FWD   =  6
private const val W_BWD   =  6
private const val W_APKT  =  9
private const val W_SYN   =  6
private const val W_FIN   =  6
private const val W_RST   =  6
private const val W_IAT   = 11
private const val W_RESULT = 24

private fun String.col(w: Int) = this.take(w).padEnd(w)
private fun Double.fmt(decimals: Int = 2) = "%.${decimals}f".format(this)
private fun Int.col(w: Int)    = this.toString().padStart(w)

/** Prints the Wireshark-style column header. Call once at start and on resume. */
fun printTableHeader() {
    val sep = "${DIM}${CYAN}" + "-".repeat(W_NO + 1) + "+" +
              "-".repeat(W_PROTO + 2) + "+" +
              "-".repeat(W_DUR + 2)   + "+" +
              "-".repeat(W_BPS + 2)   + "+" +
              "-".repeat(W_PPS + 2)   + "+" +
              "-".repeat(W_FWD + 2)   + "+" +
              "-".repeat(W_BWD + 2)   + "+" +
              "-".repeat(W_APKT + 2)  + "+" +
              "-".repeat(W_SYN + 2)   + "+" +
              "-".repeat(W_FIN + 2)   + "+" +
              "-".repeat(W_RST + 2)   + "+" +
              "-".repeat(W_IAT + 2)   + "+" +
              "-".repeat(W_RESULT + 2)+ "${RESET}"
    
    println()
    println(sep)
    println(
        "${CYAN}${BOLD}" +
        " ${"No.".padEnd(W_NO)} | ${"Proto".col(W_PROTO)} | ${"Dur(ms)".col(W_DUR)} |" +
        " ${"Bytes/s".col(W_BPS)} | ${"Pkts/s".col(W_PPS)} | ${"FWD".col(W_FWD)} |" +
        " ${"BWD".col(W_BWD)} | ${"AvgPkt".col(W_APKT)} | ${"SYN".col(W_SYN)} |" +
        " ${"FIN".col(W_FIN)} | ${"RST".col(W_RST)} | ${"IAT-Mean".col(W_IAT)} |" +
        " ${"Result".col(W_RESULT)} |$RESET"
    )
    println(sep)
}

/**
 * Prints one Wireshark-style data row.
 *
 * @param flowData  array of 13 doubles: duration, bytes/s, pkts/s, fwd, bwd,
 *                  avgPkt, fwdMax, bwdMax, syn, fin, rst, iatMean, iatStd
 * @param protocol  e.g. "TCP", "UDP", "ICMP"
 * @param status    "NORMAL" or "ANOMALIA"
 * @param attackType attack class name returned by the model
 */
fun printFlowRow(
    flowData:   Array<Double>,
    protocol:   String,
    status:     String,
    attackType: String
) {
    val no     = DisplayState.rowCounter.incrementAndGet()
    val isAnomaly = status.contains("ANOMALIA", ignoreCase = true)
    val resultColor = if (isAnomaly) RED else GREEN
    val rowColor    = if (isAnomaly) "$RED$BOLD" else RESET

    val resultText = if (isAnomaly) {
        if (attackType.isNotEmpty()) "ANOMALIA: $attackType" else "ANOMALIA"
    } else {
        "NORMAL"
    }

    val row = "${rowColor}" +
        " ${no.toString().padStart(W_NO)} |" +
        " ${protocol.col(W_PROTO)} |" +
        " ${flowData[0].fmt(1).col(W_DUR)} |" +
        " ${flowData[1].fmt(1).col(W_BPS)} |" +
        " ${flowData[2].fmt(2).col(W_PPS)} |" +
        " ${flowData[3].toInt().toString().col(W_FWD)} |" +
        " ${flowData[4].toInt().toString().col(W_BWD)} |" +
        " ${flowData[5].fmt(1).col(W_APKT)} |" +
        " ${flowData[8].toInt().toString().col(W_SYN)} |" +
        " ${flowData[9].toInt().toString().col(W_FIN)} |" +
        " ${flowData[10].toInt().toString().col(W_RST)} |" +
        " ${flowData[11].fmt(1).col(W_IAT)} |" +
        " ${resultColor}${resultText.col(W_RESULT)}${rowColor} |" +
        RESET

    println(row)
}
