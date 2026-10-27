package com.gourmet;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 核心链路集成测试：使用 H2 内存库，不依赖 MySQL / Redis。
 */
@SpringBootTest(properties = {
        // 测试使用不可达的 Redis 端口，走「数据库兜底」链路，避免真实 Redis 的存量数据干扰断言
        "spring.data.redis.host=127.0.0.1",
        "spring.data.redis.port=6390",
        "app.seckill.async-enabled=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class GourmetApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void contextLoads() {
        assertTrue(true);
    }

    @Test
    void userLoginAndPersonalizedRecommend() throws Exception {
        String token = login("13800000001", "123456");

        mockMvc.perform(get("/api/recommend/home").param("limit", "5")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].recommendAlgorithm").value("PREFERENCE"))
                .andExpect(jsonPath("$.data[0].recommendReason").isNotEmpty());
    }

    @Test
    void protectedApiWithoutTokenShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/user/center"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void userCannotAccessAdminApi() throws Exception {
        String token = login("13800000001", "123456");
        mockMvc.perform(get("/api/statistic/admin/overview")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void couponSeckillOnlyOnce() throws Exception {
        String token = login("13800000003", "123456");

        mockMvc.perform(post("/api/coupon/1/seckill")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(post("/api/coupon/1/seckill")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(409));
    }

    private String login(String phone, String password) throws Exception {
        String body = "{\"phone\":\"" + phone + "\",\"password\":\"" + password + "\"}";
        MvcResult result = mockMvc.perform(post("/api/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body.getBytes(StandardCharsets.UTF_8)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        return json.path("data").path("token").asText();
    }
}
