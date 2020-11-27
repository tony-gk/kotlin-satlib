package com.github.lipen.satlib.utils

import com.github.lipen.multiarray.MultiArray
import com.github.lipen.satlib.op.encodeOneHotBinary
import com.github.lipen.satlib.solver.Solver

interface OneHotBinaryDomainVar<T> : DomainVar<T> {
    val bits: List<Lit> // Note: bits[0] is LSB

    fun bit(index: Int): Lit = bits[index]

    companion object {
        @JvmStatic
        inline fun <T> new(
            domain: Iterable<T>,
            bitsProducer: (DomainVar<T>) -> List<Lit>,
            init: (T) -> Lit,
        ): OneHotBinaryDomainVar<T> = DefaultOneHotBinaryDomainVar(domain, bitsProducer, init)
    }
}

class DefaultOneHotBinaryDomainVar<T> @PublishedApi internal constructor(
    onehot: DomainVar<T>,
    override val bits: List<Lit>,
) : OneHotBinaryDomainVar<T>, DomainVar<T> by onehot {
    override fun bit(index: Int): Lit = bits[index]

    override fun toString(): String {
        return "OneHotBinaryDomainVar(domain = $domain)"
    }

    companion object {
        inline operator fun <T> invoke(
            domain: Iterable<T>,
            bitsProducer: (DomainVar<T>) -> List<Lit>,
            init: (T) -> Lit,
        ): DefaultOneHotBinaryDomainVar<T> {
            val onehot = DomainVar.new(domain, init)
            val bits = bitsProducer(onehot)
            return DefaultOneHotBinaryDomainVar(onehot, bits)
        }
    }
}

typealias OneHotBinaryIntVar = OneHotBinaryDomainVar<Int>
typealias OneHotBinaryIntVarArray = MultiArray<OneHotBinaryIntVar>
typealias OneHotBinaryDomainVarArray<T> = MultiArray<OneHotBinaryDomainVar<T>>

fun <T> Solver.newOneHotBinaryDomainVar(
    domain: Iterable<T>,
    init: (T) -> Lit = { newLiteral() },
): OneHotBinaryDomainVar<T> = OneHotBinaryDomainVar.new(domain, ::encodeOneHotBinary, init)

fun Solver.newOneHotBinaryIntVar(
    domain: Iterable<Int>,
    init: (Int) -> Lit = { newLiteral() },
): OneHotBinaryIntVar = newOneHotBinaryDomainVar(domain, init)

fun <T> Solver.newOneHotBinaryDomainVarArray(
    vararg shape: Int,
    init: (T) -> Lit = { newLiteral() },
    domain: (IntArray) -> Iterable<T>,
): OneHotBinaryDomainVarArray<T> = OneHotBinaryDomainVarArray.create(shape) { index ->
    newOneHotBinaryDomainVar(domain(index), init)
}

fun Solver.newOneHotBinaryIntVarArray(
    vararg shape: Int,
    init: (Int) -> Lit = { newLiteral() },
    domain: (IntArray) -> Iterable<Int>,
): OneHotBinaryIntVarArray = OneHotBinaryIntVarArray.create(shape) { index ->
    newOneHotBinaryIntVar(domain(index), init)
}
