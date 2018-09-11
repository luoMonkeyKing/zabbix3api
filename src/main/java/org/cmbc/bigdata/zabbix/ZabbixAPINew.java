package org.cmbc.bigdata.zabbix;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Log4j
public class ZabbixAPINew implements ZabbixAPIInterface {
  private URI uri;
  private volatile String auth;
  private CloseableHttpClient httpClient;

  public ZabbixAPINew(String url) {
    try {
      uri = new URI(url.trim());
    } catch (URISyntaxException e) {
      throw new RuntimeException("url invalid", e);
    }
  }

  public ZabbixAPINew(URI uri) {
    this.uri = uri;
  }

  public ZabbixAPINew(String url, CloseableHttpClient httpClient) {
    this(url);
    this.httpClient = httpClient;
  }

  public ZabbixAPINew(URI uri, CloseableHttpClient httpClient) {
    this(uri);
    this.httpClient = httpClient;
  }

  public void init() {
    if (httpClient == null) {
      httpClient = HttpClients.custom().build();
    }
  }

  public void destroy() {
    if (httpClient != null) {
      try {
        httpClient.close();
      } catch (Exception e) {
        log.error("close httpclient error!", e);
      }
    }
  }

  public boolean login(String user, String password) {
    this.auth = null;
    String method = "user.login";
    HashMap params = new HashMap();
    params.put("user", user);
    params.put("password", password);

    ZabbixAPIResult zabbixAPIResult = callApi(method, params);
    if (zabbixAPIResult.isFail()) {
      log.info("User " + user + " login failure. Error Info:" + zabbixAPIResult.getData());
      return false;
    } else {
      String auth = ((TextNode)zabbixAPIResult.getData()).asText();
      if (auth != null && !auth.isEmpty()) {
        this.auth = auth;
        log.info("User:" + user + " login success.");
        return true;
      }
      return false;
    }
  }

  public ZabbixAPIResult callApi(String method, Object params) {
    ZabbixAPIResult zabbixAPIResult = new ZabbixAPIResult();
    RequestBuilder requestBuilder = RequestBuilder.newBuilder().method(method);

    if (params != null && params instanceof Map) {
      for (Map.Entry<String, Object> param : ((Map<String, Object>)params).entrySet()) {
        requestBuilder.paramEntry(param.getKey(), param.getValue()).build();
      }
    }
    if (params != null && params instanceof List) {
      for (Map param:(List<Map>)params) {
        requestBuilder.paramAdd(param);
      }
    }
    JsonNode response = call(requestBuilder.build());
    if (response.has("error")) {
      zabbixAPIResult.setCode(response.get("error").get("code").asInt());
      zabbixAPIResult.setMessage(response.get("error").get("message").asText());
      zabbixAPIResult.setData(response.get("error").get("data").asText());
    } else {
      zabbixAPIResult.setCode(ZabbixAPIResultCode.SUCCESS.code());
      zabbixAPIResult.setMessage("Call Zabbix API Success.");
      zabbixAPIResult.setData(response.get("result"));
    }
    printAPIResult(zabbixAPIResult);

    return zabbixAPIResult;
  }

  public JsonNode call(Request request) {
    if (request.getAuth() == null) {
      request.setAuth(this.auth);
    }

    try {
      HttpUriRequest httpRequest = org.apache.http.client.methods.RequestBuilder.post().setUri(uri)
              .addHeader("Content-Type", "application/json")
              .setEntity(new StringEntity(request.toString(), ContentType.APPLICATION_JSON)).build();
      CloseableHttpResponse response = httpClient.execute(httpRequest);
      if (response == null) {
        System.out.println("response is null");
      }
      HttpEntity entity = response.getEntity();
      ObjectMapper mapper = new ObjectMapper();
      return mapper.readTree(entity.getContent());
    } catch (IOException e) {
      throw new RuntimeException("DefaultZabbixApi call exception!", e);
    }
  }

  private void printAPIResult(ZabbixAPIResult zabbixAPIResult) {
    try {
      System.out.println("Call API result:" + new ObjectMapper().writeValueAsString(zabbixAPIResult));
    } catch (Exception exception) {
      exception.printStackTrace();
    }
  }

  /*
    Get Zabbix API version. No need to login before that.
   */
  public ZabbixAPIResult apiVersion() {
    return callApi("apiinfo.version", null);
  }

  /*
    Create one host group.
    How to get the groupid:
    ZabbixAPIResult.data contains "groupids" field and fetch the first one
   */

  public ZabbixAPIResult hostgroupCreate(String groupname) {
    String method = "hostgroup.create";
    HashMap params = new HashMap();
    params.put("name", groupname);

    return callApi(method, params);
  }

  /*
    Create multiple host groups.
    Usage:
    HashMap map = new HashMap();
    map.put("name", "hg");
    groupnameArray.add(map);

    How to get the groupids:
    ZabbixAPIResult.data contains all the groupids created
   */
  public ZabbixAPIResult hostgroupsCreate(ArrayList<HashMap> groupnameArray) {
    String method = "hostgroup.create";

    return callApi(method, groupnameArray);
  }

  /*
  public Map getGroupIdByName(String groupname) {
    Map generalResponse = new HashMap();
    JSONObject filter = new JSONObject();
    filter.put("name", new String[] { groupname });

    Request request = RequestBuilder.newBuilder().method("hostgroup.get").paramEntry("filter", filter).build();
    JSONObject response = call(request);
    if (response.containsKey("error")) {
      generalResponse.put("error", response.get("error"));
    } else {
      if (response.getJSONArray("result").size() > 0) {
        System.out.println("hostgroup Search Result: " + response.getJSONArray("result"));
        String groupid = response.getJSONArray("result")
                .getJSONObject(0).getString("groupid");
        log.info("Found groupid:" + groupid + " for group:" + groupname);
        generalResponse.put("groupid", groupid);
      }
    }

    return generalResponse;
  }

  public Map hostCreateV3(String host, String groupId) {
    Map generalResponse = new HashMap();
    JSONObject hostInterface = new JSONObject();

    hostInterface.put("dns","");
    hostInterface.put("ip","127.0.0.1");
    hostInterface.put("main",1);
    hostInterface.put("port","10050");
    hostInterface.put("type",1);
    hostInterface.put("useip",1);

    JSONArray groups = new JSONArray();
    JSONObject group = new JSONObject();
    group.put("groupid", groupId);
    groups.add(group);
    Request request = RequestBuilder.newBuilder().method("host.create").paramEntry("host", host)
            .paramEntry("interfaces", hostInterface)
            .paramEntry("groups", groups).build();
    JSONObject response = call(request);

    if (response.containsKey("error")) {
      generalResponse.put("error", response.get("error"));
    } else {
      if (response.getJSONObject("result").getJSONArray("hostids").size() > 0) {
        generalResponse.put("hostid", response.getJSONObject("result").getJSONArray("hostids").getString(0));
      }
    }

    return generalResponse;
  }

  public Map getHostIdByName(String hostname) {
    Map generalResponse = new HashMap();
    JSONObject filter = new JSONObject();
    filter.put("host", new String[] { hostname });

    Request request = RequestBuilder.newBuilder().method("host.get").paramEntry("filter", filter).build();
    JSONObject response = call(request);
    if (response.containsKey("error")) {
      generalResponse.put("error", response.get("error"));
    } else {
      if (response.getJSONArray("result").size() > 0) {
        System.out.println("host Search Result: " + response.getJSONArray("result"));
        String hostid = response.getJSONArray("result")
                .getJSONObject(0).getString("hostid");
        log.info("Found hostid:" + hostid + " for host:" + hostname);
        generalResponse.put("hostid", hostid);
      }
    }

    return generalResponse;
  }

  public Map getItemIdByKeyAndHostId(String name, String hostid) {
    Map generalResponse = new HashMap();
    JSONObject filter = new JSONObject();
    filter.put("key_", new String[] { name });
    filter.put("hostid", new String[] { hostid });

    Request request = RequestBuilder.newBuilder().method("item.get").paramEntry("filter", filter).build();
    JSONObject response = call(request);
    if (response.containsKey("error")) {

    } else {
      if (response.getJSONArray("result").size() > 0) {
        System.out.println("item Search Result: " + response.getJSONArray("result"));
        String itemid = response.getJSONArray("result")
                .getJSONObject(0).getString("itemid");
        System.out.println("itemid=" + itemid);
        response.put("itemid", itemid);
      } else {
        log.info("item not found");
      }
    }

    return generalResponse;
  }

  public Map itemCreateFromMetric(String hostid, ZabbixMetric zabbixMetric) {
    Map generalResponse = new HashMap();

    Request request = RequestBuilder.newBuilder().method("item.create")
            .paramEntry("hostid", hostid)
            .paramEntry("key_", zabbixMetric.getItemKey())
            .paramEntry("name", zabbixMetric.getItemName())
            .paramEntry("type", ZabbixItemType.ZABBIX_TRAPPER.code())
            .paramEntry("value_type", ZabbixItemValueType.TEXT.code())
            .paramEntry("delay", 60).build();
    JSONObject response = call(request);
    System.out.println("itemCreateResult:" + response.toJSONString());

    if (response.containsKey("error")) {
      generalResponse.put("error", response.get("error"));
    } else {
      if (response.getJSONObject("result").getJSONArray("itemids").size() > 0) {
        generalResponse.put("itemid", response.getJSONObject("result").getJSONArray("itemids").getString(0));
      }
    }

    return generalResponse;
  }
*/
  public static void main(String[] args) {
    /*
    RequestConfig requestConfig = RequestConfig.custom()
            .setConnectTimeout(10 * 1000).setConnectionRequestTimeout(10 * 1000)
            .setSocketTimeout(10 * 1000).build();
    PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager();

    CloseableHttpClient httpclient = HttpClients.custom().setConnectionManager(connManager)
            .setDefaultRequestConfig(requestConfig).build();*/
    String url = String.format("http://%s:%d/zabbix/api_jsonrpc.php",
            "127.0.0.1", 8888);

    ZabbixAPINew zabbixApi3 = new ZabbixAPINew(url);
    zabbixApi3.init();
    System.out.println("login:" + zabbixApi3.login("Admin", "zabbix"));
    //zabbixApi3.apiVersion();
    //zabbixApi3.getHostIdByName("wenqiaodeMacBook-Pro-2.local");
    //zabbixApi3.getItemIdByKeyAndHostId("trapper", "10108");
    //zabbixApi3.getGroupIdByName("Templates");
    //zabbixApi3.setAuth("7bbef64eea33aa44e59eb9e5b6d585cf");
    ArrayList<HashMap> groupNameArray = new ArrayList();
    HashMap map1 = new HashMap();
    map1.put("name", "hg13");
    groupNameArray.add(map1);
    HashMap map2 = new HashMap();
    map2.put("name", "hg12");
    groupNameArray.add(map2);
    ZabbixAPIResult zabbixAPIResult = zabbixApi3.hostgroupsCreate(groupNameArray);

    if (zabbixAPIResult.isFail()) {
      System.out.println("fail to create host group");
    } else {
      JsonNode result = (JsonNode) zabbixAPIResult.getData();

      System.out.println("hostgroupid:" + result.get("groupids").get(0).toString());
    }

    zabbixApi3.destroy();
    zabbixApi3.setHttpClient(null);
  }
}
