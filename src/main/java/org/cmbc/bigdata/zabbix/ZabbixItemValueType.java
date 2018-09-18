package org.cmbc.bigdata.zabbix;

/**
 * Possible values:
 * 0 - numeric float;
 * 1 - character;
 * 2 - log;
 * 3 - numeric unsigned;
 * 4 - text.
 */
public enum ZabbixItemValueType {
  NUMERIC_FLOAT(0, "numeric float"),
  CHARACTER(1, "character"),
  LOG(2, "log"),
  NUMERIC_UNSIGNED(3, "numeric unsigned"),
  TEXT(4, "text");

  private Integer code;
  private String msg;

  ZabbixItemValueType(Integer code, String msg) {
    this.code = code;
    this.msg = msg;
  }

  public Integer code() {
    return this.code;
  }

  public String msg() {
    return this.msg;
  }
}