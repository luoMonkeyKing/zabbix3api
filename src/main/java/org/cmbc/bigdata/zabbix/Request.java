package org.cmbc.bigdata.zabbix;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class Request {
  private String jsonrpc = "2.0";

  private Object params = null;

  private String method;

  private String auth;

  private Integer id;

  public void initParam(String type) {
    if (type.equals("Map")) {
      params = new HashMap<String, Object>();
    } else if (type.equals("List")) {
      params = new ArrayList<>();
    }
  }

  public void putParam(String key, Object value) {
    ((Map)this.params).put(key, value);
  }

  public void removeParam(String key, Object value) {
    ((Map)this.params).remove(key);
  }

  public void addParam(Object param) {
    ((List)this.params).add(param);
  }

  @Override
  public String toString() {
    ObjectMapper mapper = new ObjectMapper();

    try {
      return mapper.writeValueAsString(this);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
}
