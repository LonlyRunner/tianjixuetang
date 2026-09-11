package com.tianji.learning.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
@RefreshScope
@ConfigurationProperties(prefix = "like")
public class LikeProperties {
    private List<String> bizTypes = new ArrayList<>();
    /**
     * 回答/评论对应的点赞业务类型
     */
    private String replyBizType = "QA";
    /** 学习笔记对应的点赞业务类型 */
    private String noteBizType = "NOTE";
}
