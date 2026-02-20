package com.logistics.controller;

import com.logistics.common.Result;
import com.logistics.entity.Address;
import com.logistics.exception.BusinessException;
import com.logistics.service.AddressService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AddressControllerTest {

    @InjectMocks
    private AddressController addressController;

    @Mock
    private AddressService addressService;

    @Mock
    private HttpServletRequest request;

    @Test
    void listAddresses_noUserId_shouldThrow() {
        when(request.getAttribute("userId")).thenReturn(null);

        assertThrows(BusinessException.class, () -> addressController.listAddresses(request));
    }

    @Test
    void listAddresses_success() {
        when(request.getAttribute("userId")).thenReturn(1L);
        when(addressService.listByUserId(1L)).thenReturn(List.of(new Address()));

        Result<List<Address>> result = addressController.listAddresses(request);
        assertTrue(result.isSuccess());
        assertEquals(1, result.getData().size());
    }

    @Test
    void createAddress_shouldSetUserId() {
        when(request.getAttribute("userId")).thenReturn(1L);
        Address address = new Address();
        address.setReceiverName("张三");
        when(addressService.add(any())).thenReturn(Result.success(address));

        Result<Address> result = addressController.createAddress(address, request);
        assertTrue(result.isSuccess());
        assertEquals(1L, address.getUserId());
    }

    @Test
    void deleteAddress_noAuth_shouldThrow() {
        when(request.getAttribute("userId")).thenReturn(null);

        assertThrows(BusinessException.class, () -> addressController.deleteAddress(1L, request));
    }
}
