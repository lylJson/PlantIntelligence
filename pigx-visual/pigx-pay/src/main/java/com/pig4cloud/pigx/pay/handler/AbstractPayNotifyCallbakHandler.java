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

package com.pig4cloud.pigx.pay.handler;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * @author lengleng
 * @date 2019-06-27
 */
@Slf4j
public abstract class AbstractPayNotifyCallbakHandler implements PayNotifyCallbakHandler {
	/**
	 * 调用入口
	 *
	 * @param params
	 * @return
	 */
	@Override
	public String handle(Map<String, String> params) {

		// 去重处理
		if (duplicateChecker(params)) {
			return null;
		}

		// 验签处理
		if (!verifyNotify(params)) {
			return null;
		}

		return parse(params);
	}
}