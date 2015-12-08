package com.qding.callable.process.version.strategy;

/**
 * 字符串版本
 * @author lichao
 *
 */
public class StringVersionCompareStrategy extends VersionCompareStrategy{

	public int compare(String current, String target) {
		return current.compareTo(target);
	}
}
