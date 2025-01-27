package com.github.lipen.satlib.solver

import com.github.lipen.satlib.core.Lit
import com.github.lipen.satlib.core.LitArray
import com.github.lipen.satlib.core.Model
import com.github.lipen.satlib.jni.solver.JGlucose
import java.io.File

@Suppress("MemberVisibilityCanBePrivate")
class GlucoseSolver @JvmOverloads constructor(
    val simpStrategy: SimpStrategy = SimpStrategy.ONCE,
    val backend: JGlucose = JGlucose(),
) : AbstractSolver() {
    private var simplified = false

    constructor(
        simpStrategy: SimpStrategy = SimpStrategy.ONCE,
        initialSeed: Double? = null,
        initialRandomVarFreq: Double? = null,
        initialRandomPolarities: Boolean = false,
        initialRandomInitialActivities: Boolean = false,
    ) : this(
        simpStrategy = simpStrategy,
        backend = JGlucose(
            initialSeed = initialSeed,
            initialRandomVarFreq = initialRandomVarFreq,
            initialRandomPolarities = initialRandomPolarities,
            initialRandomInitialActivities = initialRandomInitialActivities
        )
    )

    init {
        if (simpStrategy == SimpStrategy.NEVER) {
            backend.eliminate(turn_off_elim = true)
        }
    }

    override fun _reset() {
        backend.reset()
        if (simpStrategy == SimpStrategy.NEVER) {
            backend.eliminate(turn_off_elim = true)
        }
        simplified = false
    }

    override fun _close() {
        backend.close()
    }

    override fun _dumpDimacs(file: File) {
        backend.writeDimacs(file)
    }

    override fun _comment(comment: String) {}

    override fun _newLiteral(outerNumberOfVariables: Int): Lit {
        return backend.newVariable()
    }

    @Suppress("OverridingDeprecatedMember")
    override fun _addClause() {
        @Suppress("deprecation")
        backend.addClause()
    }

    override fun _addClause(lit: Lit) {
        backend.addClause(lit)
    }

    override fun _addClause(lit1: Lit, lit2: Lit) {
        backend.addClause(lit1, lit2)
    }

    override fun _addClause(lit1: Lit, lit2: Lit, lit3: Lit) {
        backend.addClause(lit1, lit2, lit3)
    }

    override fun _addClause(literals: LitArray) {
        backend.addClause_(literals)
    }

    private fun <T> runMatchingSimpStrategy(block: (do_simp: Boolean, turn_off_simp: Boolean) -> T): T {
        return when (simpStrategy) {
            SimpStrategy.ONCE -> block(!simplified, !simplified).also { simplified = true }
            SimpStrategy.ALWAYS -> block(true, false)
            SimpStrategy.NEVER -> block(false, false)
        }
    }

    override fun _solve(): Boolean {
        return runMatchingSimpStrategy { do_simp, turn_off_simp ->
            backend.solve(do_simp, turn_off_simp)
        }
    }

    override fun _solve(assumptions: LitArray): Boolean {
        return runMatchingSimpStrategy { do_simp, turn_off_simp ->
            backend.solve(assumptions, do_simp, turn_off_simp)
        }
    }

    override fun interrupt() {
        backend.interrupt()
    }

    override fun getValue(lit: Lit): Boolean {
        return backend.getValue(lit)
    }

    override fun getModel(): Model {
        return Model.from(backend.getModel(), zerobased = false)
    }

    companion object {
        enum class SimpStrategy {
            NEVER, ONCE, ALWAYS;
        }
    }
}
