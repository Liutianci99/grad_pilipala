package com.logistics.controller;

import com.logistics.common.Result;
import com.logistics.exception.BusinessException;
import com.logistics.mapper.DeliveryBatchMapper;
import com.logistics.mapper.DeliveryBatchOrderMapper;
import com.logistics.mapper.DeliveryLocationMapper;
import com.logistics.entity.DeliveryBatch;
import com.logistics.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliveryBatchControllerTest {

    @InjectMocks
    private DeliveryBatchController controller;

    @Mock private DeliveryRouteService deliveryRouteService;
    @Mock private DeliverySimulationService deliverySimulationService;
    @Mock private TencentMapService tencentMapService;
    @Mock private OrderService orderService;
    @Mock private WarehouseService warehouseService;
    @Mock private DeliveryLocationMapper deliveryLocationMapper;
    @Mock private DeliveryBatchMapper deliveryBatchMapper;
    @Mock private DeliveryBatchOrderMapper deliveryBatchOrderMapper;

    @Test
    void startBatch_notFound_shouldThrow() {
        when(deliveryBatchMapper.selectById(999)).thenReturn(null);

        assertThrows(BusinessException.class, () -> controller.startBatch(999));
    }

    @Test
    void startBatch_wrongStatus_shouldThrow() {
        DeliveryBatch batch = new DeliveryBatch();
        batch.setStatus(1); // already delivering
        when(deliveryBatchMapper.selectById(1)).thenReturn(batch);

        assertThrows(BusinessException.class, () -> controller.startBatch(1));
    }

    @Test
    void completeBatch_notFound_shouldThrow() {
        when(deliveryBatchMapper.selectById(999)).thenReturn(null);

        assertThrows(BusinessException.class, () -> controller.completeBatch(999));
    }

    @Test
    void completeBatch_wrongStatus_shouldThrow() {
        DeliveryBatch batch = new DeliveryBatch();
        batch.setStatus(0); // not delivering yet
        when(deliveryBatchMapper.selectById(1)).thenReturn(batch);

        assertThrows(BusinessException.class, () -> controller.completeBatch(1));
    }

    @Test
    void getRouteByBatch_noRoute_shouldThrow() {
        when(deliveryRouteService.getByBatchId(999)).thenReturn(null);

        assertThrows(BusinessException.class, () -> controller.getRouteByBatch(999));
    }

    @Test
    void getLocationByBatch_noRoute_shouldThrow() {
        when(deliveryRouteService.getByBatchId(999)).thenReturn(null);

        assertThrows(BusinessException.class, () -> controller.getLocationByBatch(999));
    }

    @Test
    void stopDelivery_shouldCallService() {
        Result<Void> result = controller.stopDelivery(1L);
        assertTrue(result.isSuccess());
        verify(deliverySimulationService).stopSimulation(1L);
    }
}
