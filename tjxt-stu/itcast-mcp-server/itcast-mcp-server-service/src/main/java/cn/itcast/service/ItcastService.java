package cn.itcast.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class ItcastService {

    @Tool(description = "根据城市查询开班计划")
    public String queryClassPlanByCity(@ToolParam(description = "城市名称，如：上海，广州") String cityName) {
        var url = "https://www.itheima.com/include/classplan/mclassPlan/m_class_plan.json";
        var data = HttpUtil.get(url, 10000);
        var jsonArray = JSONUtil.parseArray(data);

        var result = new ArrayList<>();
        for (Object obj : jsonArray) {
            var jsonObject = (JSONObject) obj;
            var name = jsonObject.getStr("name");
            if (StrUtil.contains(name, cityName)) {
                result.add(jsonObject);
            }
        }
        return JSONUtil.toJsonStr(result);
    }

    @Tool(description = "根据学科名称查询开班计划")
    public String queryClassPlanBySubject(@ToolParam(description = "学科名称，如：javaee、python、Linux") String subjectName) {
        var url = "https://www.itheima.com/include/classplan/mclassPlan/m_subject_class_plan.json";
        var data = HttpUtil.get(url, 10000);
        var jsonArray = JSONUtil.parseArray(data);

        var result = new ArrayList<>();
        for (Object obj : jsonArray) {
            var jsonObject = (JSONObject) obj;
            var name = jsonObject.getStr("name");
            var code = jsonObject.getStr("code");
            if (StrUtil.containsAnyIgnoreCase(name, subjectName) || StrUtil.containsAnyIgnoreCase(code, subjectName)) {
                result.add(jsonObject);
            }
        }
        return JSONUtil.toJsonStr(result);
    }
}
