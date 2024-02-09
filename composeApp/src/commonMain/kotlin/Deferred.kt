
typealias DeferredData = suspend () -> ByteArray

class DeferredDataProvider(
    list: List<DeferredData>,
) : Iterable<Int> {

    constructor(provider: (suspend () -> ByteArray)) : this(listOf(provider))

    @Suppress("UNCHECKED_CAST")
    private val list = list.toMutableList() as MutableList<DeferredData?>

    val size = list.size
    suspend fun load(index: Int): ByteArray {
        val func = requireNotNull(list[index]) { "Data with index:$index already loaded!" }
        return func().also {
            list[index] = null
        }
    }

    companion object {
        val Empty = DeferredDataProvider(emptyList())
    }

    override fun iterator(): Iterator<Int> = (0..<size).iterator()
}