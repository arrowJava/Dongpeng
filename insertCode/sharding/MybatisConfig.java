package com.pengxun.manager.base.config.mybatis;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.alibaba.druid.pool.DruidDataSource;
import com.pengxun.manager.base.config.mybatis.sharding.BoxCodeShardingStrategyConfiguration;
import com.pengxun.manager.base.config.mybatis.sharding.BoxInCheckCodeShardingStrategyConfiguration;
import com.pengxun.manager.base.config.mybatis.sharding.BoxOutCodeShardingStrategyConfiguration;
import com.pengxun.manager.base.config.mybatis.sharding.CodeGenerateShardingStrategyConfiguration;
import com.pengxun.manager.base.config.mybatis.sharding.CodeInCheckShardingStrategyConfiguration;
import com.pengxun.manager.base.config.mybatis.sharding.CodeShardingStrategyConfiguration;
import com.pengxun.manager.base.config.mybatis.sharding.OutCodeShardingStrategyConfiguration;
import com.pengxun.manager.base.config.mybatis.sharding.WxCodeShardingStrategyConfiguration;
import com.szeastroc.common.jdbc.multi.DynamicDataSource;
import com.szeastroc.common.jdbc.multi.JdbcConfig;
import com.szeastroc.common.jdbc.multi.MasterJdbcConfig;
import com.szeastroc.common.jdbc.page.PageInterceptor;

import io.shardingjdbc.core.api.ShardingDataSourceFactory;
import io.shardingjdbc.core.api.config.ShardingRuleConfiguration;
import io.shardingjdbc.core.api.config.TableRuleConfiguration;



@Configuration
@EnableTransactionManagement
@MapperScan({"com.pengxun.manager.dao"})
public class MybatisConfig {

	@Bean
	@Qualifier("sqlSessionFactory")
	public SqlSessionFactoryBean sqlSessionFactory(@Autowired DataSource dataSource) throws IOException {
		SqlSessionFactoryBean sqlSessionFactoryBean = new SqlSessionFactoryBean();
		sqlSessionFactoryBean.setDataSource(dataSource);
//		sqlSessionFactoryBean.setTransactionFactory(new MultiDataSourceTransactionFactory());
		org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();
		configuration.setMapUnderscoreToCamelCase(true);
		configuration.addInterceptor(new PageInterceptor());
		sqlSessionFactoryBean.setConfiguration(configuration);
		ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
		sqlSessionFactoryBean.setMapperLocations(resolver.getResources("classpath*:mapper/**/*.xml"));
		return sqlSessionFactoryBean;
	}






	@Bean
	public DataSource basicDataSource(MasterJdbcConfig masterJdbcConfig, CapCodeJdbcConfig capCodeJdbcConfig,
									  CapOutCodeJdbcConfig capOutCodeJdbcConfig, BoxCodeJdbcConfig boxCodeJdbcConfig, BoxOutCodeJdbcConfig boxOutCodeJdbcConfig,
									  CodeGenerateJdbcConfig codeGenerateJdbcConfig, BaseJdbcConfig baseJdbcConfig,StoreJdbcConfig storeJdbcConfig,
									  StoreGenerateConfig storeUserJdbcConfig,SourceCodeJdbcConfig sourceCodeJdbcConfig,LogShardingJdbcConfig logShardingJdbcConfig,ExchangeJdbcConfig exchangeJdbcConfig
	) throws SQLException {

		//后台用户及权限
		DruidDataSource master = new DruidDataSource();
		master.setDriverClassName(masterJdbcConfig.getDriverClassName());
		master.setUrl(masterJdbcConfig.getUrl());
		master.setUsername(masterJdbcConfig.getUsername());
		master.setPassword(masterJdbcConfig.getPassword());
		master.setMaxActive(64);
		master.setMinIdle(16);
		master.setMaxWait(60000);
		
		DataSource sourceCodeDataSource =  createDataSource(sourceCodeJdbcConfig, SourceCodeJdbcConfig.NAME);

		//新基础数据库（产品 产品类型 行政区域）
		DruidDataSource base = new DruidDataSource();
		base.setDriverClassName(baseJdbcConfig.getDriverClassName());
		base.setUrl(baseJdbcConfig.getUrl());
		base.setUsername(baseJdbcConfig.getUsername());
		base.setPassword(baseJdbcConfig.getPassword());
		base.setMaxActive(64);
		base.setMinIdle(16);
		base.setMaxWait(60000);

		//商户业务库
		DataSource storeSource = createDataSource(storeJdbcConfig, StoreJdbcConfig.NAME);
		Map <String, DataSource> storeDataSourceMap = new HashMap <String, DataSource>();
		storeDataSourceMap.put("store", storeSource);

		TableRuleConfiguration boxInCodeTableRuleConfig = new TableRuleConfiguration();
		boxInCodeTableRuleConfig.setLogicTable("t_box_in_check_code");
		boxInCodeTableRuleConfig.setTableShardingStrategyConfig(new BoxInCheckCodeShardingStrategyConfiguration("box_in_code"));

		TableRuleConfiguration bottleInCodeTableRuleConfig = new TableRuleConfiguration();
		bottleInCodeTableRuleConfig.setLogicTable("t_bottle_in_check_code");
		bottleInCodeTableRuleConfig.setTableShardingStrategyConfig(new CodeInCheckShardingStrategyConfiguration("bottle_in_code"));

		ShardingRuleConfiguration storeShardingRuleConfig = new ShardingRuleConfiguration();
		storeShardingRuleConfig.getTableRuleConfigs().add(boxInCodeTableRuleConfig);
		storeShardingRuleConfig.getTableRuleConfigs().add(bottleInCodeTableRuleConfig);


		//瓶内码库
		DataSource codeDataSource = createDataSource(capCodeJdbcConfig,CapCodeJdbcConfig.NAME);
		Map <String, DataSource> codeDataSourceMap = new HashMap <String, DataSource>();
		codeDataSourceMap.put("code", codeDataSource);

		TableRuleConfiguration codeTableRuleConfig = new TableRuleConfiguration();
		codeTableRuleConfig.setLogicTable("t_cap_code");
		codeTableRuleConfig.setTableShardingStrategyConfig(new CodeShardingStrategyConfiguration("code"));

		ShardingRuleConfiguration codeShardingRuleConfig = new ShardingRuleConfiguration();
		codeShardingRuleConfig.getTableRuleConfigs().add(codeTableRuleConfig);

		//瓶外码库
		DataSource outCodeDataSource = createDataSource(capOutCodeJdbcConfig,CapOutCodeJdbcConfig.NAME);
		Map <String, DataSource> outCodeDataSourceMap = new HashMap <String, DataSource>();
		outCodeDataSourceMap.put("outCode", outCodeDataSource);

		TableRuleConfiguration outCodeTableRuleConfig = new TableRuleConfiguration();
		outCodeTableRuleConfig.setLogicTable("t_cap_out_code");
		outCodeTableRuleConfig.setTableShardingStrategyConfig(new OutCodeShardingStrategyConfiguration("out_code"));

		ShardingRuleConfiguration outCodeShardingRuleConfig = new ShardingRuleConfiguration();
		outCodeShardingRuleConfig.getTableRuleConfigs().add(outCodeTableRuleConfig);

		//箱内码库(源码)
		DataSource boxCodeDataSource = createDataSource(boxCodeJdbcConfig,BoxCodeJdbcConfig.NAME);
		Map <String, DataSource> boxCodeDataSourceMap = new HashMap <String, DataSource>();
		boxCodeDataSourceMap.put("sourceBoxcode", boxCodeDataSource);

		TableRuleConfiguration boxCodeTableRuleConfig = new TableRuleConfiguration();
		boxCodeTableRuleConfig.setLogicTable("t_sourcebox_code");
		boxCodeTableRuleConfig.setTableShardingStrategyConfig(new BoxCodeShardingStrategyConfiguration("code"));

		ShardingRuleConfiguration boxCodeShardingRuleConfig = new ShardingRuleConfiguration();
		boxCodeShardingRuleConfig.getTableRuleConfigs().add(boxCodeTableRuleConfig);

		//箱外码库
		DataSource boxOutCodeDataSource = createDataSource(boxOutCodeJdbcConfig,BoxOutCodeJdbcConfig.NAME);
		Map <String, DataSource> boxOutCodeDataSourceMap = new HashMap <String, DataSource>();
		boxOutCodeDataSourceMap.put("boxOutCode", boxOutCodeDataSource);

		TableRuleConfiguration boxOutCodeTableRuleConfig = new TableRuleConfiguration();
		boxOutCodeTableRuleConfig.setLogicTable("t_box_out_code");
		boxOutCodeTableRuleConfig.setTableShardingStrategyConfig(new BoxOutCodeShardingStrategyConfiguration("out_code"));

		ShardingRuleConfiguration boxOutCodeShardingRuleConfig = new ShardingRuleConfiguration();
		boxOutCodeShardingRuleConfig.getTableRuleConfigs().add(boxOutCodeTableRuleConfig);

		//生成码库
		DataSource codeGenerateDataSource = createDataSource(codeGenerateJdbcConfig,CodeGenerateJdbcConfig.NAME);
		Map <String, DataSource> codeGenerateDataSourceMap = new HashMap <String, DataSource>();
		codeGenerateDataSourceMap.put("codeGenerate", codeGenerateDataSource);

		TableRuleConfiguration codeGenerateTableRuleConfig = new TableRuleConfiguration();
		codeGenerateTableRuleConfig.setLogicTable("t_generate_code");
		codeGenerateTableRuleConfig.setTableShardingStrategyConfig(new CodeGenerateShardingStrategyConfiguration("code"));

		TableRuleConfiguration wxCodeTableRuleConfig = new TableRuleConfiguration();
		wxCodeTableRuleConfig.setLogicTable("t_wxcode_apply_code");
		wxCodeTableRuleConfig.setTableShardingStrategyConfig(new WxCodeShardingStrategyConfiguration("qr_code"));

		ShardingRuleConfiguration codeGenerateShardingRuleConfig = new ShardingRuleConfiguration();
		codeGenerateShardingRuleConfig.getTableRuleConfigs().add(codeGenerateTableRuleConfig);
		codeGenerateShardingRuleConfig.getTableRuleConfigs().add(wxCodeTableRuleConfig);

		//noSharding 生成码库
		DataSource noShardingCodeGenerateDataSource = createDataSource(codeGenerateJdbcConfig,CodeGenerateJdbcConfig.NO_SHARDING_NAME);
		//noSharding 瓶内码库
		DataSource noShardingCodeDataSource = createDataSource(capCodeJdbcConfig,CapCodeJdbcConfig.NO_SHARDING_NAME);
		//noSharding 瓶外码库
		DataSource noShardingOutCodeDataSource = createDataSource(capOutCodeJdbcConfig,CapOutCodeJdbcConfig.NO_SHARDING_NAME);
		//noSharding 箱内码库
		DataSource noShardingBoxCodeDataSource = createDataSource(boxCodeJdbcConfig,BoxCodeJdbcConfig.NO_SHARDING_NAME);
		//noSharding 箱外码库
		DataSource noShardingBoxOutCodeDataSource = createDataSource(boxOutCodeJdbcConfig,BoxOutCodeJdbcConfig.NO_SHARDING_NAME);
		// 动态源
		DynamicDataSource dynamicDataSource = new DynamicDataSource();
		dynamicDataSource.setDefaultTargetDataSource(master);
		Map<Object, Object> targetDataSources = new HashMap<Object, Object>();
		targetDataSources.put(MasterJdbcConfig.NAME, master);
		targetDataSources.put(BaseJdbcConfig.NAME, base);
		targetDataSources.put(StoreJdbcConfig.NAME, ShardingDataSourceFactory.createDataSource(storeDataSourceMap, storeShardingRuleConfig, new HashMap <String, Object>(), new Properties()));
		targetDataSources.put(CapCodeJdbcConfig.NAME, ShardingDataSourceFactory.createDataSource(codeDataSourceMap, codeShardingRuleConfig, new HashMap <String, Object>(), new Properties()));
		targetDataSources.put(CapOutCodeJdbcConfig.NAME, ShardingDataSourceFactory.createDataSource(outCodeDataSourceMap, outCodeShardingRuleConfig, new HashMap <String, Object>(), new Properties()));
		targetDataSources.put(BoxCodeJdbcConfig.NAME, ShardingDataSourceFactory.createDataSource(boxCodeDataSourceMap, boxCodeShardingRuleConfig, new HashMap <String, Object>(), new Properties()));
		targetDataSources.put(BoxOutCodeJdbcConfig.NAME, ShardingDataSourceFactory.createDataSource(boxOutCodeDataSourceMap, boxOutCodeShardingRuleConfig, new HashMap <String, Object>(), new Properties()));
		targetDataSources.put(CodeGenerateJdbcConfig.NAME, ShardingDataSourceFactory.createDataSource(codeGenerateDataSourceMap, codeGenerateShardingRuleConfig, new HashMap <String, Object>(), new Properties()));
		targetDataSources.put(CodeGenerateJdbcConfig.NO_SHARDING_NAME, noShardingCodeGenerateDataSource);
		targetDataSources.put(CapCodeJdbcConfig.NO_SHARDING_NAME, noShardingCodeDataSource);
		targetDataSources.put(CapOutCodeJdbcConfig.NO_SHARDING_NAME, noShardingOutCodeDataSource);
		targetDataSources.put(BoxCodeJdbcConfig.NO_SHARDING_NAME, noShardingBoxCodeDataSource);
		targetDataSources.put(BoxOutCodeJdbcConfig.NO_SHARDING_NAME, noShardingBoxOutCodeDataSource);
        targetDataSources.put(SourceCodeJdbcConfig.NAME, sourceCodeDataSource);
        targetDataSources.put(ExchangeJdbcConfig.NAME, createDataSource(exchangeJdbcConfig,ExchangeJdbcConfig.NAME));
		targetDataSources.put(StoreGenerateConfig.NAME, storeSource);//商户模块
		targetDataSources.put(LogShardingJdbcConfig.NAME,createDataSource(logShardingJdbcConfig,LogShardingJdbcConfig.NAME));//商户日志模块

		dynamicDataSource.setTargetDataSources(targetDataSources);
		return dynamicDataSource;
	}






	@Bean
	public SqlSessionTemplate sqlSessionTemplate(@Autowired SqlSessionFactory sqlSessionFactory) {
		SqlSessionTemplate sqlSessionTemplate = new SqlSessionTemplate(sqlSessionFactory);
		return sqlSessionTemplate;
	}

	public DataSource createDataSource(JdbcConfig jdbcConfig,String name) throws SQLException {
		DruidDataSource dataSource = new DruidDataSource();
		dataSource.setDriverClassName(jdbcConfig.getDriverClassName());
		dataSource.setUrl(jdbcConfig.getUrl());
		dataSource.setUsername(jdbcConfig.getUsername());
		dataSource.setPassword(jdbcConfig.getPassword());
		dataSource.setMaxActive(64);
		dataSource.setMinIdle(16);
		dataSource.setMaxWait(60000);
		return dataSource;
	}


}

