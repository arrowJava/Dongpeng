package com.pengxun.manager.base.config.mybatis.sharding;


import java.util.Arrays;
import java.util.List;

import io.shardingjdbc.core.api.config.strategy.ShardingStrategyConfiguration;
import io.shardingjdbc.core.routing.strategy.ShardingStrategy;

public class BoxCodeShardingStrategyConfiguration implements ShardingStrategyConfiguration {

	private List<String> shardingColumns;

	private Integer tableCount = 1000;

	public BoxCodeShardingStrategyConfiguration(String ...shardingColumns) {
	    this.shardingColumns = Arrays.asList(shardingColumns);
	}

	@Override
	public ShardingStrategy build() {
		return new BoxCodeShardingStrategy(shardingColumns,tableCount);
	}

}
