import io.github.jan.supacompose.SupabaseClient
import io.github.jan.supacompose.SupabaseClientBuilder
import io.github.jan.supacompose.annotiations.SupaComposeInternal
import io.github.jan.supacompose.createSupabaseClient
import io.github.jan.supacompose.plugins.SupacomposePlugin
import io.github.jan.supacompose.plugins.SupacomposePluginProvider
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class TestPlugin(private val config: Config) : SupacomposePlugin {

    data class Config(var testValue: Boolean = false)

    companion object : SupacomposePluginProvider<Config, TestPlugin> {

        override val key = "test"

        override fun createConfig(init: Config.() -> Unit): Config {
            return Config().apply(init)
        }

        override fun create(supabaseClient: SupabaseClient, config: Config): TestPlugin {
            return TestPlugin(config)
        }

        override fun setup(builder: SupabaseClientBuilder, config: Config) {
            builder.useHTTPS = config.testValue
        }

    }

}

class SupabaseClientTest {

    private val mockEngine = MockEngine { _ ->
        respond("") //ignore for this test
    }

    @Test
    fun testClientBuilderParametersWithHttpsUrl() {
        val client = createMockedSupabaseClient {
            supabaseUrl = "https://example.supabase.co"
            supabaseKey = "somekey"
        }
        assertEquals("example.supabase.co", client.supabaseUrl, "Supabase url should not contain https://")
        assertEquals("somekey", client.supabaseKey, "Supabase key should be set to somekey")
        assertEquals("https://example.supabase.co", client.supabaseHttpUrl, "Supabase http url should be https://example.supabase.co")
    }

    @Test
    fun testClientBuilderPlugins() {
        val client = createMockedSupabaseClient {
            supabaseUrl = "example.supabase.co"
            supabaseKey = "somekey"

            install(TestPlugin) {
                testValue = true
            }
        }
        val plugin = client.pluginManager.getPlugin<Any?>("test")
        //test if the plugin was correctly installed
        assertIs<TestPlugin>(plugin, "Plugin 'test' should be of type TestPlugin")
        //test if the plugin correctly modified the 'useHTTPS' parameter
        assertEquals("https://example.supabase.co", client.supabaseHttpUrl, "Supabase http url should be https://example.supabase.co because the plugin modifies it")
    }

    private fun createMockedSupabaseClient(init: SupabaseClientBuilder.() -> Unit): SupabaseClient {
        return createSupabaseClient {
            httpEngine = mockEngine
            init()
        }
    }

}