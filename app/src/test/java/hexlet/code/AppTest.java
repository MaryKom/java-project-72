package hexlet.code;

import hexlet.code.App;
import hexlet.code.controllers.UrlController;
import hexlet.code.model.Url;
import hexlet.code.model.UrlCheck;
import hexlet.code.model.query.QUrl;
import hexlet.code.model.query.QUrlCheck;
import io.ebean.Transaction;
import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.assertThat;

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import io.javalin.Javalin;
import io.ebean.DB;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class AppTest {
    @Test
    void testInit() {
        assertThat(true).isEqualTo(true);
    }

    private static Javalin app;
    private static String baseUrl;
    private static Transaction transaction;
    private static MockWebServer server;

    @BeforeAll
    public static void beforeAll() throws IOException {
        app = App.getApp();
        app.start(0);
        int port = app.port();
        baseUrl = "http://localhost:" + port;

        server = new MockWebServer();
        String expectedBody = Files.readString(Path.of("src/test/resources/test.html"));
        server.enqueue(new MockResponse().setBody(expectedBody));
        server.start();
    }

    @AfterAll
    public static void afterAll() throws IOException {
        app.stop();
        server.shutdown();
    }

    // Тесты не зависят друг от друга
    // Но хорошей практикой будет возвращать базу данных между тестами в исходное состояние
    @BeforeEach
    final void beforeEach() {
        transaction = DB.beginTransaction();
    }

    @AfterEach
    final void afterEach() {
        transaction.rollback();
    }

    @Test
    void testRoot() {
        HttpResponse<String> response = Unirest
                .get(baseUrl)
                .asString();

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getBody()).contains("Анализатор страниц");
    }

    @Test
    void testRootURLS() {
        HttpResponse<String> response = Unirest
                .get(baseUrl + "/urls")
                .asString();

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getBody()).contains("Анализатор страниц");
    }

    @Test
    void testUrl() {
        HttpResponse<String> response = Unirest
                .get(baseUrl + "/urls/1")
                .asString();

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getBody()).contains("https://www.github.com");
    }

    @Test
    void testUrls() {
        HttpResponse<String> response = Unirest
                .get(baseUrl + "/urls")
                .asString();

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getBody()).contains("https://www.github.com");
        assertThat(response.getBody()).contains("https://www.railway.app");
    }

    @Test
    void testIncorrectAddUrl() {
        final String incorrectUrl = "trompompom.com";

        HttpResponse<String> response = Unirest
                .post(baseUrl + "/urls")
                .field("url", incorrectUrl)
                .asString();

        assertThat(response.getHeaders().getFirst("Location")).isEqualTo("/");

        HttpResponse<String> responseIncorrect = Unirest
                .get(baseUrl)
                .asString();

        assertThat(responseIncorrect.getBody()).contains("Некорректный URL");
    }

    @Test
    void testAddUrl() {
        String urlName = "https://www.example.com";
        HttpResponse<String> responseP = Unirest
                .post(baseUrl + "/urls")
                .field("url", urlName)
                .asEmpty();

        assertThat(responseP.getStatus()).isEqualTo(302);
        assertThat(responseP.getHeaders().getFirst("Location")).isEqualTo("/urls");

        Url postedUrl = new QUrl()
                .name.equalTo(urlName)
                .findOne();

        assertThat(postedUrl).isNotNull();
        assertThat(postedUrl.getName()).isEqualTo(urlName);

        HttpResponse<String> response = Unirest
                .get(baseUrl + "/urls")
                .asString();
        String content = response.getBody();

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(content).contains(urlName);
        assertThat(content).contains("Страница успешно добавлена");
    }

    @Test
    void testAddExistingUrl() {
        String urlName = "https://www.github.com";

        HttpResponse responsePost = Unirest
                .post(baseUrl + "/urls")
                .field("url", urlName)
                .asEmpty();

        assertThat(responsePost.getStatus()).isEqualTo(302);
        assertThat(responsePost.getHeaders().getFirst("Location")).isEqualTo("/urls");

        HttpResponse<String> response = Unirest
                .get(baseUrl + "/urls")
                .asString();

        String content = response.getBody();

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(content).contains(urlName);
        assertThat(content).contains("Страница уже существует");
    }

    @Test
    void testShowUrl() {
        HttpResponse<String> response = Unirest
                .get(baseUrl + "/urls/1")
                .asString();

        String content = response.getBody();

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(content).contains("https://www.github.com");
        assertThat(content).contains("description");
        assertThat(content).contains("Запустить проверку");
    }

    @Test
    void testCheckUrl() throws IOException {
        String serverUrl = server.url("/").toString();
        String correctServerUrl = serverUrl.substring(0, serverUrl.length() - 1);


        HttpResponse response = Unirest
                .post(baseUrl + "/urls")
                .field("url", serverUrl)
                .asEmpty();

        Url url = new QUrl()
                .name.equalTo(correctServerUrl)
                .findOne();

        assert url != null;
        long urlId = url.getId();

        assertThat(url).isNotNull();

        HttpResponse responseToCheck = Unirest
                .post(baseUrl + "/urls/" + urlId + "/checks")
                .asEmpty();

        HttpResponse<String> responseResult = Unirest
                .get(baseUrl + "/urls/" + urlId)
                .asString();

        String responseBody = responseResult.getBody();
        assertThat(responseBody).contains("Страница успешно проверена");

        List<UrlCheck> actualCheck = new QUrlCheck()
                .findList();

        assertThat(actualCheck).isNotEmpty();

        String content = responseResult.getBody();

        assertThat(content).contains("Хекслет");
        assertThat(content).contains("Живое онлайн сообщество");
        assertThat(content).contains("Это заголовок h1");

    }

    @Test
    void testRedirectUrl() {
        final String testRedirect = "https://www.youtube.com";

        HttpResponse response = Unirest
                .post(baseUrl + "/urls")
                .field("url", testRedirect)
                .asEmpty();

        assertThat(response.getStatus()).isEqualTo(302);
        assertThat(response.getHeaders().getFirst("Location")).isEqualTo("/urls");
    }

    @Test
    void testParseUrl() {
        String expected1 = "https://www.example.com";
        String expected2 = "https://www.example.com:8080";
        String actual1 = UrlController.parseUrl("https://www.example.com/one/two");
        String actual2 = UrlController.parseUrl("https://www.example.com:8080/one/two");
        String actual3 = UrlController.parseUrl("www.example.com");
        assertThat(actual1).isEqualTo(expected1);
        assertThat(actual2).isEqualTo(expected2);
        assertThat(actual3).isEqualTo(null);
    }
}
