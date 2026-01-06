package org.tbox.base.core.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tbox.base")
public class TboxBaseProperties {

    /**
     * 是否开启默认跨域配置
     */
    private boolean defaultCorsConfigEnable = true;

    /**
     * 是否开启默认jackson配置
     */
    private boolean defaultJacksonConfigEnable = true;


    /**
     * 默认等待停机时间 秒
     */
    private Integer waitStopTime = 30;


    public boolean isDefaultCorsConfigEnable() {
        return defaultCorsConfigEnable;
    }

    public void setDefaultCorsConfigEnable(boolean defaultCorsConfigEnable) {
        this.defaultCorsConfigEnable = defaultCorsConfigEnable;
    }

    public boolean isDefaultJacksonConfigEnable() {
        return defaultJacksonConfigEnable;
    }

    public void setDefaultJacksonConfigEnable(boolean defaultJacksonConfigEnable) {
        this.defaultJacksonConfigEnable = defaultJacksonConfigEnable;
    }


    public Integer getWaitStopTime() {
        return waitStopTime;
    }

    public void setWaitStopTime(Integer waitStopTime) {
        this.waitStopTime = waitStopTime;
    }


}
