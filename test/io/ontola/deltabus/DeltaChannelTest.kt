/*
 * Copyright (C), Argu BV
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.ontola.deltabus

import io.kotlintest.matchers.string.shouldBeEqualIgnoringCase
import io.kotlintest.specs.StringSpec
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking

@FlowPreview
@ExperimentalCoroutinesApi
class DeltaChannelTest : StringSpec({
    "pipeline should run" {
        val model = """
            <https://id.openraadsinformatie.nl/10> <http://schema.org/name> "Test" <http://purl.org/link-lib/supplant?graph=https%3A%2F%2Fid.openraadsinformatie.nl%2F10> .
            <https://id.openraadsinformatie.nl/10> <http://schema.org/creator> _:123 <http://purl.org/link-lib/supplant?graph=https%3A%2F%2Fid.openraadsinformatie.nl%2F10> .
            <https://id.openraadsinformatie.nl/10> <http://schema.org/tags> <https://id.openraadsinformatie.nl/10#listRoot> <http://purl.org/link-lib/supplant?graph=https%3A%2F%2Fid.openraadsinformatie.nl%2F10> .
            <https://id.openraadsinformatie.nl/10> <http://www.w3.org/2006/vcard/ns#hasOrganizationName> <http://example.com/10> <http://purl.org/link-lib/supplant?graph=https%3A%2F%2Fid.openraadsinformatie.nl%2F10> .
            <https://id.openraadsinformatie.nl/10> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/ns/org#Organization> <http://purl.org/link-lib/supplant?graph=https%3A%2F%2Fid.openraadsinformatie.nl%2F10> .

            _:123 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://schema.org/Person> <http://purl.org/link-lib/supplant?graph=https%3A%2F%2Fid.openraadsinformatie.nl%2F10> .
            _:123 <http://schema.org/name> "P. Person" <http://purl.org/link-lib/supplant?graph=https%3A%2F%2Fid.openraadsinformatie.nl%2F10> .

            <https://id.openraadsinformatie.nl/10#listRoot> <http://www.w3.org/1999/02/22-rdf-syntax-ns#first> "Value 0" <http://purl.org/link-lib/supplant?graph=https%3A%2F%2Fid.openraadsinformatie.nl%2F10> .
            <https://id.openraadsinformatie.nl/10#listRoot> <http://www.w3.org/1999/02/22-rdf-syntax-ns#rest> _:list1 <http://purl.org/link-lib/supplant?graph=https%3A%2F%2Fid.openraadsinformatie.nl%2F10> .

            _:list1 <http://www.w3.org/1999/02/22-rdf-syntax-ns#first> "Value 1" <http://purl.org/link-lib/supplant?graph=https%3A%2F%2Fid.openraadsinformatie.nl%2F10> .
            _:list1 <http://www.w3.org/1999/02/22-rdf-syntax-ns#rest> _:list2 <http://purl.org/link-lib/supplant?graph=https%3A%2F%2Fid.openraadsinformatie.nl%2F10> .

            _:list2 <http://www.w3.org/1999/02/22-rdf-syntax-ns#first> "Value 2" <http://purl.org/link-lib/supplant?graph=https%3A%2F%2Fid.openraadsinformatie.nl%2F10> .
            _:list2 <http://www.w3.org/1999/02/22-rdf-syntax-ns#rest> <http://www.w3.org/1999/02/22-rdf-syntax-ns#nil> <http://purl.org/link-lib/supplant?graph=https%3A%2F%2Fid.openraadsinformatie.nl%2F10> .

            <https://id.openraadsinformatie.nl/10> <http://schema.org/error> "Same subject with different delta graph" <http://purl.org/link-lib/supplant?graph=https%3A%2F%2Fid.openraadsinformatie.nl%2Fother> .
            <https://id.openraadsinformatie.nl/11> <http://schema.org/name> "Different subject no graph" <http://purl.org/link-lib/supplant> .
        """.trimIndent()
        val data = listOf(DeltaMessage(deltaTopic, "", model)).asFlow()

        runBlocking {
            val test = pipeline(data).collect()

            // TODO
            "test" shouldBeEqualIgnoringCase test.toString()
        }
    }
})
