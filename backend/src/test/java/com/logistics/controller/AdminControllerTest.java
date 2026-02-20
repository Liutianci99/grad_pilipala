package com.logistics.controller;

import com.logistics.common.Result;
import com.logistics.entity.User;
import com.logistics.exception.BusinessException;
import com.logistics.mapper.OrderMapper;
import com.logistics.mapper.UserMapper;
import com.logistics.mapper.WarehouseMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @InjectMocks
    private AdminController adminController;

    @Mock
    private UserMapper userMapper;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private WarehouseMapper warehouseMapper;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setRole("merchant");
        testUser.setPassword("encoded_password");
    }

    @Test
    void listUsers_shouldReturnUsersWithoutPasswords() {
        when(userMapper.selectList(any())).thenReturn(List.of(testUser));

        Result<?> result = adminController.listUsers(null, null);

        assertTrue(result.isSuccess());
        @SuppressWarnings("unchecked")
        List<User> users = (List<User>) result.getData();
        assertNotNull(users);
        assertEquals(1, users.size());
        assertNull(users.get(0).getPassword());
    }

    @Test
    void createUser_duplicateUsername_shouldThrow() {
        when(userMapper.selectCount(any())).thenReturn(1L);

        User newUser = new User();
        newUser.setUsername("testuser");
        newUser.setPassword("123");
        newUser.setRole("consumer");

        Result<?> result = adminController.createUser(newUser);
        assertFalse(result.isSuccess());
    }

    @Test
    void updateUser_notFound_shouldThrow() {
        when(userMapper.selectById(999L)).thenReturn(null);

        User update = new User();
        update.setUsername("new");
        update.setRole("admin");

        assertThrows(BusinessException.class, () -> adminController.updateUser(999L, update));
    }

    @Test
    void deleteUser_notFound_shouldThrow() {
        when(userMapper.selectById(999L)).thenReturn(null);

        assertThrows(BusinessException.class, () -> adminController.deleteUser(999L));
    }

    @Test
    void deleteUser_success() {
        when(userMapper.selectById(1L)).thenReturn(testUser);
        when(userMapper.deleteById(1L)).thenReturn(1);

        Result<?> result = adminController.deleteUser(1L);
        assertTrue(result.isSuccess());
        verify(userMapper).deleteById(1L);
    }

    @Test
    void getOverview_shouldReturnStats() {
        when(userMapper.selectCount(any())).thenReturn(10L);
        when(orderMapper.selectCount(any())).thenReturn(20L);
        when(orderMapper.selectList(any())).thenReturn(List.of());
        when(warehouseMapper.selectCount(any())).thenReturn(5L);

        Result<?> result = adminController.getOverview();
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
    }
}
