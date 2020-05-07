package com.pengxun.manager.base.config.mybatis;

import com.szeastroc.common.jdbc.multi.JdbcConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;


@Component
@ConfigurationProperties(prefix = "box.code.jdbc")
public class BoxCodeJdbcConfig extends JdbcConfig {
	public static final String NAME = "sourceBoxcode";

	public static final String NO_SHARDING_NAME="noShardingBoxCode";
}
