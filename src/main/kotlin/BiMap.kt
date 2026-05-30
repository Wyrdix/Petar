package fr.univ_lille.iut_info

open class BiMap<K,T>(open val map1: Map<K,T>, open val map2: Map<T,K>) : Map<K,T> by map1 {
    fun getReversed(key: T): K? {
        return map2[key]
    }
}

class MutableBiMap<K,T>(override val map1: MutableMap<K,T> = HashMap(), override val map2: MutableMap<T,K> = HashMap()) : BiMap<K, T>(map1,map2), MutableMap<K,T> by map1 {

    override fun put(key: K, value: T): T? {
        map2[value] = key
        return map1.put(key, value)
    }

    override fun remove(key: K): T? {
        return map1.remove(key)?.apply { map2.remove(this) }
    }

    override fun remove(key: K, value: T): Boolean {
        map2.remove(value, key)
        return map1.remove(key, value)
    }

    override fun putAll(from: Map<out K, T>) {
        map1.putAll(from)
        map2.putAll(from.entries.map { Pair(it.value, it.key) })
    }

    override fun clear() {
        map1.clear();
        map2.clear();
    }

    override fun containsKey(key: K): Boolean {
        return map1.containsKey(key)
    }

    override fun containsValue(value: T): Boolean {
        return map1.containsValue(value)
    }

    override fun isEmpty(): Boolean {
        return map1.isEmpty()
    }

    override val entries: MutableSet<MutableMap.MutableEntry<K, T>>
        get() = map1.entries
    override val size: Int
        get() = map1.size

    override fun get(key: K): T? {
        return map1[key]
    }
    override val keys: MutableSet<K>
        get() = map1.keys
    override val values: MutableCollection<T>
        get() = map1.values
}