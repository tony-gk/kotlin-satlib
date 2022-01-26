package nexus.aig

import com.github.lipen.satlib.utils.lineSequence
import com.github.lipen.satlib.utils.useWith
import mu.KotlinLogging
import okio.buffer
import okio.source
import java.io.File

private val log = KotlinLogging.logger {}

private fun findHeader(lines: Iterator<String>): String {
    for (line in lines) {
        if (line.startsWith("c")) {
            log.debug { "Pre-header comment '$line'" }
            // skip comment line
        } else if (line.startsWith("aig") || line.startsWith("aag")) {
            log.debug { "Header '$line'" }
            return line
        } else {
            error("Bad line '$line'")
        }
    }
    error("Could not find header")
}

private fun parseInput(line: String): AigInput {
    val lit = line.toInt()
    check(lit > 0)
    check(isEven(lit))
    return AigInput(lit / 2)
}

private fun parseLatch(line: String) {
    TODO("Parse latch")
}

private fun parseOutput(line: String): Ref {
    val lit = line.toInt()
    check(lit > 0)
    return Ref(lit / 2, negated = isOdd(lit))
}

private fun parseAnd(line: String): AigAndGate {
    val re = Regex("""(\d+) (\d+) (\d+)""")
    val m = re.matchEntire(line)
        ?: error("Could not match AND gate definition in '$line'")
    // log.debug("AND '$line'")
    val (i, left, right) = m.destructured.toList().map { it.toInt() }

    check(i > 0)
    check(isEven(i))
    check(left > 0)
    check(right > 0)

    return AigAndGate(i / 2, Ref.fromLiteral(left), Ref.fromLiteral(right))
}

fun parseAig(filename: String): Aig {
    log.info("Parsing AIG from '$filename'")

    File(filename).source().buffer().useWith {
        val lines = lineSequence().iterator()

        // header: aag M I L O A
        // M = maximum variable index
        // I = number of inputs
        // L = number of latches
        // O = number of outputs
        // A = number of AND gates
        // M == I + L + A
        val header = findHeader(lines)
        val parts = header.split(" ")
        val format = parts[0]
        val (maxIndex, numInputs, numLatches, numOutputs, numAnds) =
            parts.subList(1, 6).map { it.toInt() }

        if (numLatches > 0) {
            error("Latches are not supported yet")
        }

        return when (format) {
            "aag" -> {
                // ASCII AIG (format='aag')
                // [header]
                // [inputs]
                // [latches]
                // [outputs]
                // [ands]
                // [symbols]
                // c [comment section header]
                // [comments]

                val inputs = (1..numInputs).map { parseInput(lines.next()) }
                val latches = (1..numLatches).map { parseLatch(lines.next()) }
                val outputs = (1..numOutputs).map { parseOutput(lines.next()) }
                val ands = (1..numAnds).map { parseAnd(lines.next()) }
                // TODO: parse symbol table
                // skip the rest

                // check(inputIds.intersect(ands.map { it.id }).isEmpty())

                val mapping = inputs.associateBy { it.id } + ands.associateBy { it.id }

                for (id in mapping.keys) {
                    check(id <= maxIndex) { "Id $id is greater than maxIndex ($maxIndex)" }
                }

                Aig(
                    inputs,
                    outputs,
                    ands,
                    mapping,
                )
            }
            "aig" -> {
                TODO("Binary AIG parsing is not implemented")
            }
            else -> {
                error("Bad format '$format'")
            }
        }
    }
}
