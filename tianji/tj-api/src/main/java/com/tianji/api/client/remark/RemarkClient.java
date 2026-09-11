package com.tianji.api.client.remark;

import com.tianji.api.client.remark.fallback.RemarkClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Set;

@FeignClient(value = "remark-service", fallbackFactory = RemarkClientFallback.class)
public interface RemarkClient {
    /**
     * 查询点赞状态
     * @param bizIds 业务ID列表
     * @param bizType 业务类型
     * @return 已点赞的ID集合
     */
    @GetMapping("/likes/status")
    Set<Long> getLikedStatus(
            @RequestParam("bizIds") List<Long> bizIds,
            @RequestParam("bizType") String bizType
    );
}