package com.pengxun.manager.base.config.mybatis.sharding;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.szeastroc.common.utils.RouterUtils;

import io.shardingjdbc.core.api.algorithm.sharding.ListShardingValue;
import io.shardingjdbc.core.api.algorithm.sharding.ShardingValue;
import io.shardingjdbc.core.routing.strategy.ShardingStrategy;


public class BoxCodeShardingStrategy implements ShardingStrategy{

	private List<String> shardingColumns;

	private Integer tableCount;

	public BoxCodeShardingStrategy(List<String> shardingColumns, Integer tableCount) {
	     this.shardingColumns = shardingColumns;
	     this.tableCount = tableCount;
	}

	@Override
	public Collection<String> getShardingColumns() {
		return shardingColumns;
	}

	//note:
	
	@Override
	@SuppressWarnings("rawtypes")
	public Collection<String> doSharding(Collection<String> availableTargetNames,
			Collection<ShardingValue> shardingValues) {
		List<String> tables = new ArrayList<String>();
		//shardingValues 转换为 listShardingValues
		List<ListShardingValue> listShardingValues =  new ArrayList<ListShardingValue>();
		Iterator<ShardingValue> it = shardingValues.iterator();
		while (it.hasNext()) {
			ListShardingValue shardingValue = (ListShardingValue)it.next();
			listShardingValues.add(shardingValue);
		}
		//获取原始表名
		StringBuilder table = new StringBuilder(availableTargetNames.iterator().next().toString());
		int num = -1;
		//根据时间定位表名,一天一张
		for (ListShardingValue listShardingValue : listShardingValues) {
			if(listShardingValue.getColumnName().equals("code")){
				String openid = listShardingValue.getValues().iterator().next().toString();
				int routeFieldInt = RouterUtils.getResourceCode(openid);
				num = routeFieldInt % tableCount;

			}
		}
		//有传入openid
		table.append("_").append(StringUtils.leftPad(num + "", 3, "0"));
		tables.add(table.toString());
		return tables;
	}

}
