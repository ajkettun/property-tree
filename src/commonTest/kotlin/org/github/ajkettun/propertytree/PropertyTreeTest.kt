package org.github.ajkettun.propertytree

import org.github.ajkettun.propertytree.PropertyTree.Companion.propertyNodeOf
import kotlin.test.*

class PropertyTest {
    @Test
    fun isSet() {
        assertTrue(propertyWithData(linkedSetOf(true)).isSet)
        assertFalse(propertyWithData(linkedSetOf(false)).isSet)
        assertFalse(propertyWithData(linkedSetOf()).isSet)
        assertFalse(propertyWithData(linkedSetOf(true, false)).isSet)
    }

    @Test
    fun isNotSet() {
        assertFalse(propertyWithData(linkedSetOf(true)).isNotSet)
        assertTrue(propertyWithData(linkedSetOf(false)).isNotSet)
        assertFalse(propertyWithData(linkedSetOf()).isNotSet)
        assertFalse(propertyWithData(linkedSetOf(true, false)).isNotSet)
    }

    @Test
    fun singleBoolean() {
        assertNull(propertyWithData(linkedSetOf(true, false)).singleBoolean)
        assertNull(propertyWithData(linkedSetOf(true, 2L)).singleBoolean)
        assertTrue(propertyWithData(linkedSetOf(true)).singleBoolean ?: false)
        assertFalse(propertyWithData(linkedSetOf(false)).singleBoolean ?: true)
    }

    @Test
    fun singleLong() {
        assertNull(propertyWithData(linkedSetOf(1L, 2L)).singleLong)
        assertNull(propertyWithData(linkedSetOf(1L, false)).singleLong)
        assertEquals(2L, propertyWithData(linkedSetOf(2L)).singleLong)
    }

    @Test
    fun singleInt() {
        assertNull(propertyWithData(linkedSetOf(1, 2)).singleInt)
        assertNull(propertyWithData(linkedSetOf(1, false)).singleInt)
        assertEquals(2, propertyWithData(linkedSetOf(2)).singleInt)
    }

    @Test
    fun singleString() {
        assertNull(propertyWithData(linkedSetOf("1", "2")).singleString)
        assertNull(propertyWithData(linkedSetOf("1", false)).singleString)
        assertEquals("2", propertyWithData(linkedSetOf("2")).singleString)
    }

    @Test
    fun singleObject() {
        assertNull(propertyWithData(linkedSetOf(linkedMapOf("field" to 2L), linkedMapOf("field" to 3L))).singleObject)
        assertNull(propertyWithData(linkedSetOf(linkedMapOf("field" to 2L), false)).singleObject)
        assertEquals(linkedMapOf("field" to 2L), propertyWithData(linkedSetOf(linkedMapOf("field" to 2L))).singleObject)
    }

    @Test
    fun longs() {
        assertEquals(linkedSetOf(1L, 2L, 3L), propertyWithData(linkedSetOf(1L, 2L, 3L)).longs)
        assertEquals(linkedSetOf(1L, 3L), propertyWithData(linkedSetOf(1L, 4, "2", 3L)).longs)
    }

    @Test
    fun strings() {
        assertEquals(linkedSetOf("1", "2", "3"), propertyWithData(linkedSetOf("1", "2", "3")).strings)
        assertEquals(linkedSetOf("2"), propertyWithData(linkedSetOf(1L, 4, "2", 3L)).strings)
    }

    @Test
    fun ints() {
        assertEquals(linkedSetOf(1, 2, 3), propertyWithData(linkedSetOf(1, 2, 3)).ints)
        assertEquals(linkedSetOf(4), propertyWithData(linkedSetOf(1L, 4, "2", 3L)).ints)
    }

    @Test
    fun objects() {
        assertEquals(
            linkedSetOf(linkedMapOf("field" to 2L)),
            propertyWithData(linkedSetOf(linkedMapOf("field" to 2L))).objects
        )
        assertEquals(
            linkedSetOf(linkedMapOf("field" to 2L)),
            propertyWithData(linkedSetOf(1L, 4, "2", 3L, linkedMapOf("field" to 2L))).objects
        )
    }

    fun propertyWithData(data: Set<Any?>) = Property(name = PropertyName("foo"), data = data)
}

class PropertyTreeTest {
    companion object {
        val testPropertyTree = propertyNodeOf(
            "root", "description", linkedSetOf(1, 2),
            propertyNodeOf("child1", linkedSetOf("foo")),
            propertyNodeOf(
                "child2",
                propertyNodeOf("child3", linkedSetOf(linkedMapOf(Pair("foo", "bar"))))
            )
        )
    }


    @Test
    fun construct() {
        val expected = propertyNode {
            name = "root"
            description = "description"
            data = linkedSetOf(1, 2)

            propertyNode {
                name = "child1"
                data = linkedSetOf("foo")
            }

            propertyNode {
                name = "child2"

                propertyNode {
                    name = "child3"
                }
            }
        }

        val actual =
            propertyNodeOf(
                "root", "description", linkedSetOf(1, 2),
                propertyNodeOf("child1", linkedSetOf("foo")),
                propertyNodeOf(
                    "child2",
                    propertyNodeOf("child3")
                )
            )

        assertEquals(expected, actual)
    }

    @Test
    fun constructWithTypeSafeBuilder() {
        val expected = propertyNodeOf(
            "root", "description", linkedSetOf(1, 2),
            propertyNodeOf("child1", linkedSetOf("foo")),
            propertyNodeOf(
                "child2",
                propertyNodeOf("child3")
            )
        )

        val actual = propertyNode {
            name = "root"
            description = "description"
            data = linkedSetOf(1, 2)

            propertyNode {
                name = "child1"
                data = linkedSetOf("foo")
            }

            propertyNode {
                name = "child2"

                propertyNode {
                    name = "child3"
                }
            }
        }
        assertEquals(expected, actual)
    }

    @Test
    fun traverse() {
        val traversal = testPropertyTree.traverse().map { it.name }
        assertEquals(listOf("root", "child1", "child2", "child3"), traversal.toList())
    }

    @Test
    fun findAll() {
        val traversal = testPropertyTree.findAll { it.name.startsWith("child") }.map { it.name }
        assertEquals(listOf("child1", "child2", "child3"), traversal.toList())
    }
}