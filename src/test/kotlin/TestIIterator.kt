import fr.univ_lille.iut_info.IIterator
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class TestIIterator {
    @Test
    fun one() {
        assertEquals(IIterator.singleton("Test").toList(), listOf("Test"))
        assertEquals(IIterator.fromList(listOf("Test")).toList(), listOf("Test"))
        assertEquals(IIterator.flat(IIterator.fromList(listOf("Test"))).toList(), listOf("Test"))
        assertEquals(IIterator.flat(IIterator.empty(), IIterator.fromList(listOf("Test"))).toList(), listOf("Test"))
        assertEquals(
            IIterator.flat(
                IIterator.empty(),
                IIterator.empty(),
                IIterator.empty(),
                IIterator.empty(),
                IIterator.fromList(listOf("Test")),
                IIterator.empty(),
                IIterator.empty(),
                IIterator.empty()
            ).toList(), listOf("Test")
        )
    }

    @Test
    fun concat() {

        assertEquals(IIterator.fromList(listOf("Test1", "Test2")).toList(), listOf("Test1", "Test2"))
        assertEquals(
            IIterator.flat(IIterator.fromList(listOf("Test1")), IIterator.fromList(listOf("Test2"))).toList(),
            listOf("Test1", "Test2")
        )
        assertEquals(
            IIterator.flat(IIterator.empty(), IIterator.fromList(listOf("Test1", "Test2"))).toList(),
            listOf("Test1", "Test2")
        )
        assertEquals(
            IIterator.flat(
                IIterator.empty(),
                IIterator.empty(),
                IIterator.empty(),
                IIterator.empty(),
                IIterator.fromList(listOf("Test1")),
                IIterator.empty(),
                IIterator.empty(),
                IIterator.fromList(listOf("Test2")),
                IIterator.empty()
            ).toList(), listOf("Test1", "Test2")
        )
    }

    @Test
    fun flattenOrdering() {

        assertEquals(IIterator.fromList(listOf("Test1", "Test2")).toList(), listOf("Test1", "Test2"))
        assertEquals(
            IIterator.flat(IIterator.fromList(listOf("Test1")), IIterator.fromList(listOf("Test2"))).toList(),
            listOf("Test1", "Test2")
        )
        assertEquals(
            IIterator.flat(IIterator.empty(), IIterator.fromList(listOf("Test1", "Test2"))).toList(),
            listOf("Test1", "Test2")
        )
        assertEquals(
            IIterator.flat(
                IIterator.empty(),
                IIterator.empty(),
                IIterator.empty(),
                IIterator.empty(),
                IIterator.fromList(listOf("Test1")),
                IIterator.empty(),
                IIterator.empty(),
                IIterator.fromList(listOf("Test2")),
                IIterator.empty()
            ).toList(), listOf("Test1", "Test2")
        )
    }

    @Test
    fun treeLikeRun() {
        var count = 0;
        assertEquals(
            IIterator.singleton(IIterator.singleton(count++))
                .flatMapI {
                    IIterator.iterator(
                        IIterator.flat(it.freshCopy(), IIterator.singleton(count++)),
                        IIterator.flat(it.freshCopy(), IIterator.singleton(count++))
                    )
                }.flatMapI {
                    IIterator.iterator(
                        IIterator.flat(it.freshCopy(), IIterator.singleton(count++)),
                        IIterator.flat(it.freshCopy(), IIterator.singleton(count++))
                    )
                }.map { it.toList() }.toList(),
            listOf(listOf(0, 1, 3), listOf(0, 1, 4), listOf(0, 2, 5), listOf(0, 2, 6))
        )
    }
}