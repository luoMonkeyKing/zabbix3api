package org.cmbc.bigdata.zabbix;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import lombok.Data;
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
import java.util.*;

@Data
@Log4j
public class ZabbixApi implements ZabbixAPIInterface {
  private URI uri;
  private volatile String auth;
  private CloseableHttpClient httpClient;

  public ZabbixApi(String url) {
    try {
      uri = new URI(url.trim());
    } catch (URISyntaxException e) {
      throw new RuntimeException("url invalid", e);
    }
  }

  public ZabbixApi(URI uri) {
    this.uri = uri;
  }

  public ZabbixApi(String url, CloseableHttpClient httpClient) {
    this(url);
    this.httpClient = httpClient;
  }

  public ZabbixApi(URI uri, CloseableHttpClient httpClient) {
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
      httpClient = null;
    }
  }

  public boolean login(String user, String password) {
    this.auth = null;
    String method = "user.login";
    HashMap<String, String> params = new HashMap();
    params.put("user", user);
    params.put("password", password);

    ZabbixAPIResult zabbixAPIResult = callApi(method, params);
    if (zabbixAPIResult.isFail()) {
      log.info("User " + user + " login failure. Error Info:" + zabbixAPIResult.getData());
      return false;
    } else {
      String auth = ((TextNode) zabbixAPIResult.getData()).asText();
      if (auth != null && !auth.isEmpty()) {
        this.auth = auth;
        log.info("User:" + user + " login success.");
        return true;
      }
      return false;
    }
  }

  public ZabbixAPIResult callApi(String method) {
    return callApi(method, Collections.emptyList());
  }

  public ZabbixAPIResult callApi(String method, Object params) {
    ZabbixAPIResult zabbixAPIResult = new ZabbixAPIResult();
    RequestBuilder requestBuilder = RequestBuilder.newBuilder().initRequest(params).method(method);

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

  public JsonNode call(RequestAbstract request) {
    if (request.getAuth() == null) {
      request.setAuth(this.auth);
    }

    try {
      HttpUriRequest httpRequest = org.apache.http.client.methods.RequestBuilder.post().setUri(uri)
              .addHeader("Content-Type", "application/json")
              .setEntity(new StringEntity(request.toString(), ContentType.APPLICATION_JSON)).build();
      log.info(("Call API. Request is :" + request.toString()));
      CloseableHttpResponse response = httpClient.execute(httpRequest);
      HttpEntity entity = response.getEntity();
      return new ObjectMapper().readTree(entity.getContent());
    } catch (IOException e) {
      throw new RuntimeException("DefaultZabbixApi call exception!", e);
    }
  }

  private void printAPIResult(ZabbixAPIResult zabbixAPIResult) {
    try {
      log.info("Call API. Result is :" + new ObjectMapper().
              writerWithDefaultPrettyPrinter().writeValueAsString(zabbixAPIResult));
    } catch (Exception exception) {
      exception.printStackTrace();
    }
  }

  /**
   * Get Zabbix API version. No need to login before that.
   * @return
   */
  public ZabbixAPIResult apiVersion() {
    return callApi("apiinfo.version");
  }

  /**
   * Create one host group.
   *
   * @param groupname host group name
   * @return ZabbixAPIResult.data contains "groupids" field and fetch the first one of groupids.
   * If error happened, refer to code, message, data for information.
   */
  public ZabbixAPIResult hostgroupCreate(String groupname) {
    String method = "hostgroup.create";
    HashMap params = new HashMap();
    params.put("name", groupname);

    return callApi(method, params);
  }

  /**
   * Create multiple host groups.
   *
   * @param groupNameList List of host group names
   * @return ZabbixAPIResult.data.groupids is the host group id array created
   */
  public ZabbixAPIResult hostgroupListCreate(ArrayList<String> groupNameList) {
    String method = "hostgroup.create";
    ArrayList<HashMap<String, String>> params = new ArrayList<>();

    groupNameList.forEach(groupname -> {
      HashMap<String, String> map = new HashMap();
      map.put("name", groupname);
      params.add(map);
    });

    return callApi(method, params);
  }

  /**
   * Get multiple host groups by list of of host group names.
   *
   * @param groupNameList List of host group names
   * @return ZabbixAPIResult.data is the host group array found, with each one including groupid,name,flags,internal
   */
  public ZabbixAPIResult hostgroupListGetByName(ArrayList<String> groupNameList) {
    String method = "hostgroup.get";
    HashMap<String, HashMap<String, List<String>>> params = new HashMap();
    HashMap<String, List<String>> nameMap = new HashMap();
    nameMap.put("name", groupNameList);
    params.put("filter", nameMap);

    return callApi(method, params);
  }

  /**
   * Get multiple host group ids by list of of host group names.
   *
   * @param groupNameList List of host group names
   * @return ZabbixAPIResult.data is the host group id array found
   */
  public ZabbixAPIResult hostgroupIdListGetByName(ArrayList<String> groupNameList) {
    ZabbixAPIResult hostgroupGetResult = hostgroupListGetByName(groupNameList);
    ArrayList<String> groupIdList = new ArrayList<>();

    if (hostgroupGetResult.isFail()) return hostgroupGetResult;
    JsonNode data = (JsonNode) hostgroupGetResult.getData();
    data.forEach(group -> {
      groupIdList.add(group.get("groupid").asText());
    });

    hostgroupGetResult.setData(groupIdList);

    return hostgroupGetResult;
  }

  /**
   * Get host group by host group name.
   *
   * @param groupname host group name
   * @return ZabbixAPIResult.data is the host group array found, with each one including groupid,name,flags,internal.
   * If found, get the first item.
   */
  public ZabbixAPIResult hostgroupGetByGroupName(String groupname) {
    ArrayList<String> groupNameList = new ArrayList<>();
    groupNameList.add(groupname);

    return hostgroupListGetByName(groupNameList);
  }

  /**
   * Get host groups by host names
   *
   * @param hostNameList List of host name
   * @return ZabbixAPIResult.data is the host group array found, with each one including groupid,name,flags,internal.
   */
  public ZabbixAPIResult hostgroupGetByHostNameList(ArrayList<String> hostNameList) {
    ZabbixAPIResult hostGetResult = hostListGetByHostName(hostNameList);
    if (hostGetResult.isFail()) return hostGetResult;

    JsonNode data = (JsonNode) hostGetResult.getData();
    ArrayList<String> hostIdList = new ArrayList<>();
    data.get("hostids").forEach(hostid -> {
      hostIdList.add(hostid.asText());
    });

    HashMap<String, List<String>> params = new HashMap();
    params.put("hostids", hostIdList);

    String method = "hostgroup.get";
    return callApi(method, params);
  }

  /**
   * Get host group by host name
   *
   * @param host host name
   * @return ZabbixAPIResult.data is the host group array found, with each one including groupid,name,flags,internal.
   * If found, get the first item.
   */
  public ZabbixAPIResult hostgroupGetByHostName(String host) {
    ArrayList<String> hostNameList = new ArrayList<>();
    hostNameList.add(host);

    return hostgroupGetByHostNameList(hostNameList);
  }

  /**
   * Check host group whether exists.
   *
   * @param groupname host group name
   * @return True means the host group exists. False means the host group not exists or search request returns some error.
   */
  public boolean hostgroupExists(String groupname) {
    boolean exists = false;
    ZabbixAPIResult hostgroupGetResult = hostgroupGetByGroupName(groupname);
    if (!hostgroupGetResult.isFail()) {
      JsonNode data = (JsonNode) hostgroupGetResult.getData();
      if (data.size() > 0) exists = true;
    }
    return exists;
  }

  /**
   * Delete multiple host groups by group ids
   *
   * @param hostGroupIdList List of host group ids
   * @return ZabbixAPIResult.data.groupids returns groupids that have been deleted
   */
  public ZabbixAPIResult hostgroupListDeleteById(ArrayList<String> hostGroupIdList) {
    String method = "hostgroup.delete";

    return callApi(method, hostGroupIdList);
  }

  /**
   * Delete multiple host groups by group name
   *
   * @param groupNameList List of host group names
   * @return ZabbixAPIResult.data.groupids returns groupids that have been deleted
   */
  public ZabbixAPIResult hostgroupListDeleteByName(ArrayList<String> groupNameList) {
    ZabbixAPIResult hostgroupGetResult = hostgroupIdListGetByName(groupNameList);
    if (hostgroupGetResult.isFail()) return hostgroupGetResult;

    ArrayList<String> hostGroupIdList = (ArrayList<String>) hostgroupGetResult.getData();
    if (hostGroupIdList.size() > 0) {
      return hostgroupListDeleteById(hostGroupIdList);
    } else {
      ZabbixAPIResult zabbixAPIResult = new ZabbixAPIResult();
      zabbixAPIResult.setCode(ZabbixAPIResultCode.SUCCESS.code());
      zabbixAPIResult.setMessage("Call Zabbix API Success.");
      HashMap<String, List<String>> groupids = new HashMap();
      groupids.put("groupids", hostGroupIdList);
      zabbixAPIResult.setData(groupids);

      return zabbixAPIResult;
    }
  }

  /**
   * Delete one host group by group id
   *
   * @param hostGroupId host group id
   * @return ZabbixAPIResult.data.groupids.get(0) returns groupid that has been deleted
   */
  public ZabbixAPIResult hostgroupDeleteById(String hostGroupId) {
    ArrayList<String> hostGroupIdList = new ArrayList<>();
    hostGroupIdList.add(hostGroupId);

    return hostgroupListDeleteById(hostGroupIdList);
  }

  /**
   * Delete one host group by group name
   *
   * @param groupname host group name
   * @return ZabbixAPIResult.data.groupids.get(0) returns groupid that has been deleted
   */
  public ZabbixAPIResult hostgroupDeleteByName(String groupname) {
    ArrayList<String> groupNameList = new ArrayList<>();
    groupNameList.add(groupname);

    return hostgroupListDeleteByName(groupNameList);
  }

  /**
   * Create one host
   *
   * @param host           host name
   * @param groupIdList    List of groupId to add the host to
   * @param hostInterfaces Interfaces to be created for the host
   * @return ZabbixAPIResult.data.hostids is the host id array created
   */
  public ZabbixAPIResult hostCreate(String host, ArrayList<String> groupIdList, Object hostInterfaces) {
    String method = "host.create";

    ArrayList<HashMap> groups = new ArrayList();
    groupIdList.forEach(groupId -> {
      HashMap<String, String> group = new HashMap();
      group.put("groupid", groupId);
      groups.add(group);
    });

    HashMap<String, Object> params = new HashMap();
    params.put("host", host);
    params.put("groups", groups);
    params.put("interfaces", hostInterfaces);

    return callApi(method, params);
  }

  /**
   * Create multiple hosts.
   *
   * @param hostParamList List of host params. Each param should contain host, groups, interfaces.
   * @return ZabbixAPIResult.data.hostids is the host id array created
   */
  public ZabbixAPIResult hostListCreate(ArrayList<HashMap> hostParamList) {
    String method = "host.create";

    return callApi(method, hostParamList);
  }

  /**
   * Check host whether exists.
   *
   * @param host host name
   * @return True means the host exists. False means the host not exists or search request returns some error.
   */
  public boolean hostExists(String host) {
    boolean exists = false;
    ZabbixAPIResult hostGetResult = hostGetByHostName(host);
    if (!hostGetResult.isFail()) {
      JsonNode data = (JsonNode) hostGetResult.getData();
      if (data.size() > 0) exists = true;
    }
    return exists;
  }

  /**
   * Get hosts by host name list.
   *
   * @param hostNameList List of host name
   * @return ZabbixAPIResult.data is the host array, with each one including host properties such as
   * host, hostid, name, status etc.
   */
  public ZabbixAPIResult hostListGetByHostName(ArrayList<String> hostNameList) {
    String method = "host.get";

    HashMap<String, HashMap<String, List<String>>> params = new HashMap();
    HashMap<String, List<String>> filter = new HashMap();
    filter.put("host", hostNameList);
    params.put("filter", filter);

    return callApi(method, params);
  }

  /**
   * Get host by host name.
   *
   * @param host host name
   * @return ZabbixAPIResult.data is the host array, with each one including host properties such as
   * host, hostid, name, status etc. If exists, fetch the first one.
   */
  public ZabbixAPIResult hostGetByHostName(String host) {
    ArrayList<String> hostNameList = new ArrayList();
    hostNameList.add(host);

    return hostListGetByHostName(hostNameList);
  }

  /**
   * Get host by group names.
   *
   * @param groupNameList List of host group name
   * @return ZabbixAPIResult.data is the host array found.
   */
  public ZabbixAPIResult hostGetByGroupName(ArrayList<String> groupNameList) {
    String method = "host.get";

    ZabbixAPIResult hostgroupGetResult = hostgroupIdListGetByName(groupNameList);
    if (hostgroupGetResult.isFail()) return hostgroupGetResult;
    ArrayList<String> groupIdList = (ArrayList<String>) hostgroupGetResult.getData();

    HashMap<String, List<String>> params = new HashMap();
    if (groupIdList.size() > 0) {
      params.put("groupids", groupIdList);
    }

    return callApi(method, params);
  }

  /**
   * Get host by host names and group names.
   *
   * @param hostNameList  List of host name.
   * @param groupNameList List of host group name
   * @return ZabbixAPIResult.data is the host array found.
   */
  public ZabbixAPIResult hostGetByHostNameAndGroupName(ArrayList<String> hostNameList,
                                                       ArrayList<String> groupNameList) {
    String method = "host.get";

    ZabbixAPIResult hostgroupGetResult = hostgroupIdListGetByName(groupNameList);
    if (hostgroupGetResult.isFail()) return hostgroupGetResult;

    ArrayList<String> groupIdList = (ArrayList<String>) hostgroupGetResult.getData();
    HashMap<String, Object> params = new HashMap();
    if (groupIdList.size() > 0) {
      params.put("groupids", groupIdList);
    }

    HashMap<String, List<String>> filter = new HashMap();
    filter.put("host", hostNameList);
    params.put("filter", filter);

    return callApi(method, params);
  }

  /**
   * Delete multiple hosts by host ids.
   *
   * @param hostIdList List of host id
   * @return ZabbixAPIResult.data.hostids is host id array that have been deleted
   */
  public ZabbixAPIResult hostListDeleteById(ArrayList<String> hostIdList) {
    String method = "host.delete";

    return callApi(method, hostIdList);
  }

  /**
   * Delete host by host id
   *
   * @param hostId host id
   * @return ZabbixAPIResult.data.hostids is host id array that have been deleted.
   * If host id exists, fetch the first one.
   */
  public ZabbixAPIResult hostDeleteById(String hostId) {
    ArrayList<String> hostIdList = new ArrayList<>();
    hostIdList.add(hostId);

    return hostgroupListDeleteById(hostIdList);
  }

  /**
   * Delete hosts by host names.
   *
   * @param hostNameList List of host name
   * @return ZabbixAPIResult.data.hostids is host id array that have been deleted.
   * If host id exists, fetch the first one.
   */
  public ZabbixAPIResult hostListDeleteByName(ArrayList<String> hostNameList) {
    ZabbixAPIResult hostListGetResult = hostListGetByHostName(hostNameList);
    if (hostListGetResult.isFail()) return hostListGetResult;
    JsonNode data = (JsonNode) hostListGetResult.getData();
    ArrayList<String> hostIdList = new ArrayList<>();
    if (data.size() > 0) {
      data.forEach(host -> {
        hostIdList.add(host.get("hostid").asText());
      });
    }

    if (hostIdList.size() > 0) {
      return hostListDeleteById(hostIdList);
    }

    ZabbixAPIResult zabbixAPIResult = new ZabbixAPIResult();
    zabbixAPIResult.setCode(ZabbixAPIResultCode.SUCCESS.code());
    zabbixAPIResult.setMessage("Call Zabbix API Success.");
    HashMap<String, List<String>> groupids = new HashMap();
    groupids.put("hostids", hostIdList);
    zabbixAPIResult.setData(groupids);

    return zabbixAPIResult;
  }

  /**
   * Delete host by host name.
   *
   * @param host host name
   * @return ZabbixAPIResult.data.hostids is host id array that have been deleted.
   * If host id exists, fetch the first one.
   */
  public ZabbixAPIResult hostDeleteByName(String host) {
    ArrayList<String> hostNameList = new ArrayList<>();
    hostNameList.add(host);

    return hostListDeleteByName(hostNameList);
  }

  /**
   * Create host interface.
   *
   * @param dns    required property. DNS name used by the interface. Can be empty if the connection is made via IP.
   * @param hostid required property. ID of the host the interface belongs to.
   * @param ip     required property. IP address used by the interface. Can be empty if the connection is made via DNS.
   * @param main   required property. Whether the interface is used as default on the host.
   *               Possible values are:
   *               0 - not default;
   *               1 - default.
   * @param port   required property. Port number used by the interface.
   * @param type   required property. Interface type.
   *               Possible values are:
   *               1 - agent;
   *               2 - SNMP;
   *               3 - IPMI;
   *               4 - JMX.
   * @param useip  required property. Whether the connection should be made via IP.
   *               Possible values are:
   *               0 - connect using host DNS name;
   *               1 - connect using host IP address for this host interface.
   * @param bulk   not required property. Whether to use bulk SNMP requests.
   *               Possible values are:
   *               0 - don't use bulk requests;
   *               1 - (default) use bulk requests.
   * @return ZabbixAPIResult.data.interfaceids is the interface id array
   */
  public ZabbixAPIResult hostInterfaceCreate(String dns, String hostid, String ip, String main,
                                             String port, String type, String useip, String bulk) {
    String method = "hostinterface.create";

    HashMap<String, String> param = new HashMap();
    param.put("dns", dns);
    param.put("hostid", hostid);
    param.put("ip", ip);
    param.put("main", main);
    param.put("port", port);
    param.put("type", type);
    param.put("useip", useip);
    param.put("bulk", bulk);

    return callApi(method, param);
  }

  /**
   * Create multiple host interfaces.
   *
   * @param hostInterfaceList List of host interface params
   * @return ZabbixAPIResult.data.interfaceids is the interface id array
   */
  public ZabbixAPIResult hostInterfaceListCreate(ArrayList<HashMap> hostInterfaceList) {
    String method = "hostinterface.create";
    String[] requiredProperties = {"dns", "hostid", "ip", "main", "port", "type", "useip"};
    ZabbixAPIResult zabbixAPIResult = new ZabbixAPIResult();

    for (int i = 0; i < hostInterfaceList.size(); i++) {
      for (int j = 0; j < requiredProperties.length; j++) {
        if (!hostInterfaceList.get(i).containsKey(requiredProperties[j])) {
          zabbixAPIResult.setCode(ZabbixAPIResultCode.PARAM_IS_INVALID.code());
          zabbixAPIResult.setMessage(ZabbixAPIResultCode.PARAM_IS_INVALID.message() +
                  requiredProperties + " are required.");
          zabbixAPIResult.setData("Param has no property : " + requiredProperties[j]);
          return zabbixAPIResult;
        }
      }
    }
    return callApi(method, hostInterfaceList);
  }

  /**
   * Get host interface by host ids.
   *
   * @param hostIdList List of host id
   * @return ZabbixAPIResult.data is host interface array found, with each one including
   * interfaceid, hostid, main, type, useip, ip, etc.
   */
  public ZabbixAPIResult hostInterfaceGetByHostIds(ArrayList<String> hostIdList) {
    String method = "hostinterface.get";

    HashMap<String, List<String>> param = new HashMap();
    param.put("hostids", hostIdList);

    return callApi(method, param);
  }

  /**
   * Get host interfaces by host names.
   *
   * @param hostNameList List of host name
   * @return ZabbixAPIResult.data is host interface array found, with each one including
   * interfaceid, hostid, main, type, useip, ip, etc.
   */
  public ZabbixAPIResult hostInterfaceGetByHostNames(ArrayList<String> hostNameList) {
    String method = "hostinterface.get";

    ZabbixAPIResult hostGetResult = hostListGetByHostName(hostNameList);
    if (hostGetResult.isFail()) return hostGetResult;

    ArrayList hostIdList = new ArrayList();
    JsonNode data = (JsonNode) hostGetResult.getData();
    data.forEach(host -> {
      hostIdList.add(host.get("hostid"));
    });

    HashMap<String, List<String>> param = new HashMap();
    param.put("hostids", hostIdList);

    return callApi(method, param);
  }

  /**
   * Delete multiple host interfaces by host interface ids
   *
   * @param hostInterfaceIdList List of h ids
   * @return ZabbixAPIResult.data.interfaceids returns interfaceids that have been deleted
   */
  public ZabbixAPIResult hostInterfaceListDeleteById(ArrayList<String> hostInterfaceIdList) {
    String method = "hostinterface.delete";

    return callApi(method, hostInterfaceIdList);
  }

  /**
   * Create item.
   *
   * @param param item parameter.
   *              It should contain the required properties:delay,hostid,interfaceid,key_,name,type,value_type.
   * @return ZabbixAPIResult.data contains "itemids" field and fetch the first one of itemids.
   * *         If error happened, refer to code, message, data for information.
   */
  public ZabbixAPIResult itemCreate(HashMap<String, Object> param) {
    ArrayList<HashMap<String, Object>> params = new ArrayList<>();

    params.add(param);

    return itemListCreate(params);
  }

  /**
   * Create multiple items.
   *
   * @param params List of item parameter .
   *               Each parameter should contain the required properties:delay,hostid,interfaceid,key_,name,type,value_type.
   * @return ZabbixAPIResult.data contains "itemids".
   * *         If error happened, refer to code, message, data for information.
   */
  public ZabbixAPIResult itemListCreate(ArrayList<HashMap<String, Object>> params) {
    String method = "item.create";

    String[] requiredProperties = {"delay", "hostid", "interfaceid", "key_", "name", "type", "value_type"};
    ZabbixAPIResult zabbixAPIResult = new ZabbixAPIResult();
    for (int i = 0; i < params.size(); i++) {
      HashMap param = params.get(i);
      for (int j = 0; j < requiredProperties.length; j++) {
        if (!param.containsKey(requiredProperties[j])) {
          zabbixAPIResult.setCode(ZabbixAPIResultCode.PARAM_IS_INVALID.code());
          zabbixAPIResult.setMessage(ZabbixAPIResultCode.PARAM_IS_INVALID.message() +
                  requiredProperties + " are required.");
          zabbixAPIResult.setData("Param has no property : " + requiredProperties[j]);
          return zabbixAPIResult;
        }
      }
    }

    return callApi(method, params);
  }

  /**
   * Check whether item exists by host name and item key.
   *
   * @param hostname host name
   * @param itemKey  item key_
   * @return True means item exists. False means item not exists.
   */
  public boolean itemExistsByItemKey(String hostname, String itemKey) {
    ArrayList<String> itemKeyList = new ArrayList();

    itemKeyList.add(itemKey);
    ZabbixAPIResult itemGetResult = itemGetByHostNameAndItemKey(hostname, itemKeyList);

    return itemExistsByCheckResult(itemGetResult);
  }

  /**
   * Check whether item exists by host name and item name.
   *
   * @param host     host name
   * @param itemName item name
   * @return True means item exists. False means item not exists.
   */
  public boolean itemExistsByItemName(String host, String itemName) {
    ArrayList<String> itemNameList = new ArrayList();

    itemNameList.add(itemName);
    ZabbixAPIResult itemGetResult = itemGetByHostNameAndItemName(host, itemNameList);

    return itemExistsByCheckResult(itemGetResult);
  }

  private boolean itemExistsByCheckResult(ZabbixAPIResult itemGetResult) {
    if (!itemGetResult.isFail()) {
      JsonNode data = (JsonNode) itemGetResult.getData();

      if (data.size() > 0) {
        String itemid = data.get(0).get("itemid").asText();
        if (itemid != null && !itemid.isEmpty()) {
          return true;
        }
      }
    }

    return false;
  }

  /**
   * Get item by param and search info.
   *
   * @param param  It can include itemids, groupids, hostids, interfaceids, host, group, etc
   * @param filter It can include key_, name, etc.
   * @return ZabbixAPIResult.data is the item array found, with each one including itemid,hostid,key_,name,etc.
   */
  public ZabbixAPIResult itemGet(HashMap<String, Object> param, HashMap<String, Object> filter) {
    String method = "item.get";
    param.put("filter", filter);

    return callApi(method, param);
  }

  /**
   * Get item by host and item key.
   *
   * @param host        host name
   * @param itemKeyList List of item key_
   * @return ZabbixAPIResult.data is the item array found, with each one including itemid,hostid,key_,name,etc.
   */
  public ZabbixAPIResult itemGetByHostNameAndItemKey(String host, ArrayList<String> itemKeyList) {
    HashMap<String, Object> param = new HashMap();
    HashMap<String, Object> filter = new HashMap();

    param.put("host", host);
    filter.put("key_", itemKeyList);

    return itemGet(param, filter);
  }

  /**
   * Get item by host and item name.
   *
   * @param host         host name
   * @param itemNameList List of item name
   * @return ZabbixAPIResult.data is the item array found, with each one including itemid,hostid,key_,name,etc.
   */
  public ZabbixAPIResult itemGetByHostNameAndItemName(String host, ArrayList<String> itemNameList) {
    HashMap<String, Object> param = new HashMap();
    HashMap<String, Object> filter = new HashMap();

    param.put("host", host);
    filter.put("name", itemNameList);

    return itemGet(param, filter);
  }

  /**
   * Delete multiple item by item ids.
   *
   * @param itemIdList List of item id.
   * @return ZabbixAPIResult.data.itemids is the item array that have been deleted.
   */
  public ZabbixAPIResult itemListDeleteByItemId(ArrayList<String> itemIdList) {
    String method = "item.delete";

    return callApi(method, itemIdList);
  }

  /**
   * Delete item by item id.
   *
   * @param itemId item id.
   * @return ZabbixAPIResult.data.itemids is the item array that have been deleted. Fetch the first one.
   */
  public ZabbixAPIResult itemDeleteByItemId(String itemId) {
    ArrayList<String> itemIdList = new ArrayList();

    itemIdList.add(itemId);

    return itemListDeleteByItemId(itemIdList);
  }

  /**
   * Delete items by item key
   *
   * @param host        host name
   * @param itemKeyList List of item key
   * @return ZabbixAPIResult.data.itemids is the item array that have been deleted.
   */
  public ZabbixAPIResult itemListDeleteByItemKey(String host, ArrayList<String> itemKeyList) {
    ZabbixAPIResult itemGetResult = itemGetByHostNameAndItemKey(host, itemKeyList);
    if (itemGetResult.isFail()) return itemGetResult;

    JsonNode data = (JsonNode) itemGetResult.getData();
    ArrayList itemIdList = new ArrayList();
    data.forEach(item -> {
      itemIdList.add(item.get("itemid"));
    });

    return itemListDeleteByItemId(itemIdList);
  }

  /**
   * Delete items by item name
   *
   * @param host         host name
   * @param itemNameList List of item name
   * @return ZabbixAPIResult.data.itemids is the item array that have been deleted.
   */
  public ZabbixAPIResult itemListDeleteByItemName(String host, ArrayList<String> itemNameList) {
    ZabbixAPIResult itemGetResult = itemGetByHostNameAndItemName(host, itemNameList);
    if (itemGetResult.isFail()) return itemGetResult;

    JsonNode data = (JsonNode) itemGetResult.getData();
    ArrayList<String> itemIdList = new ArrayList();
    data.forEach(item -> {
      itemIdList.add(item.get("itemid").asText());
    });

    return itemListDeleteByItemId(itemIdList);
  }
}