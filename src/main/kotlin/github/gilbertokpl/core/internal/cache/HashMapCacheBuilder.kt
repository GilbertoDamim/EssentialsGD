package github.gilbertokpl.core.internal.cache

import github.gilbertokpl.core.external.CorePlugin
import github.gilbertokpl.core.external.cache.convert.SerializerBase
import github.gilbertokpl.core.external.cache.interfaces.CacheBuilderV2
import org.bukkit.entity.Player
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.json.JSONObject
import java.io.File
import java.sql.SQLIntegrityConstraintViolationException

internal class HashMapCacheBuilder<T, V, K>(
    private val table: Table,
    private val primaryColumn: Column<String>,
    private val column: Column<T>,
    private val classConvert: SerializerBase<HashMap<V, K>, T>
) : CacheBuilderV2<HashMap<V, K>, V> {

    private val hashMap = HashMap<String, HashMap<V, K>?>()
    private var toUpdate = JSONObject()
    private var jsonPath = ""

    override fun getMap(): Map<String, HashMap<V, K>?> {
        return hashMap.toMap()
    }

    override operator fun get(entity: String): HashMap<V, K>? {
        return hashMap[entity.lowercase()]
    }

    override operator fun get(entity: Player): HashMap<V, K>? {
        return hashMap[entity.name.lowercase()]
    }

    override operator fun set(entity: Player, value: HashMap<V, K>) {
        set(entity.name, value)
    }

    override fun set(entity: String, value: HashMap<V, K>, override: Boolean) {
        if (override) {
            hashMap[entity.lowercase()] = value
            toUpdate.put(entity.lowercase(), classConvert.convertToDatabase(value))
            return
        }
        set(entity, value)
        return
    }

    override operator fun set(entity: String, value: HashMap<V, K>) {
        hashMap[entity.lowercase()] = value
        toUpdate.put(entity.lowercase(), classConvert.convertToDatabase(value))
    }

    override fun remove(entity: Player, value: V) {
        remove(entity.name, value)
    }

    override fun remove(entity: String, value: V) {
        val ent = hashMap[entity.lowercase()] ?: return
        ent.remove(value)
        hashMap[entity.lowercase()] = ent
        toUpdate.put(entity.lowercase(), classConvert.convertToDatabase(ent))
    }

    override fun remove(entity: Player) {
        remove(entity.name.lowercase())
    }

    override fun remove(entity: String) {
        hashMap[entity.lowercase()] = null
        toUpdate.put(entity.lowercase(), emptyMap<V, K>())
    }

    private fun save(list: Set<String>) {
        if (toUpdate.isEmpty) {
            saveJson()
            return
        }

        val existingRows = table.select { primaryColumn inList toUpdate.keySet() }.toList().associateBy { it[primaryColumn] }

        for (i in list) {
            toUpdate.remove(i)
            val value = hashMap[i]

            if (value == null) {
                existingRows[i]?.let { row ->
                    table.deleteWhere { primaryColumn eq row[primaryColumn] }
                }
            } else {
                if (existingRows[i] == null) {
                    try {
                        table.insert {
                            it[primaryColumn] = i
                            it[column] = classConvert.convertToDatabase(value)
                        }
                    } catch (sql : SQLIntegrityConstraintViolationException) {
                        table.update({ primaryColumn eq i }) {
                            it[column] = classConvert.convertToDatabase(value)
                        }
                    }
                } else {
                    table.update({ primaryColumn eq i }) {
                        it[column] = classConvert.convertToDatabase(value)
                    }
                }
            }
        }
        saveJson()
    }

    override fun update() {
        save(toUpdate.keySet())
    }

    override fun load(corePlugin: CorePlugin) {
        jsonPath = "./${corePlugin.mainPath}/sql/internal/HashMapCacheBuilder-${column.name.lowercase()}.json"

        for (row in table.selectAll()) {
            hashMap[row[primaryColumn]] = classConvert.convertToCache(row[column])
        }

        val file = File(jsonPath)

        if (file.exists()) {
            toUpdate = JSONObject(file.readText())

            toUpdate.keys().forEach { key ->
                val value = classConvert.convertToCache(toUpdate[key] as T)
                hashMap[key.lowercase()] = value
            }
        } else {
            File("./${corePlugin.mainPath}/sql/internal").mkdirs()
        }
    }

    override fun unload() {
        save(toUpdate.keySet())
    }

    private fun saveJson() {
        if (toUpdate.isEmpty) {
            File(jsonPath).delete()
        } else {
            File(jsonPath).writeText(toUpdate.toString())
        }
    }
}
