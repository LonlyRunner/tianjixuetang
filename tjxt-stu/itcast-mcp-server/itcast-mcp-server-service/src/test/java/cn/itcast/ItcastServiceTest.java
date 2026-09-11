package cn.itcast;

import cn.itcast.service.ItcastService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ItcastServiceTest {

    @Test
    void queryClassPlanByCity() {
        ItcastService itcastService = new ItcastService();
        String result = itcastService.queryClassPlanByCity("上海");
        System.out.println(result);
    }

    @Test
    void queryClassPlanBySubject() {
        ItcastService itcastService = new ItcastService();
        String result = itcastService.queryClassPlanBySubject("java");
        System.out.println(result);
    }
}
