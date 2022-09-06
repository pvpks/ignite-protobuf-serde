import org.apache.ignite.Ignite
import org.apache.ignite.Ignition
import org.apache.ignite.cache.QueryEntity
import org.apache.ignite.cache.query.ScanQuery
import org.apache.ignite.cache.query.SqlFieldsQuery
import org.apache.ignite.configuration.CacheConfiguration
import org.apache.ignite.configuration.IgniteConfiguration
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import resources.HumanProto

class HumanTest {
    private var ignite: Ignite? = null

    @BeforeEach
    fun init() {
        System.setProperty("IGNITE_QUIET", "false");
        ignite = igniteInstance()
    }

    @Test
    fun `test obj with proto and key as long`() {
        println("--------- START ---------")
        val humanBuilder = HumanProto.Human.newBuilder()
        val human1 = humanBuilder.setName("h1").setAge(30).setPlaceOfBirth("India").build()
        val human2 = humanBuilder.setName("h2").setAge(50).setPlaceOfBirth("America").build()
        val human3 = humanBuilder.setName("h3").setAge(60).setPlaceOfBirth("Europe").build()

        val cacheConfig = genericCacheConfig<Long, HumanProto.Human>("HUMAN")
        val queryEntity = QueryEntity(Long::class.java, HumanProto.Human::class.java)
        queryEntity.addQueryField("placeOfBirth", Long::class.java.name, null)
        queryEntity.addQueryField("name", Long::class.java.name, null)
        cacheConfig.queryEntities = listOf(queryEntity)
        val cache = ignite!!.getOrCreateCache(cacheConfig)

        cache.put(human1.age, human1)
        cache.put(human2.age, human2)
        cache.put(human3.age, human3)

        val cursor = cache.query(ScanQuery<Long, HumanProto.Human>())
        val iterator = cursor.iterator()
        while(iterator.hasNext()) {
            val currEntry = iterator.next()
            println("NONSQL: Key: ${currEntry.key}, Value: ${currEntry.value.name}")
        }
        cursor.close()
        println("------------------")

        val cursor2 = cache.query(SqlFieldsQuery("SELECT * FROM \"TEST_HUMAN\".\"HUMAN\""))
        val iterator2 = cursor2.iterator()
        if(!iterator2.hasNext()) {
            println("SQL: Nothing found")
        }
        while(iterator2.hasNext()) {
            val currEntry = iterator.next()
            println("SQL: Key: ${currEntry.key}, Value: ${currEntry.value.name}")
        }
        cursor.close()

        // Check logs to see error messages from GridQueryProcess.describeTypeMismatch
        println("Sleeping for 30sec before exiting. See logs above for the error message")
        Thread.sleep(30000)
        println("--------- END ---------")
    }

    private fun <K, V> genericCacheConfig(nameSuffix: String?): CacheConfiguration<K, V> {
        val cacheCfg = CacheConfiguration<K, V>()
        val cacheName = "TEST_$nameSuffix"
        cacheCfg.name = cacheName
        return cacheCfg
    }

    @Synchronized
    private fun igniteInstance(): Ignite {
        val igniteCfg = IgniteConfiguration()
        igniteCfg.isClientMode = false
        ignite = Ignition.start(igniteCfg)
        return ignite!!
    }

    @AfterEach
    fun close() {
        System.clearProperty("IGNITE_QUIET")
        if (ignite != null) {
            ignite!!.destroyCache("TEST_HUMAN")
            ignite!!.close()
        }
    }
}
