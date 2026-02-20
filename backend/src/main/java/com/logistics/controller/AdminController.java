package com.logistics.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.logistics.common.Result;
import com.logistics.entity.Order;
import com.logistics.entity.User;
import com.logistics.entity.Warehouse;
import com.logistics.mapper.OrderMapper;
import com.logistics.mapper.UserMapper;
import com.logistics.mapper.WarehouseMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private WarehouseMapper warehouseMapper;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // ── User Management ──

    @GetMapping("/users")
    public Result<?> listUsers(@RequestParam(required = false) String role,
                               @RequestParam(required = false) String search) {
        LambdaQueryWrapper<User> qw = new LambdaQueryWrapper<>();
        if (role != null && !role.isEmpty() && !"all".equals(role)) {
            qw.eq(User::getRole, role);
        }
        if (search != null && !search.isEmpty()) {
            qw.like(User::getUsername, search);
        }
        qw.orderByAsc(User::getId);
        List<User> users = userMapper.selectList(qw);
        // Don't expose passwords
        users.forEach(u -> u.setPassword(null));
        return Result.success(users);
    }

    @PostMapping("/users")
    public Result<?> createUser(@RequestBody User user) {
        // Check duplicate username
        LambdaQueryWrapper<User> qw = new LambdaQueryWrapper<>();
        qw.eq(User::getUsername, user.getUsername());
        if (userMapper.selectCount(qw) > 0) {
            return Result.error("用户名已存在");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userMapper.insert(user);
        user.setPassword(null);
        return Result.success(user);
    }

    @PutMapping("/users/{id}")
    public Result<?> updateUser(@PathVariable Long id, @RequestBody User user) {
        User existing = userMapper.selectById(id);
        if (existing == null) return Result.error("用户不存在");

        existing.setUsername(user.getUsername());
        existing.setRole(user.getRole());
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            existing.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        userMapper.updateById(existing);
        existing.setPassword(null);
        return Result.success(existing);
    }

    @DeleteMapping("/users/{id}")
    public Result<?> deleteUser(@PathVariable Long id) {
        User existing = userMapper.selectById(id);
        if (existing == null) return Result.error("用户不存在");
        userMapper.deleteById(id);
        return Result.success("删除成功");
    }

    // ── Order Management (all orders) ──

    @GetMapping("/orders")
    public Result<?> listAllOrders(@RequestParam(required = false) Integer status,
                                   @RequestParam(required = false) String search) {
        List<Order> orders = orderMapper.selectAllOrdersForAdmin(status, search);
        return Result.success(orders);
    }

    @PutMapping("/orders/{orderId}/status")
    public Result<?> updateOrderStatus(@PathVariable Integer orderId,
                                       @RequestParam Integer status) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) return Result.error("订单不存在");
        order.setStatus(status);
        orderMapper.updateById(order);
        return Result.success("状态更新成功");
    }

    @DeleteMapping("/orders/{orderId}")
    public Result<?> deleteOrder(@PathVariable Integer orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) return Result.error("订单不存在");
        orderMapper.deleteById(orderId);
        return Result.success("删除成功");
    }

    // ── Data Analysis ──

    @GetMapping("/stats/overview")
    public Result<?> getOverview() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userMapper.selectCount(null));
        stats.put("totalOrders", orderMapper.selectCount(null));

        // Count by role
        for (String role : List.of("merchant", "driver", "consumer")) {
            LambdaQueryWrapper<User> qw = new LambdaQueryWrapper<>();
            qw.eq(User::getRole, role);
            stats.put(role + "Count", userMapper.selectCount(qw));
        }

        // Count by order status
        Map<String, Long> statusCounts = new HashMap<>();
        for (int s = 0; s <= 5; s++) {
            LambdaQueryWrapper<Order> qw = new LambdaQueryWrapper<>();
            qw.eq(Order::getStatus, s);
            statusCounts.put(String.valueOf(s), orderMapper.selectCount(qw));
        }
        stats.put("ordersByStatus", statusCounts);

        // Total revenue
        LambdaQueryWrapper<Order> revenueQw = new LambdaQueryWrapper<>();
        revenueQw.ge(Order::getStatus, 1); // shipped or beyond
        List<Order> paidOrders = orderMapper.selectList(revenueQw);
        BigDecimal totalRevenue = paidOrders.stream()
                .map(Order::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.put("totalRevenue", totalRevenue);

        // Warehouse count
        stats.put("totalWarehouses", warehouseMapper.selectCount(null));

        return Result.success(stats);
    }

    @GetMapping("/stats/daily-orders")
    public Result<?> getDailyOrders(@RequestParam(required = false, defaultValue = "7") int days) {
        LocalDateTime start = LocalDate.now().minusDays(days - 1).atStartOfDay();
        LambdaQueryWrapper<Order> qw = new LambdaQueryWrapper<>();
        qw.ge(Order::getOrderTime, start);
        qw.orderByAsc(Order::getOrderTime);
        List<Order> orders = orderMapper.selectList(qw);

        // Group by date
        Map<String, Long> dailyMap = new LinkedHashMap<>();
        for (int i = days - 1; i >= 0; i--) {
            dailyMap.put(LocalDate.now().minusDays(i).toString(), 0L);
        }
        orders.forEach(o -> {
            if (o.getOrderTime() != null) {
                String date = o.getOrderTime().toLocalDate().toString();
                dailyMap.merge(date, 1L, Long::sum);
            }
        });

        List<Map<String, Object>> result = new ArrayList<>();
        dailyMap.forEach((date, count) -> {
            Map<String, Object> item = new HashMap<>();
            item.put("date", date);
            item.put("count", count);
            result.add(item);
        });
        return Result.success(result);
    }
}
