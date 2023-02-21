import hexlet.code.App;
import hexlet.code.model.Url;
import hexlet.code.model.query.QUrl;
import io.ebean.Transaction;
import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.assertThat;

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import io.javalin.Javalin;
import io.ebean.DB;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

public class AppTest {
    @Test
    void testInit() {
        assertThat(true).isEqualTo(true);
    }

    private static Javalin app;
    private static String baseUrl;
    private static Transaction transaction;

    @BeforeAll
    public static void beforeAll() {
        app = App.getApp();
        app.start(0);
        int port = app.port();
        baseUrl = "http://localhost:" + port;
    }

    @AfterAll
    public static void afterAll() {
        app.stop();
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
}
