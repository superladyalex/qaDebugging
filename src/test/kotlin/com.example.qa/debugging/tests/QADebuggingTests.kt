package com.qa.debugging.tests

import com.example.qa.debugging.clients.Redis
import com.google.gson.GsonBuilder
import io.github.cdimascio.dotenv.dotenv
import models.ErrorResponse
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.util.*


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class QADebuggingTests {

    //Should be done better.
    val dotenv = dotenv {
        directory = "./.env"
        ignoreIfMalformed = true
        ignoreIfMissing = true
    }

    val redis = Redis(dotenv.get("REDIS_HOST"), dotenv.get("REDIS_PORT"), dotenv.get("REDIS_PASSWORD"))
    val client = OkHttpClient()
    val emptyRequestBody = RequestBody.create(null, ByteArray(0))
    lateinit var uuid: UUID
    val gson = GsonBuilder().create()

    @BeforeEach
    fun setUp() {
        uuid = UUID.randomUUID()
    }


    @DisplayName("Post: Key/Value pair - 200 Response")
    @Test
    fun postKeyValuePair() {

        val request = Request.Builder()
            .url("""${dotenv.get("QA_DEBUGGING_URL")}keys?name=${uuid}-key&value=${uuid}-value""")
            .header("token", dotenv.get("API_TOKEN"))
            .method("POST", emptyRequestBody)
            .build()

        val response = client.newCall(request).execute()

        assertEquals(200, response.code, "Status code was unexpected")

        val redisResult = redis.getKey("${uuid}-key")

        assertEquals(redisResult, "${uuid}-value")
    }

    @DisplayName("Post: Attempt to post a key that already exists - 400 Response")
    @Tag("bug")
    @Test
    fun postKeyAlreadyExists() {

        //Setting the key through redis itself to keep functionality scoped for the purpose of this test
        redis.setKey("${uuid}-key", "${uuid}-value")

        val request = Request.Builder()
            .url("""${dotenv.get("QA_DEBUGGING_URL")}keys?name=${uuid}-key&value=${uuid}-value""")
            .header("token", dotenv.get("API_TOKEN"))
            .method("POST", emptyRequestBody)
            .build()

        val response = client.newCall(request).execute()

        assertEquals(400, response.code, "Status code was unexpected")
    }

    @DisplayName("Post: POST HTML encoded characters - 200- Response")
    @Tag("bug")
    @Test
    fun postHTMLEncodedChars() {

        //Deleting the key if it exists
        redis.deleteKey("%25")

        val request = Request.Builder()
            .url("""${dotenv.get("QA_DEBUGGING_URL")}keys?name=%25&value=${uuid}-value""")
            .header("token", dotenv.get("API_TOKEN"))
            .method("POST", emptyRequestBody)
            .build()

        val response = client.newCall(request).execute()

        assertEquals(200, response.code, "Status code was unexpected")

        //Getting the key in REDIS
        val result = redis.getKey("%25")

        assertEquals("${uuid}-value", result)
    }

    @DisplayName("PUT: Update Value for an existing Key - 200 Response")
    @Test
    fun updateValueForExistingKey() {

        //Setting the key through redis itself to keep functionality scoped for the purpose of this test
        redis.setKey("${uuid}-key", "${uuid}-value")

        val request = Request.Builder()
            .url("""${dotenv.get("QA_DEBUGGING_URL")}keys?name=${uuid}-key&value=${uuid}-newValue""")
            .header("token", dotenv.get("API_TOKEN"))
            .method("PUT", emptyRequestBody)
            .build()

        val response = client.newCall(request).execute()

        assertEquals(200, response.code, "Status code was unexpected")

        val redisResult = redis.getKey("${uuid}-key")

        assertEquals(redisResult, "${uuid}-newValue")
    }

    @DisplayName("PUT: Attempt to update a value for a key that does not exist - 400 Response")
    @Test
    fun updateValueForKeyThatDoesNotExist() {

        val request = Request.Builder()
            .url("""${dotenv.get("QA_DEBUGGING_URL")}keys?name=${uuid}-key&value=${uuid}-newValue""")
            .header("token", dotenv.get("API_TOKEN"))
            .method("PUT", emptyRequestBody)
            .build()

        val response = client.newCall(request).execute()

        assertEquals(400, response.code, "Status code was unexpected")
        val parsedBody = gson.fromJson(response.body?.string() ?: "empty", ErrorResponse::class.java)

        assertEquals(parsedBody.error, "Key does not exist")

        val redisResult = redis.getKey("${uuid}-key")

        assertNull(redisResult)
    }

    @DisplayName("DELETE: Delete an existing key -- 200 Response")
    @Tag("bug")
    @Test
    fun deleteKey() {
        //Setting the key through redis itself to keep functionality scoped for the purpose of this test
        redis.setKey("${uuid}-key", "${uuid}-value")

        val request = Request.Builder()
            .url("""${dotenv.get("QA_DEBUGGING_URL")}keys?name=${uuid}-key""")
            .header("token", dotenv.get("API_TOKEN"))
            .method("DELETE", emptyRequestBody)
            .build()

        val response = client.newCall(request).execute()

        assertEquals(200, response.code, "Status code was unexpected")

        val redisResult = redis.getKey("${uuid}-key")

        assertNull(redisResult)
    }

    @DisplayName("DELETE: Attempt to delete a key that does not exist -- 400 Response")
    @Test
    fun deleteKeyThatDoesNotExist() {

        val request = Request.Builder()
            .url("""${dotenv.get("QA_DEBUGGING_URL")}keys?name=${uuid}-key""")
            .header("token", dotenv.get("API_TOKEN"))
            .method("DELETE", emptyRequestBody)
            .build()

        val response = client.newCall(request).execute()

        assertEquals(400, response.code, "Status code was unexpected")

        val parsedBody = gson.fromJson(response.body?.string() ?: "empty", ErrorResponse::class.java)

        assertEquals(parsedBody.error, "Key does not exist")
    }

    @DisplayName("Post: Missing token on POST -- 401 response")
    @Tag("Auth")
    @Test
    fun tokenMissingOnPOST() {

        val request = Request.Builder()
            .url("""${dotenv.get("QA_DEBUGGING_URL")}keys?name=${uuid}-key&value=${uuid}-value""")
            .method("POST", emptyRequestBody)
            .build()

        val response = client.newCall(request).execute()

        assertEquals(401, response.code, "Status code was unexpected")

        val parsedBody = gson.fromJson(response.body?.string() ?: "empty", ErrorResponse::class.java)

        assertEquals(parsedBody.error, "Unauthorized")
    }

    @DisplayName("Post: Missing parameters on request")
    @Tag("Bug")
    @Test
    fun missingParameters() {

        val request = Request.Builder()
            .url("""${dotenv.get("QA_DEBUGGING_URL")}keys""")
            .header("token", dotenv.get("API_TOKEN"))
            .method("POST", emptyRequestBody)
            .build()

        val response = client.newCall(request).execute()

        assertTrue(response.code in 401..499) { "Response code was: ${response.code}" }
    }

    @DisplayName("GET: Unsupported Methood")
    @Tag("Bug")
    @Test
    fun unsupportedMethod() {

        val request = Request.Builder()
            .url("""${dotenv.get("QA_DEBUGGING_URL")}keys""")
            .header("token", dotenv.get("API_TOKEN"))
            .get()
            .build()

        val response = client.newCall(request).execute()

        assertEquals(405, response.code, "Status code was unexpected")

        //TODO: when fixed needs error message json check
    }

    @DisplayName("PERFORMANCE: Time it takes an api POST request to be made over a set command")
    @Tag("Performance")
    @Test
    fun timing() {

        val request = Request.Builder()
            .url("""${dotenv.get("QA_DEBUGGING_URL")}keys?name=${uuid}-key&value=${uuid}-value""")
            .header("token", dotenv.get("API_TOKEN"))
            .method("POST", emptyRequestBody)
            .build()

        val beforeApiRequest = System.currentTimeMillis()
        val response = client.newCall(request).execute()
        val afterApiRequest = System.currentTimeMillis()

        // Just a sanity to ensure that an actual 200 was returned, so we're only comparing apples to apples
        assertEquals(200, response.code, "Status code was unexpected")

        val beforeSet = System.currentTimeMillis()
        redis.setKey("${uuid}-key-1", "${uuid}-value")
        val afterSet = System.currentTimeMillis()

        val apiTime = (afterApiRequest - beforeApiRequest)
        val setTime = (afterSet - beforeSet)

        val totalDifference = apiTime - setTime
        println(totalDifference)

        assert(totalDifference < 3000)
    }


    @AfterAll
    fun teardown() {
        redis.shutdownRedis()
    }
}
