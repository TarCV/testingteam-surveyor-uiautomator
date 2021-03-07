/*
 *  UiAutomator plugin for TestingTeam-Surveyor
 *  This program, except the code under src/main/kotlin/android directory, is
 *
 *  Copyright (C) 2021 TarCV
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/
package com.github.tarcv.testingteam.surveyor.uiautomator

import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import com.github.tarcv.testingteam.surveyor.Evaluator
import net.jqwik.api.*
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Modifier

class BySelectorProperties {
    private val evaluator = Evaluator()
    private val defaultBySelector = By.checked(true)
    private val maxDepth = 10

    @Property
    fun bySelectorsDontCrash(
        @ForAll("bySelector") selector: BySelector
    ) {
        evaluator.evaluateBySelector(singleRootNode, selector)
    }

    @Provide
    fun bySelector(): Arbitrary<BySelector> {
        return bySelectorInternal()
    }

    private fun bySelectorInternal(depth: Int = 0): Arbitrary<BySelector> {
        return Arbitraries.of(staticMethods)
            .flatMap { method ->
                if (depth > maxDepth) {
                    return@flatMap Arbitraries.just(defaultBySelector)
                }
                val parameters = method.parameters
                val expectsPattern = method.name.contains("Matches")
                parameters
                    .map {
                        arbitraryFor(expectsPattern, depth + 1, it.type)
                    }
                    .let { arbitraries ->
                        when (arbitraries.size) {
                            1 -> arbitraries[0].map { listOf(it) }
                            2 -> Combinators.combine(arbitraries[0], arbitraries[1]).`as` { a, b -> listOf(a, b) }
                            else -> TODO()
                        }
                    }
                    .map {
                        method.invoke(null, *it.toTypedArray()) as BySelector
                    }
            }
            .flatMap { selector -> maybeAddMethodRecursive(selector, depth + 1) }
    }

    private fun maybeAddMethodRecursive(selector: BySelector, depth: Int): Arbitrary<BySelector> {
        return Arbitraries
            .lazyOf(
                { Arbitraries.just(selector) },
                {
                    addMethod(selector, depth + 1).flatMap {
                        when {
                            it == null -> Arbitraries.just(selector)
                            depth > maxDepth -> Arbitraries.just(defaultBySelector)
                            else -> maybeAddMethodRecursive(it, depth + 1)
                        }
                    }
                }
            )
    }

    // TODO: Kotlin allows Arbitrary<non-nullable BySelector> in return value, why?
    private fun addMethod(selector: BySelector, depth: Int): Arbitrary<BySelector> {
        return Arbitraries.of(methods)
            .flatMap { method ->
                val parameters = method.parameters
                val expectsPattern = method.name.contains("Matches")
                parameters
                    .map {
                        arbitraryFor(expectsPattern, depth + 1, it.type)
                    }
                    .let { arbitraries ->
                        when (arbitraries.size) {
                            1 -> arbitraries[0].map { listOf(it) }
                            2 -> Combinators.combine(arbitraries[0], arbitraries[1]).`as` { a, b -> listOf(a, b) }
                            else -> TODO()
                        }
                    }
                    .map {
                        try {
                            method.invoke(selector, *it.toTypedArray()) as BySelector
                        } catch (e: InvocationTargetException) {
                            if (e.targetException is IllegalStateException) {
                                null
                            } else {
                                throw e.targetException
                            }
                        }
                    }
            }
    }

    private fun arbitraryFor(expectsPattern: Boolean, depth: Int, it: Class<*>?): Arbitrary<out Any?> {
        return if (it?.isAssignableFrom(BySelector::class.java) == true) {
            Arbitraries.lazy { bySelectorInternal(depth + 1) }
        } else {
            commonArbitraryFor(it, expectsPattern)
        }
    }

    companion object {
        val staticMethods = By::class.java
            .methods
            .filter { Modifier.isStatic(it.modifiers) }
            .filter { it.name != "copy"}
        val methods = BySelector::class.java
            .methods
            .filter { Object::class.java.methods.none { rootMethod -> it.name == rootMethod.name } }
    }
}