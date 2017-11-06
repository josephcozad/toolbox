package com.jc.app.rest;

import java.util.List;
import java.util.Map;

public abstract class ValueOptionsLoader {

   public abstract List<Map<String, Object>> getValueOptions() throws Exception;
}
