/**
 * Copyright © 2020, Darya Grechishkina, Konstantin Chukharev, ITMO University
 */

package com.github.lipen.satlib.jni.solver

import com.github.lipen.satlib.jni.Loader
import com.github.lipen.satlib.utils.useWith
import java.io.File

@Suppress("PropertyName", "FunctionName", "MemberVisibilityCanBePrivate", "unused")
class JGlucose(
    val initialSeed: Double? = null, // default is 91648253
    val initialRandomVarFreq: Double? = null,
    val initialRandomPolarities: Boolean = false,
    val initialRandomInitialActivities: Boolean = false,
) : AutoCloseable {
    private var handle: Long = 0
    private var solvable: Boolean = false

    val numberOfVariables: Int get() = glucose_nvars(handle)
    val numberOfClauses: Int get() = glucose_nclauses(handle)
    val numberOfLearnts: Int get() = glucose_nlearnts(handle)
    val numberOfDecisions: Long get() = glucose_decisions(handle)
    val numberOfPropagations: Long get() = glucose_propagations(handle)
    val numberOfConflicts: Long get() = glucose_conflicts(handle)

    init {
        reset()
    }

    fun reset() {
        if (handle != 0L) glucose_dtor(handle)
        handle = glucose_ctor()
        if (handle == 0L) throw OutOfMemoryError("glucose_ctor returned NULL")
        if (initialSeed != null) setRandomSeed(initialSeed)
        if (initialRandomVarFreq != null) setRandomVarFreq(initialRandomVarFreq)
        if (initialRandomPolarities) setRandomPolarities(true)
        if (initialRandomInitialActivities) setRandomInitialActivities(true)
        solvable = true
    }

    override fun close() {
        if (handle != 0L) glucose_dtor(handle)
        handle = 0
    }

    fun isIncremental(): Boolean {
        return glucose_is_incremental(handle)
    }

    fun setIncremental() {
        glucose_set_incremental(handle)
    }

    fun okay(): Boolean {
        return glucose_okay(handle)
    }

    @JvmOverloads
    fun newVariable(
        polarity: Boolean = true,
        decision: Boolean = true,
        frozen: Boolean = true,
    ): Int {
        val lit = glucose_new_var(handle, polarity, decision)
        if (frozen) freeze(lit)
        return lit
    }

    fun setPolarity(lit: Int, polarity: Boolean) {
        glucose_set_polarity(handle, lit, polarity)
    }

    fun setDecision(lit: Int, decision: Boolean) {
        glucose_set_decision(handle, lit, decision)
    }

    fun setFrozen(lit: Int, frozen: Boolean) {
        glucose_set_frozen(handle, lit, frozen)
    }

    fun freeze(lit: Int) {
        setFrozen(lit, true)
    }

    fun simplify(): Boolean {
        return glucose_simplify(handle)
    }

    @JvmOverloads
    fun eliminate(turn_off_elim: Boolean = false): Boolean {
        return glucose_eliminate(handle, turn_off_elim)
    }

    fun isEliminated(lit: Int): Boolean {
        return glucose_is_eliminated(handle, lit)
    }

    fun setRandomSeed(random_seed: Double) {
        glucose_set_random_seed(handle, random_seed)
    }

    fun setRandomVarFreq(random_var_freq: Double) {
        glucose_set_random_var_freq(handle, random_var_freq)
    }

    fun setRandomPolarities(rnd_pol: Boolean) {
        glucose_set_rnd_pol(handle, rnd_pol)
    }

    fun setRandomInitialActivities(rnd_init_act: Boolean) {
        glucose_set_rnd_init_act(handle, rnd_init_act)
    }

    fun setConfBudget(x: Long) {
        glucose_set_conf_budget(handle, x)
    }

    fun setPropBudget(x: Long) {
        glucose_set_prop_budget(handle, x)
    }

    fun budgetOff() {
        glucose_budget_off(handle)
    }

    fun interrupt() {
        glucose_interrupt(handle)
    }

    fun clearInterrupt() {
        glucose_clear_interrupt(handle)
    }

    fun writeDimacs(path: String) {
        glucose_to_dimacs(handle, path)
    }

    fun writeDimacs(file: File) {
        writeDimacs(file.path)
    }

    @Deprecated(
        "Clause must contain at least one literal!",
        ReplaceWith("addClause(...)")
    )
    fun addClause(): Boolean {
        solvable = glucose_add_clause(handle)
        return solvable
    }

    fun addClause(lit: Int): Boolean {
        solvable = glucose_add_clause(handle, lit)
        return solvable
    }

    fun addClause(lit1: Int, lit2: Int): Boolean {
        solvable = glucose_add_clause(handle, lit1, lit2)
        return solvable
    }

    fun addClause(lit1: Int, lit2: Int, lit3: Int): Boolean {
        solvable = glucose_add_clause(handle, lit1, lit2, lit3)
        return solvable
    }

    fun addClause(vararg literals: Int): Boolean {
        return addClause_(literals)
    }

    fun addClause_(literals: IntArray): Boolean {
        solvable = glucose_add_clause(handle, literals)
        return solvable
    }

    @JvmOverloads
    fun solve(do_simp: Boolean = true, turn_off_simp: Boolean = false): Boolean {
        solvable = glucose_solve(handle, do_simp, turn_off_simp)
        return solvable
    }

    @JvmOverloads
    fun solve(assumptions: IntArray, do_simp: Boolean = true, turn_off_simp: Boolean = false): Boolean {
        solvable = glucose_solve(handle, assumptions, do_simp, turn_off_simp)
        return solvable
    }

    @JvmOverloads
    @JvmName("solveVararg")
    fun solve(vararg assumptions: Int, do_simp: Boolean = true, turn_off_simp: Boolean = false): Boolean {
        return solve(assumptions, do_simp, turn_off_simp)
    }

    @JvmOverloads
    fun solveLimited(assumptions: IntArray, do_simp: Boolean = true, turn_off_simp: Boolean = false): Boolean? {
        return when (val value = glucose_solve_limited(handle, assumptions, do_simp, turn_off_simp)) {
            LBOOL_TRUE -> true
            LBOOL_FALSE -> false
            LBOOL_UNDEF -> null
            else -> error("glucose_solve_limited returned $value")
        }
    }

    fun getValue(lit: Int): Boolean {
        assert(solvable)
        return when (val value = glucose_get_value(handle, lit)) {
            LBOOL_TRUE -> true
            LBOOL_FALSE -> false
            LBOOL_UNDEF -> error("glucose_get_value returned l_Undef")
            else -> error("glucose_get_value returned $value")
        }
    }

    /** Note: resulting array is 1-based. */
    fun getModel(): BooleanArray {
        assert(solvable)
        return glucose_get_model(handle)
            ?: throw OutOfMemoryError("glucose_get_model returned NULL")
    }

    private external fun glucose_ctor(): Long
    private external fun glucose_dtor(handle: Long)
    private external fun glucose_okay(handle: Long): Boolean
    private external fun glucose_is_incremental(handle: Long): Boolean
    private external fun glucose_set_incremental(handle: Long)
    private external fun glucose_nvars(handle: Long): Int
    private external fun glucose_nclauses(handle: Long): Int
    private external fun glucose_nlearnts(handle: Long): Int
    private external fun glucose_decisions(handle: Long): Long
    private external fun glucose_propagations(handle: Long): Long
    private external fun glucose_conflicts(handle: Long): Long
    private external fun glucose_new_var(handle: Long, polarity: Boolean, decision: Boolean): Int
    private external fun glucose_set_polarity(handle: Long, lit: Int, polarity: Boolean)
    private external fun glucose_set_decision(handle: Long, lit: Int, decision: Boolean)
    private external fun glucose_set_frozen(handle: Long, lit: Int, frozen: Boolean)
    private external fun glucose_simplify(handle: Long): Boolean
    private external fun glucose_eliminate(handle: Long, turn_off_elim: Boolean): Boolean
    private external fun glucose_is_eliminated(handle: Long, lit: Int): Boolean
    private external fun glucose_set_random_seed(handle: Long, seed: Double)
    private external fun glucose_set_random_var_freq(handle: Long, random_var_freq: Double)
    private external fun glucose_set_rnd_pol(handle: Long, rnd_pol: Boolean)
    private external fun glucose_set_rnd_init_act(handle: Long, rnd_init_act: Boolean)
    private external fun glucose_set_conf_budget(handle: Long, x: Long)
    private external fun glucose_set_prop_budget(handle: Long, x: Long)
    private external fun glucose_budget_off(handle: Long)
    private external fun glucose_interrupt(handle: Long)
    private external fun glucose_clear_interrupt(handle: Long)
    private external fun glucose_to_dimacs(handle: Long, path: String)
    private external fun glucose_add_clause(handle: Long): Boolean
    private external fun glucose_add_clause(handle: Long, lit: Int): Boolean
    private external fun glucose_add_clause(handle: Long, lit1: Int, lit2: Int): Boolean
    private external fun glucose_add_clause(handle: Long, lit1: Int, lit2: Int, lit3: Int): Boolean
    private external fun glucose_add_clause(handle: Long, literals: IntArray): Boolean

    private external fun glucose_solve(
        handle: Long,
        do_simp: Boolean,
        turn_off_simp: Boolean,
    ): Boolean

    private external fun glucose_solve(
        handle: Long,
        assumptions: IntArray,
        do_simp: Boolean,
        turn_off_simp: Boolean,
    ): Boolean

    private external fun glucose_solve_limited(
        handle: Long,
        assumptions: IntArray,
        do_simp: Boolean,
        turn_off_simp: Boolean,
    ): Byte

    private external fun glucose_get_value(handle: Long, lit: Int): Byte
    private external fun glucose_get_model(handle: Long): BooleanArray?

    companion object {
        init {
            Loader.load("jglucose")
        }

        private const val LBOOL_TRUE: Byte = 0
        private const val LBOOL_FALSE: Byte = 1
        private const val LBOOL_UNDEF: Byte = 2
    }
}

private fun main() {
    @Suppress("DuplicatedCode")
    JGlucose().useWith {
        val x = newVariable()
        val y = newVariable()
        val z = newVariable()

        println("Encoding exactlyOne({x, y, z})")
        addClause(-x, -y)
        addClause(-x, -z)
        addClause(-y, -z)
        addClause(x, y, z)

        println("nvars = $numberOfVariables, nclauses = $numberOfClauses, nlearnts = $numberOfLearnts")
        println("Solving...")
        check(solve()) { "Unexpected UNSAT" }
        println("x = ${getValue(x)}, y = ${getValue(y)}, z = ${getValue(z)}")
        println("model = ${getModel().drop(1)}")

        println("Solving with assumptions...")
        check(solve(x)); println("model = ${getModel().drop(1)}"); check(getValue(x))
        check(solve(y)); println("model = ${getModel().drop(1)}"); check(getValue(y))
        check(solve(z)); println("model = ${getModel().drop(1)}"); check(getValue(z))
        println("Solving with assumptions: OK")
    }
}
