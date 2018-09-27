package org.cmbc.bigdata.zabbix;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

import java.util.List;

@Data
public class ListRequest extends RequestAbstract {
  private List params;

  public ListRequest() {
  }

  @Override
  void setParams(Object params) {
    this.params = (List) params;
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
