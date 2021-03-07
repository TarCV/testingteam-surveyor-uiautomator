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

import androidx.test.uiautomator.UiSelector
import com.github.tarcv.testingteam.surveyor.Evaluator
import net.jqwik.api.*

class UiSelectorProperties {
    private val evaluator = Evaluator()

    @Property
    fun uiSelectorsDontCrash(
        @ForAll("uiSelector") selector: UiSelector
    ) {
        evaluator.evaluateUiSelector(singleRootNode, selector)
    }

    @Provide
    fun uiSelector(): Arbitrary<UiSelector> {
        val methods = UiSelector::class.java
            .methods
            .filter { Object::class.java.methods.none { rootMethod -> it.name == rootMethod.name } }
        return Arbitraries.of(methods)
            .flatMap { method ->
                val parameters = method.parameters
                val expectsPattern = method.name.contains("Matches")
                parameters
                    .map {
                        arbitraryFor(expectsPattern, it.type)
                    }
                    .let { arbitraries ->
                        when(arbitraries.size) {
                            1 -> arbitraries[0].map { listOf(it) }
                            2 -> Combinators.combine(arbitraries[0], arbitraries[1]).`as` { a, b, -> listOf(a, b)}
                            else -> TODO()
                        }
                    }
                    .map {
                        method.invoke(UiSelector(), *it.toTypedArray()) as UiSelector
                    }
            }
    }

    private fun arbitraryFor(expectsPattern: Boolean, it: Class<*>?): Arbitrary<out Any?> {
        return if (it?.isAssignableFrom(UiSelector::class.java) == true) {
            Arbitraries.lazy { uiSelector() }
        } else {
            commonArbitraryFor(it, expectsPattern)
        }
    }
}