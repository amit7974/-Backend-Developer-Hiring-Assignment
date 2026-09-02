package com.example.productapi.integration;

import com.example.productapi.dto.request.LoginRequest;
import com.example.productapi.dto.request.ProductRequest;
import com.example.productapi.dto.response.AuthResponse;
import com.example.productapi.dto.response.ProductResponse;
import com.example.productapi.entity.Role;
import com.example.productapi.entity.User;
import com.example.productapi.repository.ProductRepository;
import com.example.productapi.repository.RoleRepository;
import com.example.productapi.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Product API Integration Tests")
class ProductIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static String adminToken;
    private static Long createdProductId;

    @BeforeEach
    void setUp() {
        if (!roleRepository.findByName("ROLE_ADMIN").isPresent()) {
            Role adminRole = roleRepository.save(Role.builder().name("ROLE_ADMIN").build());
            Role userRole = roleRepository.save(Role.builder().name("ROLE_USER").build());

            User admin = User.builder()
                    .username("testadmin")
                    .email("testadmin@example.com")
                    .password(passwordEncoder.encode("Admin@123"))
                    .enabled(true)
                    .roles(Set.of(adminRole, userRole))
                    .build();
            userRepository.save(admin);
        }
    }

    private String baseUrl(String path) {
        return "http://localhost:" + port + path;
    }

    @Test
    @Order(1)
    @DisplayName("POST /api/v1/auth/login - admin should get JWT")
    void login_shouldReturnJwt() {
        LoginRequest login = new LoginRequest("testadmin", "Admin@123");
        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                baseUrl("/api/v1/auth/login"), login, AuthResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getAccessToken()).isNotBlank();
        adminToken = response.getBody().getAccessToken();
    }

    @Test
    @Order(2)
    @DisplayName("POST /api/v1/products - admin should create product")
    void createProduct_shouldReturn201() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        ProductRequest request = new ProductRequest("Integration Test Product");
        HttpEntity<ProductRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<ProductResponse> response = restTemplate.exchange(
                baseUrl("/api/v1/products"), HttpMethod.POST, entity, ProductResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getProductName()).isEqualTo("Integration Test Product");
        createdProductId = response.getBody().getId();
    }

    @Test
    @Order(3)
    @DisplayName("GET /api/v1/products - should return list with created product")
    void getAllProducts_shouldReturnProducts() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl("/api/v1/products"), HttpMethod.GET, entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Integration Test Product");
    }

    @Test
    @Order(4)
    @DisplayName("GET /api/v1/products/{id} - should return single product")
    void getProductById_shouldReturnProduct() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<ProductResponse> response = restTemplate.exchange(
                baseUrl("/api/v1/products/" + createdProductId), HttpMethod.GET, entity, ProductResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getProductName()).isEqualTo("Integration Test Product");
    }

    @Test
    @Order(5)
    @DisplayName("PUT /api/v1/products/{id} - should update product")
    void updateProduct_shouldReturn200() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        ProductRequest request = new ProductRequest("Updated Product");
        HttpEntity<ProductRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<ProductResponse> response = restTemplate.exchange(
                baseUrl("/api/v1/products/" + createdProductId), HttpMethod.PUT, entity, ProductResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getProductName()).isEqualTo("Updated Product");
    }

    @Test
    @Order(6)
    @DisplayName("DELETE /api/v1/products/{id} - should delete product")
    void deleteProduct_shouldReturn204() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<Void> response = restTemplate.exchange(
                baseUrl("/api/v1/products/" + createdProductId), HttpMethod.DELETE, entity, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    @Order(7)
    @DisplayName("GET /api/v1/products - should return 401 without token")
    void getAllProducts_shouldReturn401_withoutToken() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                baseUrl("/api/v1/products"), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
