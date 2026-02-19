package com.logistics.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * 腾讯地图服务
 * 负责调用腾讯地图API
 */
@Slf4j
@Service
public class TencentMapService {

    @Value("${tencent.map.api-key:}")
    private String apiKey;

    private static final String ROUTE_API = "https://apis.map.qq.com/ws/direction/v1/driving/";
    private static final String GEOCODER_API = "https://apis.map.qq.com/ws/geocoder/v1/";

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 规划驾车路线
     *
     * @param from      起点坐标 "纬度,经度"
     * @param to        终点坐标 "纬度,经度"
     * @param waypoints 途经点坐标列表（可选）
     * @return 路线规划结果JSON
     */
    public JSONObject planRoute(String from, String to, String waypoints) {
        try {
            StringBuilder url = new StringBuilder(ROUTE_API);
            url.append("?from=").append(from);
            url.append("&to=").append(to);
            url.append("&key=").append(apiKey);
            url.append("&output=json");

            if (waypoints != null && !waypoints.isEmpty()) {
                url.append("&waypoints=").append(waypoints);
            }

            log.info("调用腾讯地图路线规划API: {}", url);

            String response = restTemplate.getForObject(url.toString(), String.class);
            JSONObject result = JSON.parseObject(response);

            if (result.getInteger("status") != 0) {
                log.error("路线规划失败: {}", result.getString("message"));
                return null;
            }

            log.info("路线规划成功");
            return result;

        } catch (Exception e) {
            log.error("调用腾讯地图API失败", e);
            return null;
        }
    }

    /**
     * 逆地理编码 - 将坐标转换为地址
     */
    public String getAddress(Double latitude, Double longitude) {
        try {
            String url = GEOCODER_API + "?location=" + latitude + "," + longitude +
                    "&key=" + apiKey + "&output=json";

            log.debug("调用逆地理编码API: lat={}, lng={}", latitude, longitude);

            String response = restTemplate.getForObject(url, String.class);
            JSONObject result = JSON.parseObject(response);

            if (result.getInteger("status") == 0) {
                JSONObject resultObj = result.getJSONObject("result");
                if (resultObj != null) {
                    return resultObj.getString("address");
                }
            }

            log.warn("逆地理编码失败: {}", result.getString("message"));
            return "位置获取中...";

        } catch (Exception e) {
            log.error("逆地理编码失败", e);
            return "位置获取失败";
        }
    }

    /**
     * 解压polyline坐标数组
     */
    public JSONArray decompressPolyline(String polylineJson) {
        try {
            JSONArray coors = JSON.parseArray(polylineJson);

            for (int i = 2; i < coors.size(); i++) {
                double value = coors.getDouble(i - 2) + coors.getDouble(i) / 1000000.0;
                coors.set(i, value);
            }

            JSONArray points = new JSONArray();
            for (int i = 0; i < coors.size(); i += 2) {
                if (i + 1 < coors.size()) {
                    JSONArray point = new JSONArray();
                    point.add(coors.getDouble(i));
                    point.add(coors.getDouble(i + 1));
                    points.add(point);
                }
            }

            return points;

        } catch (Exception e) {
            log.error("解压polyline失败", e);
            return new JSONArray();
        }
    }
}
