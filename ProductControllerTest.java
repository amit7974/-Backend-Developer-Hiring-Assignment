package com.example.productapi.controller;

import com.example.productapi.dto.request.ProductRequest;
import com.example.productapi.dto.response.ProductResponse;
import com.example.productapi.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
@Import(com.example.productapi.config.SecurityConfig.class)
@DisplayName("ProductController Slice Tests")
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    @MockBean
    private com.example.productapi.security.JwtUtils jwtUtils;

    @MockBean
    private com.example.productapi.security.JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    private ProductResponse productResponse;

    @BeforeEach
    void setUp() {
        productResponse = ProductResponse.builder()
                .id(1L)
                .productName("Sample Product")
                .createdBy("admin")
                .createdOn(LocalDateTime.now())
                .build();
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /api/v1/products - should return 200 with page")
    void getAllProducts_shouldReturn200() throws Exception {
        Page<ProductResponse> page = new PageImpl<>(List.of(productResponse));
        when(productService.getAllProducts(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/products")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].productName").value("Sample Product"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /api/v1/products/{id} - should return 200")
    void getProductById_shouldReturn200() throws Exception {
        when(productService.getProductById(1L)).thenReturn(productResponse);

        mockMvc.perform(get("/api/v1/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.productName").value("Sample Product"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/v1/products - should return 201")
    void createProduct_shouldReturn201() throws Exception {
        ProductRequest request = new ProductRequest("Sample Product");
        when(productService.createProduct(any(ProductRequest.class), anyString())).thenReturn(productResponse);

        mockMvc.perform(post("/api/v1/products")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productName").value("Sample Product"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/v1/products - should return 400 for blank product name")
    void createProduct_shouldReturn400_whenInvalidRequest() throws Exception {
        ProductRequest invalid = new ProductRequest("");

        mockMvc.perform(post("/api/v1/products")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("PUT /api/v1/products/{id} - should return 200")
    void updateProduct_shouldReturn200() throws Exception {
        ProductRequest request = new ProductRequest("Updated Product");
        when(productService.updateProduct(anyLong(), any(ProductRequest.class), anyString()))
                .thenReturn(productResponse);

        mockMvc.perform(put("/api/v1/products/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("DELETE /api/v1/products/{id} - should return 204")
    void deleteProduct_shouldReturn204() throws Exception {
        doNothing().when(productService).deleteProduct(1L);

        mockMvc.perform(delete("/api/v1/products/1").with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("DELETE /api/v1/products/{id} - should return 403 for USER role")
    void deleteProduct_shouldReturn403_forUser() throws Exception {
        mockMvc.perform(delete("/api/v1/products/1").with(csrf()))
                .andExpect(status().isForbidden());
    }
}
