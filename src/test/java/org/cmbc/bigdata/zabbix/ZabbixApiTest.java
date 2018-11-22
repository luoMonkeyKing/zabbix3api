package org.cmbc.bigdata.zabbix;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static junit.framework.Assert.assertTrue;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;

public class ZabbixApiTest {
  ZabbixApi zabbixApi;
  static final String Zabbix_API_URL = "http://127.0.0.1:8888/zabbix/api_jsonrpc.php";
  static final String Zabbix_Version = "3.0.20";
  static final String Zabbix_User = "Admin";
  static final String Zabbix_Password = "zabbix";
  static final String Zabbix_Password_Wrong = "zabbix3";
  static final String Zabbix_Test_Host_Group_One = "testgroup1";
  static final String Zabbix_Test_Host_Group_Two = "testgroup2";
  static final String Zabbix_Test_Host_One = "testhost1";
  static final String Zabbix_Test_Host_Two = "testhost2";
  static final String Zabbix_Test_Item_One_Key = "testItemKey1";
  static final String Zabbix_Test_Item_Two_Key = "testItemKey2";

  @Before
  public void before() {
    zabbixApi = new ZabbixApi(Zabbix_API_URL);
    zabbixApi.init();
  }

  @After
  public void after() {
    zabbixApi.destroy();
  }

  @Test
  public void testApiVersion() {
    ZabbixAPIResult zabbixAPIResult = zabbixApi.apiVersion();
    if (!zabbixAPIResult.isFail()) {
      JsonNode data = (JsonNode) zabbixAPIResult.getData();
      String version = data.asText();
      assertEquals(version, Zabbix_Version);
    }
  }

  @Test
  public void testLogin() {
    boolean loginSuccess = zabbixApi.login(Zabbix_User, Zabbix_Password);
    boolean loginFail = zabbixApi.login(Zabbix_User, Zabbix_Password_Wrong);

    assertTrue(loginSuccess);
    assertFalse(loginFail);
  }

  @Test
  public void testHostgroupCreateAndDelete() {
    zabbixApi.login(Zabbix_User, Zabbix_Password);

    zabbixApi.hostgroupDeleteByName(Zabbix_Test_Host_Group_One);
    zabbixApi.hostgroupCreate(Zabbix_Test_Host_Group_One);

    assertTrue(zabbixApi.hostgroupExists(Zabbix_Test_Host_Group_One));

    zabbixApi.hostgroupDeleteByName(Zabbix_Test_Host_Group_One);
  }

  @Test
  public void testHostgroupListCreateAndDelete() {
    zabbixApi.login(Zabbix_User, Zabbix_Password);

    ArrayList<String> groupNameList = new ArrayList<>();
    groupNameList.add(Zabbix_Test_Host_Group_One);
    groupNameList.add(Zabbix_Test_Host_Group_Two);

    zabbixApi.hostgroupListDeleteByName(groupNameList);
    zabbixApi.hostgroupListCreate(groupNameList);

    assertTrue(zabbixApi.hostgroupExists(Zabbix_Test_Host_Group_One));
    assertTrue(zabbixApi.hostgroupExists(Zabbix_Test_Host_Group_Two));

    ZabbixAPIResult hostgroupListGetResult = zabbixApi.hostgroupListGetByName(groupNameList);
    if (!hostgroupListGetResult.isFail()) {
      JsonNode data = (JsonNode) hostgroupListGetResult.getData();
      assertEquals(2, data.size());
      if (data.size() > 0) {
        data.forEach(hostgroup -> {
          String groupname = hostgroup.get("name").asText();
          assertTrue(groupNameList.contains(groupname));
        });
      }
    }

    zabbixApi.hostgroupListDeleteByName(groupNameList);
  }

  @Test
  public void testHostCreateAndDelete() {
    zabbixApi.login(Zabbix_User, Zabbix_Password);

    Map hostInterface = createHostInterface("10050", "1");

    boolean hostExists = zabbixApi.hostExists(Zabbix_Test_Host_One);
    String groupId = "";
    // If host exists, delete the host
    if (hostExists) {
      zabbixApi.hostDeleteByName(Zabbix_Test_Host_One);
    }
    // Get the groupid of Zabbix_Test_Host_Group_One
    if (zabbixApi.hostgroupExists(Zabbix_Test_Host_Group_One)) {
      ZabbixAPIResult hostgroupGetResult = zabbixApi.hostgroupGetByGroupName(Zabbix_Test_Host_Group_One);
      if (!hostgroupGetResult.isFail()) {
        JsonNode data = (JsonNode) hostgroupGetResult.getData();
        // If Zabbix_Test_Host_Group exists, fetch the groupid
        groupId = data.get(0).get("groupid").asText();
      }
    } else {
      ZabbixAPIResult hostgroupCreateResult = zabbixApi.hostgroupCreate(Zabbix_Test_Host_Group_One);
      if (!hostgroupCreateResult.isFail()) {
        JsonNode data = (JsonNode) hostgroupCreateResult.getData();
        groupId = data.get("groupids").get(0).asText();
      }
    }

    ArrayList<String> groupIdList = new ArrayList<>();
    groupIdList.add(groupId);

    zabbixApi.hostCreate(Zabbix_Test_Host_One, groupIdList, hostInterface);

    assertTrue(zabbixApi.hostExists(Zabbix_Test_Host_One));

    zabbixApi.hostDeleteByName(Zabbix_Test_Host_One);
    zabbixApi.hostgroupDeleteByName(Zabbix_Test_Host_Group_One);
  }

  @Test
  public void testHostListCreateAndDelete() {
    zabbixApi.login(Zabbix_User, Zabbix_Password);
    ArrayList<String> groupNameList = new ArrayList<>();
    ArrayList<String> hostNameList = new ArrayList<>();
    groupNameList.add(Zabbix_Test_Host_Group_One);
    groupNameList.add(Zabbix_Test_Host_Group_Two);
    hostNameList.add(Zabbix_Test_Host_One);
    hostNameList.add(Zabbix_Test_Host_Two);

    ArrayList groupIdList = new ArrayList<>();
    ZabbixAPIResult hostgroupGetResult = zabbixApi.hostgroupListGetByName(groupNameList);
    if (!hostgroupGetResult.isFail()) {
      JsonNode data = (JsonNode) hostgroupGetResult.getData();
      if (data.size() > 0) {
        data.forEach(group -> {
          Map groupMap = new HashMap();
          groupMap.put("groupid", group.get("groupid").asText());
          groupIdList.add(groupMap);
        });
      }
    }

    if (groupIdList.size() == 0) {
      ZabbixAPIResult hostgroupCreateResult = zabbixApi.hostgroupListCreate(groupNameList);
      if (!hostgroupCreateResult.isFail()) {
        JsonNode result = (JsonNode) hostgroupCreateResult.getData();
        result.get("groupids").forEach(groupid -> {
          Map groupMap = new HashMap();
          groupMap.put("groupid", groupid);
          groupIdList.add(groupMap);
        });
      }
    }

    zabbixApi.hostListDeleteByName(hostNameList);
    ArrayList<HashMap> params = new ArrayList();
    HashMap param = new HashMap();
    param.put("host", Zabbix_Test_Host_One);
    param.put("groups", groupIdList);
    param.put("interfaces", createHostInterface("10050", "1"));
    params.add(param);
    param = new HashMap();
    param.put("host", Zabbix_Test_Host_Two);
    param.put("groups", groupIdList);
    Map interface1 = createHostInterface("10050", "1");
    Map interface2 = createHostInterface("9010", "4");
    ArrayList interfaces = new ArrayList();
    interfaces.add(interface1);
    interfaces.add(interface2);
    param.put("interfaces", interfaces);
    params.add(param);

    zabbixApi.hostListCreate(params);

    assertTrue(zabbixApi.hostExists(Zabbix_Test_Host_One));
    assertTrue(zabbixApi.hostExists(Zabbix_Test_Host_Two));

    ZabbixAPIResult hostListGetResult = zabbixApi.hostListGetByHostName(hostNameList);
    if (!hostListGetResult.isFail()) {
      JsonNode data = (JsonNode) hostListGetResult.getData();
      assertEquals(2, data.size());
      if (data.size() > 0) {
        data.forEach(host -> {
          String hostname = host.get("host").asText();
          assertTrue(hostNameList.contains(hostname));
        });
      }
    }

    zabbixApi.hostListDeleteByName(hostNameList);
  }

  private Map createHostInterface(String port, String type) {
    Map hostInterface = new HashMap<>();
    hostInterface.put("dns", "");
    hostInterface.put("ip", "127.0.0.1");
    hostInterface.put("main", 1);
    hostInterface.put("port", port);
    hostInterface.put("type", type);
    hostInterface.put("useip", 1);

    return hostInterface;
  }

  @Test
  public void testHostInterfaceCreate() {
    zabbixApi.login(Zabbix_User, Zabbix_Password);

    String groupId = null;
    String hostId = null;
    String interfaceId = null;

    if (!zabbixApi.hostgroupExists(Zabbix_Test_Host_Group_One)) {
      ZabbixAPIResult hostgroupCreateResult = zabbixApi.hostgroupCreate(Zabbix_Test_Host_Group_One);
      if (!hostgroupCreateResult.isFail()) {
        JsonNode data = (JsonNode) hostgroupCreateResult.getData();
        if (data.size() > 0) {
          groupId = data.get("groupids").get(0).asText();
        }
      }

    }
    if (!zabbixApi.hostExists(Zabbix_Test_Host_One)) {
      ArrayList<String> groupIdList = new ArrayList();
      groupIdList.add(groupId);
      ZabbixAPIResult hostCreateResult = zabbixApi.hostCreate(Zabbix_Test_Host_One, groupIdList, createHostInterface("10050", "1"));
      if (!hostCreateResult.isFail()) {
        JsonNode data = (JsonNode) hostCreateResult.getData();
        if (data.size() > 0) {
          hostId = data.get("hostids").get(0).asText();
        }
      }
    }
    ZabbixAPIResult hostGetResult = zabbixApi.hostGetByHostName(Zabbix_Test_Host_One);
    if (!hostGetResult.isFail()) {
      JsonNode data = (JsonNode) hostGetResult.getData();
      if (data.size() > 0) {
        hostId = data.get(0).get("hostid").asText();
      }
    }

    String dns = "";
    String ip = "127.0.0.1";
    String main = "0";
    String port = "10051";
    String type = "1";
    String useip = "1";
    String bulk = "1";

    ZabbixAPIResult hostInterfaceCreateResult = zabbixApi.hostInterfaceCreate(dns, hostId, ip, main, port, type, useip, bulk);
    if (!hostInterfaceCreateResult.isFail()) {
      JsonNode data = (JsonNode) hostInterfaceCreateResult.getData();
      if (data.size() > 0) {
        interfaceId = data.get("interfaceids").get(0).asText();
      }
    }

    ArrayList<String> hostIdList = new ArrayList<>();
    hostIdList.add(hostId);
    ArrayList<String> interfaceIdList = new ArrayList<>();
    ZabbixAPIResult hostInterfaceGetResult = zabbixApi.hostInterfaceGetByHostIds(hostIdList);
    if (!hostInterfaceGetResult.isFail()) {
      JsonNode hostInterfaces = (JsonNode) hostInterfaceGetResult.getData();
      if (hostInterfaces.size() > 0) {
        hostInterfaces.forEach(hostInterface -> {
          interfaceIdList.add(hostInterface.get("interfaceid").asText());
        });
      }
    }
    assertTrue(interfaceIdList.contains(interfaceId));

    zabbixApi.hostDeleteByName(Zabbix_Test_Host_One);
    zabbixApi.hostgroupDeleteByName(Zabbix_Test_Host_Group_One);
  }

  @Test
  public void testHostInterfaceListCreateError() {
    zabbixApi.login(Zabbix_User, Zabbix_Password);

    String dns = "";
    String main = "0";
    String port = "10050";
    String type = "1";
    String useip = "1";

    //Without required property hostid
    HashMap param = new HashMap();
    param.put("dns", dns);
    param.put("ip", "127.0.0.1");
    param.put("main", main);
    param.put("port", port);
    param.put("type", type);
    param.put("useip", useip);

    ArrayList<HashMap> params = new ArrayList<>();
    params.add(param);
    ZabbixAPIResult zabbixAPIResult = zabbixApi.hostInterfaceListCreate(params);

    Integer code = zabbixAPIResult.getCode();
    String data = zabbixAPIResult.getData().toString();

    assertEquals(code, ZabbixAPIResultCode.PARAM_IS_INVALID.code());
    assertEquals(data, "Param has no property : " + "hostid");
  }

  @Test
  public void testItemCreateAndDelete() {
    zabbixApi.login(Zabbix_User, Zabbix_Password);

    String groupId = null;
    String hostId = null;
    String interfaceId = null;
    String itemKey = "";
    ArrayList itemKeyList = new ArrayList();
    itemKeyList.add(Zabbix_Test_Item_One_Key);

    if (!zabbixApi.hostgroupExists(Zabbix_Test_Host_Group_One)) {
      zabbixApi.hostgroupCreate(Zabbix_Test_Host_Group_One);
    }

    ZabbixAPIResult hostgroupGetResult = zabbixApi.hostgroupGetByGroupName(Zabbix_Test_Host_Group_One);
    if (!hostgroupGetResult.isFail()) {
      JsonNode data = (JsonNode) hostgroupGetResult.getData();
      if (data.size() > 0) {
        groupId = data.get(0).get("groupid").asText();
      }
    }

    if (!zabbixApi.hostExists(Zabbix_Test_Host_One)) {
      ArrayList<String> groupIdList = new ArrayList();
      groupIdList.add(groupId);
      zabbixApi.hostCreate(Zabbix_Test_Host_One, groupIdList, createHostInterface("10050", "1"));
    }

    ArrayList hostNameList = new ArrayList();
    hostNameList.add(Zabbix_Test_Host_One);
    ZabbixAPIResult hostGetResult = zabbixApi.hostInterfaceGetByHostNames(hostNameList);
    if (!hostGetResult.isFail()) {
      JsonNode data = (JsonNode) hostGetResult.getData();
      if (data.size() > 0) {
        hostId = data.get(0).get("hostid").asText();
        interfaceId = data.get(0).get("interfaceid").asText();
      }
    }

    zabbixApi.itemListDeleteByItemKey(Zabbix_Test_Host_One, itemKeyList);

    HashMap param = new HashMap();
    param.put("delay", "60");
    param.put("hostid", hostId);
    param.put("interfaceid", interfaceId);
    param.put("key_", Zabbix_Test_Item_One_Key);
    param.put("name", Zabbix_Test_Item_One_Key);
    param.put("type", ZabbixItemType.ZABBIX_AGENT.code());
    param.put("value_type", ZabbixItemValueType.TEXT.code());

    zabbixApi.itemCreate(param);

    ZabbixAPIResult itemGetResult = zabbixApi.itemGetByHostNameAndItemKey(Zabbix_Test_Host_One, itemKeyList);

    if (!itemGetResult.isFail()) {
      JsonNode data = (JsonNode) itemGetResult.getData();
      if (data.size() > 0) {
        itemKey = data.get(0).get("key_").asText();
      }
    }

    assertEquals(itemKey, Zabbix_Test_Item_One_Key);

    zabbixApi.hostDeleteByName(Zabbix_Test_Host_One);
    zabbixApi.hostgroupDeleteByName(Zabbix_Test_Host_Group_One);
  }

  @Test
  public void testItemListCreateAndDelete() {
    zabbixApi.login(Zabbix_User, Zabbix_Password);

    String groupId = null;
    String hostId = null;
    String interfaceId = null;

    ArrayList itemKeyList = new ArrayList();
    itemKeyList.add(Zabbix_Test_Item_One_Key);
    itemKeyList.add(Zabbix_Test_Item_Two_Key);

    if (!zabbixApi.hostgroupExists(Zabbix_Test_Host_Group_One)) {
      zabbixApi.hostgroupCreate(Zabbix_Test_Host_Group_One);
    }

    ZabbixAPIResult hostgroupGetResult = zabbixApi.hostgroupGetByGroupName(Zabbix_Test_Host_Group_One);
    if (!hostgroupGetResult.isFail()) {
      JsonNode data = (JsonNode) hostgroupGetResult.getData();
      if (data.size() > 0) {
        groupId = data.get(0).get("groupid").asText();
      }
    }

    if (!zabbixApi.hostExists(Zabbix_Test_Host_One)) {
      ArrayList<String> groupIdList = new ArrayList();
      groupIdList.add(groupId);
      zabbixApi.hostCreate(Zabbix_Test_Host_One, groupIdList, createHostInterface("10050", "1"));
    }

    ArrayList hostNameList = new ArrayList();
    hostNameList.add(Zabbix_Test_Host_One);
    ZabbixAPIResult hostGetResult = zabbixApi.hostInterfaceGetByHostNames(hostNameList);
    if (!hostGetResult.isFail()) {
      JsonNode data = (JsonNode) hostGetResult.getData();
      if (data.size() > 0) {
        hostId = data.get(0).get("hostid").asText();
        interfaceId = data.get(0).get("interfaceid").asText();
      }
    }

    zabbixApi.itemListDeleteByItemKey(Zabbix_Test_Host_One, itemKeyList);

    HashMap param = new HashMap();
    param.put("delay", "60");
    param.put("hostid", hostId);
    param.put("interfaceid", interfaceId);
    param.put("key_", Zabbix_Test_Item_One_Key);
    param.put("name", Zabbix_Test_Item_One_Key);
    param.put("type", ZabbixItemType.ZABBIX_AGENT.code());
    param.put("value_type", ZabbixItemValueType.TEXT.code());

    HashMap param2 = new HashMap();
    param2.put("delay", "60");
    param2.put("hostid", hostId);
    param2.put("interfaceid", interfaceId);
    param2.put("key_", Zabbix_Test_Item_Two_Key);
    param2.put("name", Zabbix_Test_Item_Two_Key);
    param2.put("type", ZabbixItemType.ZABBIX_AGENT.code());
    param2.put("value_type", ZabbixItemValueType.TEXT.code());

    ArrayList params = new ArrayList();
    params.add(param);
    params.add(param2);

    zabbixApi.itemListCreate(params);

    ZabbixAPIResult itemGetResult = zabbixApi.itemGetByHostNameAndItemKey(Zabbix_Test_Host_One,
            itemKeyList);
    ArrayList itemKeys = new ArrayList();
    if (!itemGetResult.isFail()) {
      JsonNode data = (JsonNode) itemGetResult.getData();
      data.forEach(item -> {
        itemKeys.add(item.get("key_").asText());
      });
    }

    assertTrue(itemKeys.contains(Zabbix_Test_Item_One_Key));
    assertTrue(itemKeys.contains(Zabbix_Test_Item_Two_Key));

    zabbixApi.hostDeleteByName(Zabbix_Test_Host_One);
    zabbixApi.hostgroupDeleteByName(Zabbix_Test_Host_Group_One);
  }

}
