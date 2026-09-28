package com.flycms;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * API 错误契约集成测试（阶段 K4 / G4）。
 *
 * <p>验证 K1 的端到端效果：未登录访问受保护端点时，
 * <b>不是</b>容器 HTML 错误页，而是结构化 JSON（含 {@code code/status/traceId}）且不外泄异常类名。
 *
 * <p><b>运行条件</b>：需要 Docker。无 Docker 的环境（如本机开发机）整类在测试发现阶段自动跳过
 * （{@link EnabledIf} 早于 Spring 上下文加载），因此不会因缺库而失败。
 * CI（GitHub Actions ubuntu-latest 自带 Docker）会真实执行。
 *
 * <p>数据源由 Testcontainers 提供的 MySQL 5.7 覆盖（{@code DruidConfig} 绑定
 * {@code spring.datasource.*}，用 {@link DynamicPropertySource} 改写即生效）；
 * 本用例只走"未认证"分支，不触达任何业务表。
 *
 * <p>Boot 4 已移除 {@code TestRestTemplate}，故改用 JDK {@link HttpClient} +
 * {@link LocalServerPort}，避免再引入测试专用 HTTP 客户端。
 */
@Testcontainers
@EnabledIf("dockerAvailable")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiErrorContractIntegrationTest {

    @Container
    @SuppressWarnings("resource")
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:5.7")
            .withDatabaseName("flycms")
            .withUsername("root")
            .withPassword("123456");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @LocalServerPort
    private int port;

    @Test
    @DisplayName("未登录访问受保护 API → 401 + 结构化 JSON 信封（含 traceId，不含堆栈）")
    void unauthenticatedApiReturnsStructured401() throws Exception {
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/auth/codes"))
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();
        HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(401, resp.statusCode(), "HTTP 状态码必须保持 401（前端据此触发重新登录）");
        assertTrue(resp.headers().firstValue("X-Trace-Id").isPresent(), "响应头应带 X-Trace-Id");

        String body = resp.body();
        assertNotNull(body, "错误响应体不能为空（改造前是容器 HTML 页）");
        assertTrue(body.contains("\"code\":40101"), "应含业务码 40101，实际：" + body);
        assertTrue(body.contains("\"status\":401"), "应含 status 401，实际：" + body);
        assertFalse(body.contains("Exception"), "不得外泄异常类名，实际：" + body);
        assertFalse(body.toLowerCase().contains("<html"), "不得是容器 HTML 错误页，实际：" + body);
    }

    /**
     * 探测 Docker 是否可用（在测试发现阶段执行，供 {@link EnabledIf} 使用）
     */
    static boolean dockerAvailable() {
        try {
            Process p = new ProcessBuilder("docker", "info").redirectErrorStream(true).start();
            p.getInputStream().readAllBytes();
            return p.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }
}
