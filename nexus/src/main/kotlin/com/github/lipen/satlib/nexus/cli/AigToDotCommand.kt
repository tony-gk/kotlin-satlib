package com.github.lipen.satlib.nexus.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.path
import com.github.lipen.satlib.nexus.aig.convertAigToDot
import com.github.lipen.satlib.nexus.aig.parseAig
import com.github.lipen.satlib.nexus.eqgates.searchEqGates
import com.github.lipen.satlib.solver.GlucoseSolver
import com.github.lipen.satlib.utils.writeln
import mu.KotlinLogging
import okio.buffer
import okio.sink
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.div
import kotlin.io.path.nameWithoutExtension
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.random.Random

private val logger = KotlinLogging.logger {}

class AigToDotCommand : CliktCommand() {
    private val pathAig: Path by option(
        "-i", "--input",
        metavar = "<path>",
        help = "File with And-Inverter graph in ASCII AIGER format"
    ).path(canBeDir = false).required()

    private val pathDot: Path by option(
        "-o", "--output",
        metavar = "<path>",
        help = "Path to the resulting DOT (just a dir with --reuse-name, or a complete path without it)"
    ).path().required()

    private val pathPdf: Path? by option(
        "-p",
        "--pdf",
        metavar = "<path>",
        help = "Path to the resulting PDF (just a dir with --reuse-name, or a complete path without it)"
    ).path()

    private val reuseName: Boolean by option(
        "--reuse-name",
        help = "Reuse the name of the input AIG for the resulting DOT/PDF"
    ).flag(
        "--no-reuse-name",
        default = true,
    )

    private val rankByLayers: Boolean by option(
        "--rank-by-layers",
        help = "Rank nodes by layers (from a topological sort)"
    ).flag(
        "--no-rank-by-layers",
        default = true,
    )

    private val computeDisbalance: Boolean by option(
        "--disbalance",
        help = "Compute disbalance of AIG nodes"
    ).flag(
        "--no-disbalance",
        default = false,
    )
    private val sampleSize: Int by option(
        "-s",
        "--sample-size",
        metavar = "<int>",
        help = "Sample size for disbalance computation"
    ).int().default(10000)
    private val randomSeed: Int by option(
        "--random-seed",
        metavar = "<int>",
        help = "Random seed"
    ).int().default(42)

    private val computeEqGates: Boolean by option(
        "--eq-gates",
        help = "Determine equivalent gates"
    ).flag(
        "--no-eq-gates",
        default = false,
    )
    private val solverType: SolverType by solverTypeOption()

    override fun run() {
        val aig = parseAig(pathAig)

        val pathDot = if (reuseName) {
            pathDot / (pathAig.nameWithoutExtension + ".dot")
        } else {
            pathDot
        }
        logger.info("Writing DOT to '$pathDot'...")
        pathDot.parent.createDirectories()
        pathDot.sink().buffer().use {
            val eqIds = if(computeEqGates) {
                val solverProvider = solverType.solverProvider()
                searchEqGates(aig, solverProvider)
            } else {
                emptyList()
            }

            val lines = if (computeDisbalance) {
                fun s(t: Int, f: Int): Double {
                    return t.toDouble() / (t + f)
                }

                fun hex(x: Double): String {
                    require(x in 0.0..1.0)
                    return "%02x".format(min(255, (x * 256).roundToInt()))
                }

                val random = Random(randomSeed)
                logger.info("Computing disbalance table using sampleSize=$sampleSize and randomSeed=$randomSeed...")
                val table = aig._compute(sampleSize, random)

                println("Table for $aig:")
                for ((id, p) in table.entries.sortedBy { (_, p) -> val (t, f) = p; s(t, f) }) {
                    val (t, f) = p
                    println("  - $id: t=$t, f=$f, s=%.3f".format(s(t, f)))
                }

                val nodeLabel = table.mapValues { (_, p) ->
                    val (t, f) = p
                    "\\N:%.3f".format(s(t, f))
                }
                val nodeAddStyle = table.mapValues { (_, p) ->
                    val (t, f) = p
                    val saturation = s(t, f) // saturation
                    check(saturation in 0.0..1.0)
                    val mid = 0.25
                    val power = 3
                    if (saturation > mid) {
                        "style=filled,fillcolor=\"#00ff00${hex(((saturation - mid) / (1 - mid)).pow(power))}\""
                    } else {
                        "style=filled,fillcolor=\"#ff0000${hex(((mid - saturation) / mid).pow(power))}\""
                    }
                }

                convertAigToDot(aig, rankByLayers = rankByLayers, eqIds = eqIds, nodeLabel = nodeLabel, nodeAddStyle = nodeAddStyle)
            } else {
                convertAigToDot(aig, rankByLayers = rankByLayers, eqIds = eqIds)
            }

            for (line in lines) {
                it.writeln(line)
            }
        }

        if (pathPdf != null) {
            val pathPdf = if (reuseName) {
                pathPdf!! / (pathDot.nameWithoutExtension + ".pdf")
            } else {
                pathPdf!!
            }
            logger.info("Rendering DOT to '$pathPdf'")
            pathPdf.parent.createDirectories()
            Runtime.getRuntime().exec("dot -Tpdf \"$pathDot\" -o \"$pathPdf\"").waitFor()
        }
    }
}
