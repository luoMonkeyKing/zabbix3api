package org.cmbc.bigdata.zabbix;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ZabbixAPIResult {
  public int code;
  public String message;
  public Object data;

  public ZabbixAPIResult() {
    this.data = new Object();
  }

  public boolean isFail() {
    if (code != ZabbixAPIResultCode.SUCCESS.code()) {
      return true;
    }
    return false;
  }
}
