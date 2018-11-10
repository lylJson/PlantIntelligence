/*
 *    Copyright (c) 2018-2025, lengleng All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * Neither the name of the pig4cloud.com developer nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * Author: lengleng (wangiegie@gmail.com)
 */

package com.pig4cloud.pigx.admin.service.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import com.pig4cloud.pigx.admin.api.entity.SysRouteConf;
import com.pig4cloud.pigx.admin.mapper.SysRouteConfMapper;
import com.pig4cloud.pigx.admin.service.SysRouteConfService;
import com.pig4cloud.pigx.common.core.constant.CommonConstant;
import com.pig4cloud.pigx.common.gateway.vo.RouteDefinitionVo;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author lengleng
 * @date 2018年11月06日10:27:55
 * <p>
 * 动态路由处理类
 */
@Slf4j
@AllArgsConstructor
@Service("sysRouteConfService")
public class SysRouteConfServiceImpl extends ServiceImpl<SysRouteConfMapper, SysRouteConf> implements SysRouteConfService {
	private final RedisTemplate redisTemplate;
	private final ApplicationEventPublisher applicationEventPublisher;


	/**
	 * 获取全部路由
	 * <p>
	 * RedisRouteDefinitionWriter.java
	 * PropertiesRouteDefinitionLocator.java
	 *
	 * @return
	 */
	@Override
	public List<SysRouteConf> routes() {
		SysRouteConf condition = new SysRouteConf();
		condition.setDelFlag(CommonConstant.STATUS_NORMAL);
		return baseMapper.selectList(new EntityWrapper<>(condition));
	}

	/**
	 * 更新路由信息
	 *
	 * @param routes 路由信息
	 * @return
	 */
	@Override
	public Mono<Void> editRoutes(JSONArray routes) {
		// 清空Redis 缓存
		Boolean result = redisTemplate.delete(CommonConstant.ROUTE_KEY);
		log.info("清空网关路由 {} ", result);

		// 遍历修改的routes，保存到Redis
		List<RouteDefinitionVo> routeDefinitionVoList = new ArrayList<>();
		routes.forEach(value -> {
			log.info("更新路由 ->{}", value);
			RouteDefinitionVo vo = new RouteDefinitionVo();
			Map<String, Object> map = (Map) value;

			Object id = map.get("routeId");
			if (id != null) {
				vo.setId(String.valueOf(id));
			}

			Object predicates = map.get("predicates");
			if (predicates != null) {
				JSONArray predicatesArray = (JSONArray) predicates;
				List<PredicateDefinition> predicateDefinitionList =
					predicatesArray.toList(PredicateDefinition.class);
				vo.setPredicates(predicateDefinitionList);
			}

			Object filters = map.get("filters");
			if (filters != null) {
				JSONArray filtersArray = (JSONArray) filters;
				List<FilterDefinition> filterDefinitionList
					= filtersArray.toList(FilterDefinition.class);
				vo.setFilters(filterDefinitionList);
			}

			Object uri = map.get("uri");
			if (uri != null) {
				vo.setUri(URI.create(String.valueOf(uri)));
			}

			Object order = map.get("order");
			if (order != null) {
				vo.setOrder(Integer.parseInt(String.valueOf(order)));
			}

			redisTemplate.setHashValueSerializer(new Jackson2JsonRedisSerializer<>(RouteDefinitionVo.class));
			redisTemplate.opsForHash().put(CommonConstant.ROUTE_KEY, vo.getId(), vo);
			routeDefinitionVoList.add(vo);
		});

		// 逻辑删除全部
		SysRouteConf condition = new SysRouteConf();
		condition.setDelFlag(CommonConstant.STATUS_NORMAL);
		this.delete(new EntityWrapper<>(condition));

		//插入生效路由
		List<SysRouteConf> routeConfList = routeDefinitionVoList.stream().map(vo -> {
			SysRouteConf routeConf = new SysRouteConf();
			routeConf.setRouteId(vo.getId());
			routeConf.setFilters(JSONUtil.toJsonStr(vo.getFilters()));
			routeConf.setPredicates(JSONUtil.toJsonStr(vo.getPredicates()));
			routeConf.setOrder(vo.getOrder());
			routeConf.setUri(vo.getUri().toString());
			return routeConf;
		}).collect(Collectors.toList());
		this.insertBatch(routeConfList);
		log.debug("更新网关路由结束 ");

		this.applicationEventPublisher.publishEvent(new RefreshRoutesEvent(this));
		return Mono.empty();
	}

}