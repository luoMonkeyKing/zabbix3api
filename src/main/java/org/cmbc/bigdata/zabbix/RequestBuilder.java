package org.cmbc.bigdata.zabbix;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class RequestBuilder {
  private static final AtomicInteger nextId = new AtomicInteger(1);

  private RequestAbstract request;

  private RequestBuilder() {

  }

  static public org.cmbc.bigdata.zabbix.RequestBuilder newBuilder() {
    return new RequestBuilder();
  }

  public RequestAbstract build() {
    if (request.getId() == null) {
      request.setId(nextId.getAndIncrement());
    }

    return request;
  }

  public RequestBuilder version(String version) {
    request.setJsonrpc(version);
    return this;
  }

  public RequestBuilder initRequest(Object param) {
    if (param instanceof Map) {
      request = new MapRequest();
    } else if (param instanceof List) {
      request = new ListRequest();
    } else {
      request = new MapRequest();
    }
    request.setParams(param);

    return this;
  }

  /**
   * Do not necessary to call this method.If don not set id, ZabbixApi will auto set request auth..
   * @param auth authentication to login zabbix
   * @return RequestBuilder
   */
  public RequestBuilder auth(String auth) {
    request.setAuth(auth);
    return this;
  }

  public RequestBuilder method(String method) {
    request.setMethod(method);
    return this;
  }

  /**
   * Do not necessary to call this method.If don not set id, RequestBuilder will auto generate.
   *
   * @param id
   * @return
   */
  public RequestBuilder id(Integer id) {
    request.setId(id);
    return this;
  }
}
