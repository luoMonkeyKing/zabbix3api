package org.cmbc.bigdata.zabbix;

/**
 * Type of the item.
 * <p>
 * Possible values:
 * 0 - Zabbix agent;
 * 1 - SNMPv1 agent;
 * 2 - Zabbix trapper;
 * 3 - simple check;
 * 4 - SNMPv2 agent;
 * 5 - Zabbix internal;
 * 6 - SNMPv3 agent;
 * 7 - Zabbix agent (active);
 * 8 - Zabbix aggregate;
 * 9 - web item;
 * 10 - external check;
 * 11 - database monitor;
 * 12 - IPMI agent;
 * 13 - SSH agent;
 * 14 - TELNET agent;
 * 15 - calculated;
 * 16 - JMX agent;
 * 17 - SNMP trap.
 */
public enum ZabbixItemType {
  ZABBIX_AGENT(0, "Zabbix agent"),
  SNMPV1_AGENT(1, "SNMPv1 agent"),
  ZABBIX_TRAPPER(2, "Zabbix trapper"),
  SIMIPLE_CHECK(3, "simple check"),
  SNMPV2_AGENT(4, "SNMPv2 agent;"),
  ZABBIX_INTERNAL(5, "Zabbix internal"),
  SNMPV3_AGENT(6, "SNMPv3 agent"),
  ZABBIX_AGENT_ACTIVE(7, "Zabbix agent active"),
  ZABBIX_AGGREGATE(8, "Zabbix aggregate"),
  WEB_ITEM(9, "web item"),
  EXTERNAL_CHECK(10, "external check"),
  DATABASE_MONITOR(11, "database monitor"),
  IPMI_AGENT(12, "IPMI agent"),
  SSH_AGENT(13, "SSH agent"),
  TELNET_AGENT(14, "TELNET agent"),
  CALCULATED(15, "calculated"),
  JMX_AGENT(16, "JMX agent"),
  SNMP_TRAP(17, "SNMP trap");

  private Integer code;
  private String msg;

  ZabbixItemType(Integer code, String msg) {
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

