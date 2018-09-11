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

  //private Map<String, Object> params = new HashMap<>();
  private Object params = null;

  private String method;

  private String auth;

  private Integer id;

  /*
  public void putParam(String key, Object value) {
    params.put(key, value);
  }
  */
  public void putParam(String key, Object value) {
    if (params == null) {
      params = new HashMap<String, Object>();
    }
    ((Map)this.params).put(key, value);
  }

  /*
  public Object removeParam(String key) {
    return params.remove(key);
  }
  */

  public void removeParam(String key, Object value) {
    ((Map)this.params).remove(key);
  }

  public void addParam(Map<String, Object> param) {
    if (params == null) {
      params = new ArrayList<HashMap>();
    }
    ((List)this.params).add(param);
  }
  /*
  public String getJsonrpc() {
    return jsonrpc;
  }

  public void setJsonrpc(String jsonrpc) {
    this.jsonrpc = jsonrpc;
  }


  public Map<String, Object> getParams() {
    return params;
  }

  public void setParams(Map<String, Object> params) {
    this.params = params;
  }


  public void setParams(Object params) {
    this.params = params;
  }

  public String getMethod() {
    return method;
  }

  public void setMethod(String method) {
    this.method = method;
  }

  public String getAuth() {
    return auth;
  }

  public void setAuth(String auth) {
    this.auth = auth;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }
  */

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
