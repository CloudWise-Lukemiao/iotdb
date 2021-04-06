/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// this file is generated by openapi generator

package org.apache.iotdb.openapi.gen.handler.impl;

import org.apache.iotdb.db.auth.AuthException;
import org.apache.iotdb.db.auth.AuthorityChecker;
import org.apache.iotdb.db.conf.IoTDBDescriptor;
import org.apache.iotdb.db.exception.StorageEngineException;
import org.apache.iotdb.db.exception.metadata.IllegalPathException;
import org.apache.iotdb.db.exception.metadata.MetadataException;
import org.apache.iotdb.db.exception.metadata.StorageGroupNotSetException;
import org.apache.iotdb.db.exception.query.QueryProcessException;
import org.apache.iotdb.db.metadata.PartialPath;
import org.apache.iotdb.db.qp.Planner;
import org.apache.iotdb.db.qp.executor.PlanExecutor;
import org.apache.iotdb.db.qp.physical.PhysicalPlan;
import org.apache.iotdb.db.qp.physical.crud.GroupByTimeFillPlan;
import org.apache.iotdb.db.qp.physical.crud.GroupByTimePlan;
import org.apache.iotdb.db.qp.physical.sys.ShowTimeSeriesPlan;
import org.apache.iotdb.db.query.context.QueryContext;
import org.apache.iotdb.db.query.control.QueryResourceManager;
import org.apache.iotdb.db.query.dataset.ShowTimeseriesDataSet;
import org.apache.iotdb.db.query.executor.QueryRouter;
import org.apache.iotdb.openapi.gen.handler.NotFoundException;
import org.apache.iotdb.openapi.gen.handler.V1ApiService;
import org.apache.iotdb.openapi.gen.model.GroupByFillPlan;
import org.apache.iotdb.tsfile.exception.filter.QueryFilterOptimizationException;
import org.apache.iotdb.tsfile.read.common.Field;
import org.apache.iotdb.tsfile.read.common.Path;
import org.apache.iotdb.tsfile.read.common.RowRecord;
import org.apache.iotdb.tsfile.read.query.dataset.QueryDataSet;

import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.ProtocolStringList;
import org.apache.commons.lang3.StringUtils;
import org.xerial.snappy.Snappy;
import prometheus.Remote;
import prometheus.Remote.Query;
import prometheus.Remote.ReadRequest;
import prometheus.Remote.ReadResponse;
import prometheus.Types;
import prometheus.Types.Label;
import prometheus.Types.LabelMatcher;
import prometheus.Types.Sample;
import prometheus.Types.TimeSeries;
import prometheus.Types.TimeSeries.Builder;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@javax.annotation.Generated(
    value = "org.openapitools.codegen.languages.JavaJerseyServerCodegen",
    date = "2021-01-13T21:45:03.765+08:00[Asia/Shanghai]")
public class V1ApiServiceImpl extends V1ApiService {
  private static final long MS_TO_MONTH = 30 * 86400_000L;
  private ConcurrentHashMap<String, ConcurrentHashMap> bigmap =
      new ConcurrentHashMap<String, ConcurrentHashMap>();
  private AtomicInteger lableTime = new AtomicInteger(0);
  private int sgcount = 5;
  private String timestampPrecision = "ms";
  private float timePrecision = 1; // the timestamp Precision is default ms
  private String NOPERMSSION = "No permissions for this operation ";

  public V1ApiServiceImpl() {
    sgcount = IoTDBDescriptor.getInstance().getConfig().getSgCount();
    timestampPrecision = IoTDBDescriptor.getInstance().getConfig().getTimestampPrecision();
    if (timestampPrecision.equals("ns")) {
      timePrecision = timePrecision * 1000000;
    } else if (timestampPrecision.equals("us")) {
      timePrecision = timePrecision * 1000;
    } else if (timestampPrecision.equals("s")) {
      timePrecision = timePrecision / 1000;
    }
  }

  @Override
  public Response postV1GrafanaData(
      GroupByFillPlan groupByFillPlan, SecurityContext securityContext) throws NotFoundException {
    Gson result = new Gson();
    Map timemap = new HashMap();
    List<String> pathList = groupByFillPlan.getPaths();
    String path = Joiner.on(".").join(pathList);
    String sql = "";
    List<Map> listtemp = new ArrayList<>();
    long stime = (long) (groupByFillPlan.getStime().doubleValue() * timePrecision);
    long etime = (long) (groupByFillPlan.getEtime().doubleValue() * timePrecision);
    if (groupByFillPlan.getFills() != null && groupByFillPlan.getFills().size() > 0) {
      sql =
          String.format(
              "select last_value(%s) FROM %s where time>=%d and time<%d group by ([%d, %d),%s) fill (%s[%s,%s])",
              path.substring(path.lastIndexOf('.') + 1),
              path.substring(0, path.lastIndexOf('.')),
              stime,
              etime,
              stime,
              etime,
              groupByFillPlan.getInterval(),
              groupByFillPlan.getFills().get(0).getDtype(),
              groupByFillPlan.getFills().get(0).getFun(),
              groupByFillPlan.getInterval());
    } else {
      sql =
          String.format(
              "select avg(%s) FROM %s where time>=%d and time<%d group by ([%d, %d),%s)",
              path.substring(path.lastIndexOf('.') + 1),
              path.substring(0, path.lastIndexOf('.')),
              stime,
              etime,
              stime,
              etime,
              groupByFillPlan.getInterval());
    }
    Planner planner = new Planner();

    try {
      PhysicalPlan physicalPlan = planner.parseSQLToPhysicalPlan(sql);
      if (!AuthorityChecker.check(
          securityContext.getUserPrincipal().getName(),
          physicalPlan.getPaths(),
          physicalPlan.getOperatorType(),
          null)) {
        return Response.ok()
            .entity("No permissions for this operation " + physicalPlan.getOperatorType())
            .build();
      }
      int fetchSize =
          Math.min(getFetchSizeForGroupByTimePlan((GroupByTimePlan) physicalPlan), 10000);
      QueryRouter qr = new QueryRouter();
      Long quertId = QueryResourceManager.getInstance().assignQueryId(true, fetchSize, -1);
      QueryContext context = new QueryContext(quertId);

      QueryDataSet dataSet = null;
      if (physicalPlan instanceof GroupByTimeFillPlan) {
        GroupByTimeFillPlan groupByTimeFillPlan = (GroupByTimeFillPlan) physicalPlan;
        dataSet = qr.groupByFill(groupByTimeFillPlan, context);
      } else if (physicalPlan instanceof GroupByTimePlan) {
        GroupByTimePlan groupByTimePlan = (GroupByTimePlan) physicalPlan;
        dataSet = qr.groupBy(groupByTimePlan, context);
      }

      Map<String, Object> tmpmap = new HashMap<String, Object>();
      List<Object> temlist = new ArrayList<Object>();
      List<Path> listpaths = dataSet.getPaths();

      timemap.put("name", "Time");
      timemap.put("type", "time");
      timemap.put("values", temlist);
      listtemp.add(timemap);
      for (Path pathResult : listpaths) {
        Map m = new HashMap();
        String fullname = pathResult.getFullPath();
        m.put("name", fullname);
        listtemp.add(m);
      }
      while (dataSet.hasNext()) {
        RowRecord rowRecord = dataSet.next();
        temlist.add(rowRecord.getTimestamp());
        List<org.apache.iotdb.tsfile.read.common.Field> fields = rowRecord.getFields();
        for (int i = 1; i < listtemp.size(); i++) {
          List listvalues = null;
          if (listtemp.get(i).get("values") != null) {
            listvalues = (List) listtemp.get(i).get("values");
          } else {
            listvalues = new ArrayList<>();
          }
          if (fields.get(i - 1) == null || fields.get(i - 1).getDataType() == null) {
            listvalues.add(null);
          } else {
            listvalues.add(fields.get(i - 1).getObjectValue(fields.get(i - 1).getDataType()));
            listtemp.get(i).put("values", listvalues);
          }
          if (listtemp.get(i).get("type") == null) {
            listtemp.get(i).put("type", "");
          } else if (listtemp.get(i).get("type").toString().length() == 0
              && fields.get(i - 1) != null
              && fields.get(i - 1).getDataType() != null) {
            listtemp.get(i).put("type", fields.get(i - 1).getDataType());
          }
        }
      }
    } catch (QueryProcessException e) {
      e.printStackTrace();
    } catch (StorageEngineException e) {
      e.printStackTrace();
    } catch (QueryFilterOptimizationException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    } catch (AuthException e) {
      e.printStackTrace();
    }
    return Response.ok().entity(result.toJson(listtemp)).build();
  }

  private int getFetchSizeForGroupByTimePlan(GroupByTimePlan groupByTimePlan) {
    int rows =
        (int)
            ((groupByTimePlan.getEndTime() - groupByTimePlan.getStartTime())
                / groupByTimePlan.getInterval());
    // rows gets 0 is caused by: the end time - the start time < the time interval.
    if (rows == 0 && groupByTimePlan.isIntervalByMonth()) {
      Calendar calendar = Calendar.getInstance();
      calendar.setTimeInMillis(groupByTimePlan.getStartTime());
      calendar.add(Calendar.MONTH, (int) (groupByTimePlan.getInterval() / MS_TO_MONTH));
      rows = calendar.getTimeInMillis() <= groupByTimePlan.getEndTime() ? 1 : 0;
    }
    return rows;
  }

  @Override
  public Response postV1GrafanaDataSimplejson(
      GroupByFillPlan groupByFillPlan, SecurityContext securityContext) throws NotFoundException {
    Gson result = new Gson();
    List<Map> resultList = new ArrayList<Map>();
    List<String> pathList = groupByFillPlan.getPaths();
    String path = Joiner.on(".").join(pathList);
    String sql = "";
    long stime = (long) (groupByFillPlan.getStime().doubleValue() * timePrecision);
    long etime = (long) (groupByFillPlan.getEtime().doubleValue() * timePrecision);
    if (groupByFillPlan.getFills() != null && groupByFillPlan.getFills().size() > 0) {
      sql =
          String.format(
              "select last_value(%s) FROM %s where time>=%d and time<%d group by ([%d, %d),%s) fill (%s[%s,%s])",
              path.substring(path.lastIndexOf('.') + 1),
              path.substring(0, path.lastIndexOf('.')),
              stime,
              etime,
              stime,
              etime,
              groupByFillPlan.getInterval(),
              groupByFillPlan.getFills().get(0).getDtype(),
              groupByFillPlan.getFills().get(0).getFun(),
              groupByFillPlan.getInterval());
    } else {
      sql =
          String.format(
              "select avg(%s) FROM %s where time>=%d and time<%d group by ([%d, %d),%s)",
              path.substring(path.lastIndexOf('.') + 1),
              path.substring(0, path.lastIndexOf('.')),
              stime,
              etime,
              stime,
              etime,
              groupByFillPlan.getInterval());
    }
    try {
      Planner planner = new Planner();
      PhysicalPlan physicalPlan = planner.parseSQLToPhysicalPlan(sql);
      if (!AuthorityChecker.check(
          securityContext.getUserPrincipal().getName(),
          physicalPlan.getPaths(),
          physicalPlan.getOperatorType(),
          null)) {

        return Response.ok().entity(NOPERMSSION + physicalPlan.getOperatorType()).build();
      }
      QueryDataSet dataSet = getDataBySelect(securityContext, sql, physicalPlan);
      Map<String, Object> tmpmap = new HashMap<String, Object>();
      List<Object> temlist = new ArrayList<Object>();
      while (dataSet.hasNext()) {
        RowRecord rowRecord = dataSet.next();
        List<org.apache.iotdb.tsfile.read.common.Field> fields = rowRecord.getFields();
        for (Field field : fields) {
          if (field == null || field.getDataType() == null) {
            temlist.add(null);
          } else {
            temlist.add(field.getObjectValue(field.getDataType()));
          }
          temlist.add(rowRecord.getTimestamp());
        }
      }
      if (dataSet != null && dataSet.getPaths().size() > 0) {
        tmpmap.put("target", dataSet.getPaths().get(0).getFullPath());
      } else {
        tmpmap.put("target", "");
      }

      tmpmap.put("datapoints", temlist);
      resultList.add(tmpmap);
    } catch (IOException | QueryProcessException | AuthException e) {
      e.printStackTrace();
    }
    return Response.ok().entity(result.toJson(resultList)).build();
  }

  @Override
  public Response postV1GrafanaNode(List<String> requestBody, SecurityContext securityContext)
      throws NotFoundException {
    Gson result = new Gson();
    PartialPath path = null;
    List<Map> leafnodes = new ArrayList<Map>();
    List<String> internalNodes = new ArrayList<String>();
    Map<String, List> resultMap = Maps.newHashMap();
    try {
      if (requestBody != null && requestBody.size() > 0) {
        String timeser = Joiner.on(".").join(requestBody);
        path = new PartialPath(timeser);
        ShowTimeSeriesPlan showTimeSeriesPlan =
            new ShowTimeSeriesPlan(path, false, null, null, 0, 0, false);
        if (!AuthorityChecker.check(
            securityContext.getUserPrincipal().getName(),
            showTimeSeriesPlan.getPaths(),
            showTimeSeriesPlan.getOperatorType(),
            null)) {
          return Response.ok().entity(NOPERMSSION + showTimeSeriesPlan.getOperatorType()).build();
        }
        QueryContext context = new QueryContext();
        ShowTimeseriesDataSet showTimeseriesDataSet =
            new ShowTimeseriesDataSet(showTimeSeriesPlan, context);
        List<RowRecord> list = showTimeseriesDataSet.getQueryDataSet();
        Map<String, Boolean> temNodeMap = new HashMap<String, Boolean>();
        for (RowRecord rowRecord : list) {
          String series = rowRecord.getFields().get(0).toString();
          String[] nodes = series.split("\\.");
          if (nodes.length > requestBody.size()) {
            String node = nodes[requestBody.size()];
            if (nodes.length - 2 >= requestBody.size()
                && (temNodeMap.get(node) == null || temNodeMap.get(node))) { // 倒数第二节点
              temNodeMap.put(node, false);
            } else if (nodes.length - 1 == requestBody.size()
                && temNodeMap.get(node) == null) { // 倒数第一个节点
              temNodeMap.put(node, true);
            }
          }
        }
        for (Map.Entry<String, Boolean> entry : temNodeMap.entrySet()) {
          Map<String, Object> nodeMap = Maps.newHashMap();
          if (entry.getValue()) {
            nodeMap.put("name", entry.getKey());
            nodeMap.put("leaf", entry.getValue());
            leafnodes.add(nodeMap);
          } else {
            nodeMap.put("name", entry.getKey());
            nodeMap.put("leaf", entry.getValue());
            leafnodes.add(nodeMap);
            internalNodes.add(entry.getKey());
          }
        }
      }
    } catch (IllegalPathException e) {
      e.printStackTrace();
    } catch (MetadataException e) {
      e.printStackTrace();
    } catch (AuthException e) {
      e.printStackTrace();
    }
    resultMap.put("internal", internalNodes);
    resultMap.put("series", leafnodes);
    return Response.ok().entity(result.toJson(resultMap)).build();
  }

  @Override
  public Response postV1PrometheusQuery(
      String userAgent,
      String xPrometheusRemoteReadVersion,
      byte[] body,
      SecurityContext securityContext)
      throws NotFoundException {
    long startTime = 0;
    long endTime = 0;
    String sql = "";
    ReadResponse.Builder readResponse = Remote.ReadResponse.newBuilder();
    try {
      if (bigmap.size() == 0) {
        initBigMap(securityContext);
      }
      int l = body.length;
      byte[] a = Snappy.uncompress(body);
      ReadRequest readRequest = Remote.ReadRequest.parseFrom(a);
      List<Query> queryList = readRequest.getQueriesList();
      StringBuilder sqlSelect = new StringBuilder("select ");
      StringBuilder sqlWhere = new StringBuilder(" where ");
      StringBuilder sqlGroupBy = new StringBuilder();
      String merticName = "";
      String strogeGroup = "root";
      List pathList = new ArrayList<String>();
      for (Query query : queryList) {
        startTime = (long) (query.getStartTimestampMs() * timePrecision);
        endTime = (long) (query.getEndTimestampMs() * timePrecision);
        for (LabelMatcher labelMatcher : query.getMatchersList()) {
          if ("__name__".equals(labelMatcher.getName())) {
            merticName = labelMatcher.getValue();
            int sgNum = Math.abs(merticName.hashCode()) % sgcount;
            strogeGroup = "root.sg" + sgNum + "." + merticName + ".";
            break;
          }
        }
        if (bigmap == null || bigmap.get(merticName) == null) {
          continue;
          // return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK,
          // "magic!")).build();
        }
        for (int i = 0; i < bigmap.get(merticName).size(); i++) {
          pathList.add("*");
        }
        for (LabelMatcher labelMatcher : query.getMatchersList()) {
          if (!"__name__".equals(labelMatcher.getName())) {
            appendtimeseries(labelMatcher, pathList, merticName);
          }
        }
        Types.ReadHints readHints = query.getHints();
        boolean by = readHints.getBy();
        long step = readHints.getStepMs() / 1000;
        long range = readHints.getRangeMs() / 1000;
        long groupStartTime = (long) (readHints.getStartMs() * timePrecision);
        long groupEndTime = (long) (readHints.getEndMs() * timePrecision);
        ProtocolStringList grouplist = readHints.getGroupingList();
        String func = readHints.getFunc();
        if (!StringUtils.isEmpty(func) && (func.equals("count") || func.equals("sum"))) {
          sqlSelect.append(func).append("(").append("*").append(")");
        } else if (!StringUtils.isEmpty(func) && func.equals("max")) {
          sqlSelect.append("max_value").append("(").append("*").append(")");
        } else if (!StringUtils.isEmpty(func) && func.equals("min")) {
          sqlSelect.append("min_value").append("(").append("*").append(")");
        } else {
          sqlSelect.append(" * ");
        }

        if (startTime >= 0) {
          sqlWhere.append("  time >= ").append(startTime);
        }

        if (startTime >= 0 && endTime >= 0) {
          sqlWhere.append(" and ").append(" time < ").append(endTime);
        } else if (startTime < 0 && endTime >= 0) {
          sqlWhere.append(" time < ").append(endTime);
        }

        if (grouplist.size() > 0 || by) {
          sqlGroupBy
              .append(" group by ([ ")
              .append(groupStartTime)
              .append(",")
              .append(groupEndTime)
              .append(" )");
          if (range > 0) {
            sqlGroupBy.append(",").append(range).append("s");
          }
          if (step > 0) {
            sqlGroupBy.append(",").append(step).append("s)");
          } else {
            sqlGroupBy.append(")");
          }
        }
        clear(pathList);
        sql = sqlSelect + " from " + strogeGroup + Joiner.on(".").join(pathList) + sqlWhere;
        Planner planner = new Planner();
        PhysicalPlan physicalPlan = planner.parseSQLToPhysicalPlan(sql);
        if (!AuthorityChecker.check(
            securityContext.getUserPrincipal().getName(),
            physicalPlan.getPaths(),
            physicalPlan.getOperatorType(),
            null)) {

          return Response.ok().entity(NOPERMSSION + physicalPlan.getOperatorType()).build();
        }
        QueryDataSet dataSet = getDataBySelect(securityContext, sql, physicalPlan);

        Map<String, Object> tmpmap = new HashMap<String, Object>();
        List<Object> temlist = new ArrayList<Object>();
        List<Map> timeseries = new ArrayList<Map>();

        TimeSeries timeSeries = null;
        int index = 0;
        while (dataSet.hasNext()) {
          RowRecord rowRecord = dataSet.next();
          List<org.apache.iotdb.tsfile.read.common.Field> fields = rowRecord.getFields();
          List<Path> listpath = dataSet.getPaths();
          Map<String, Object> temMap = new HashMap<String, Object>();
          List<Label> labelList = new ArrayList<>();
          for (int i = 0; i < fields.size(); i++) {
            Field field = fields.get(i);
            if (field == null || field.getDataType() == null) {
              continue;
            }
            timeSeries =
                generTimeSeries(dataSet.getPaths().get(i), merticName)
                    .setSamples(
                        index,
                        Sample.newBuilder()
                            .setTimestamp(rowRecord.getTimestamp())
                            .setValue(field.getDoubleV()))
                    .build();
          }
          index++;
        }
        if (timeSeries == null || timeSeries.getLabelsList().size() == 0) {
          return Response.ok().header("type", "application/x-protobuf").entity(null).build();
        }
        Remote.QueryResult.Builder queryResult =
            Remote.QueryResult.newBuilder().setTimeseries(0, timeSeries);
        readResponse.setResults(0, queryResult.build());
        byte[] datas = readResponse.build().toByteArray();
        ;
        datas = Snappy.compress(datas);
        return Response.ok().header("type", "application/x-protobuf").entity(datas).build();
      }

    } catch (InvalidProtocolBufferException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    } catch (AuthException e) {
      e.printStackTrace();
    } catch (QueryProcessException e) {
      e.printStackTrace();
    }
    return Response.ok().header("type", "application/x-protobuf").entity(null).build();
  }

  private Builder generTimeSeries(Path path, String metricName) {
    Builder timeBuilder = TimeSeries.newBuilder();
    String[] paths = path.getFullPath().split(".");
    if (paths.length > 2) {
      List<Label> lables = new ArrayList<Label>();
      int index = 0;
      for (int i = 2; i < paths.length; i++) {
        Map m = new HashMap<String, String>();
        if (paths[i].equals("ph")) {
          continue;
        }
        Iterator<Entry<String, String>> iter3 = bigmap.get(metricName).entrySet().iterator();
        while (iter3.hasNext()) {
          Entry<String, String> entry = iter3.next();
          if (i == Integer.valueOf(entry.getValue())) {
            timeBuilder.setLabels(
                index,
                Label.newBuilder().setName(entry.getKey().toString()).setValue(paths[i]).build());
            index++;
            break;
          }
        }
      }
      return timeBuilder;
    }
    return null;
  }

  private QueryDataSet getDataBySelect(
      SecurityContext securityContext, String sql, PhysicalPlan physicalPlan) {
    try {
      PlanExecutor executor = new PlanExecutor();
      int fetchSize = 10000;
      if (physicalPlan instanceof GroupByTimePlan) {
        fetchSize =
            Math.min(getFetchSizeForGroupByTimePlan((GroupByTimePlan) physicalPlan), fetchSize);
      }
      QueryRouter qr = new QueryRouter();
      Long quertId = QueryResourceManager.getInstance().assignQueryId(true, fetchSize, -1);
      QueryContext context = new QueryContext(quertId);
      return executor.processQuery(physicalPlan, context);

    } catch (QueryProcessException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    } catch (QueryFilterOptimizationException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {
      e.printStackTrace();
    } catch (MetadataException e) {
      e.printStackTrace();
    } catch (StorageEngineException e) {
      e.printStackTrace();
    }
    return null;
  }

  private void clear(List<String> list) {
    List<String> newSubPathList = new ArrayList<>();
    int i = 0;
    for (i = list.size() - 1; i >= 0; i--) {
      if (!list.get(i).equals("*")) {
        break;
      }
      newSubPathList.add(list.get(i));
    }
    list = newSubPathList;
  }

  private void appendtimeseries(LabelMatcher labelMatcher, List pathList, String merticName) {
    String tagV = labelMatcher.getValue();
    String tagK = labelMatcher.getName();
    tagV = tagV.replaceAll("'", "\\'");
    if (StringUtils.isEmpty(tagV)) {
      return;
    }
    switch (labelMatcher.getTypeValue()) {
      case LabelMatcher.Type.EQ_VALUE:
        pathList.set(
            Integer.valueOf(bigmap.get(merticName).get(tagK).toString()), "\"" + tagV + "\"");
        break;
      case LabelMatcher.Type.NEQ_VALUE:
        break;
      case LabelMatcher.Type.RE_VALUE: // contain
        break;
      case LabelMatcher.Type.NRE_VALUE: // uncontain
        break;
      default:
        pathList.set(
            Integer.valueOf(bigmap.get(merticName).get(tagV).toString()), "\"" + tagV + "\"");
    }
  }

  @Override
  public Response postV1PrometheusReceive(
      String userAgent,
      String xPrometheusRemoteWriteVersion,
      byte[] body,
      SecurityContext securityContext)
      throws NotFoundException {
    String resultValue = "fail";
    Gson result = new Gson();
    Map resultMap = new HashMap<String, Object>();
    try {
      if (bigmap.size() == 0) {
        initBigMap(securityContext);
      }
      byte[] a = Snappy.uncompress(body);
      long timestamp = 0;
      double value = 0;
      List<Types.TimeSeries> timeSeriesList = Remote.WriteRequest.parseFrom(a).getTimeseriesList();
      for (Types.TimeSeries timeSeries : timeSeriesList) {
        List<Types.Sample> samples = timeSeries.getSamplesList();
        List<Types.Label> labels = timeSeries.getLabelsList();
        ConcurrentHashMap<String, Object> map = new ConcurrentHashMap<String, Object>();
        Map<String, Object> pathmap = new LinkedHashMap<>();
        List<String> path = new ArrayList<String>();
        String merticName = "";
        for (Types.Label label : labels) {
          if (label.getName().equals("__name__")) {
            merticName = label.getValue();
            break;
          }
        }
        for (Types.Label label : labels) {
          if (bigmap.get(merticName) == null) {
            bigmap.put(merticName, map);
          } else if (bigmap.get(merticName) != null
              && bigmap.get(merticName).get(label.getName()) == null
              && !label.getName().equals("__name__")) {
            insertLabelOrder(bigmap.get(merticName).size(), lableTime, securityContext);
            insertMertic(merticName, lableTime, securityContext);
            insertLabel(label.getName(), lableTime, securityContext);
            bigmap.get(merticName).put(label.getName(), bigmap.get(merticName).size());
            pathmap.put(label.getName(), label.getValue());
          }
        }
        for (Types.Sample sample : samples) {
          timestamp = sample.getTimestamp();
          value = sample.getValue();
        }
        if (String.valueOf(value).equals("NaN")) {
          continue;
        }
        if (!pathmap.isEmpty()) {
          String sql = generInsertSql(bigmap, pathmap, merticName, timestamp, value, sgcount);
          resultValue = insertDb(securityContext, sql);
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    if (resultValue.equals("success")) {
      resultMap.put("code", 200);
      resultMap.put("message", "write data success");
    } else if (resultValue.equals("fail")) {
      resultMap.put("code", 500);
      resultMap.put("message", "write data failed");
    } else {
      resultMap.put("code", 401);
      resultMap.put("message", resultValue);
    }
    return Response.ok().entity(result.toJson(resultMap)).build();
  }

  public String insertDb(SecurityContext securityContext, String sql) {
    String result = "fail";
    try {
      Planner planner = new Planner();
      PlanExecutor executor = new PlanExecutor();
      PhysicalPlan physicalPlan = planner.parseSQLToPhysicalPlan(sql);
      if (!AuthorityChecker.check(
          securityContext.getUserPrincipal().getName(),
          physicalPlan.getPaths(),
          physicalPlan.getOperatorType(),
          null)) {
        return NOPERMSSION + physicalPlan.getOperatorType();
      }
      boolean b = executor.processNonQuery(physicalPlan);
      if (b) {
        result = "success";
      }
    } catch (QueryProcessException e) {
      e.printStackTrace();
    } catch (AuthException e) {
      e.printStackTrace();
    } catch (StorageGroupNotSetException e) {
      e.printStackTrace();
    } catch (StorageEngineException e) {
      e.printStackTrace();
    }
    return result;
  }

  public String insertMertic(
      String merticName, AtomicInteger time, SecurityContext securityContext) {
    String sql =
        "insert into root.system_p.label_info(timestamp,metric_name) values("
            + time
            + ",\""
            + merticName
            + "\")";
    return insertDb(securityContext, sql);
  }

  public String insertLabel(String labelName, AtomicInteger time, SecurityContext securityContext) {
    String sql =
        "insert into root.system_p.label_info(timestamp,label_name) values("
            + time
            + ",\""
            + labelName
            + "\")";
    String result = insertDb(securityContext, sql);
    time.getAndAdd(1);
    return result;
  }

  public String insertLabelOrder(
      int labelOrder, AtomicInteger time, SecurityContext securityContext) {
    String sql =
        "insert into root.system_p.label_info(timestamp,label_order) values("
            + time
            + ",\""
            + labelOrder
            + "\")";
    return insertDb(securityContext, sql);
  }

  public String generInsertSql(
      ConcurrentHashMap<String, ConcurrentHashMap> bigMap,
      Map pathMap,
      String metricName,
      long timestamp,
      double value,
      int sgcount) {
    BigDecimal bd =
        BigDecimal.valueOf(
            timestamp); // Scientific counting does not support the need to convert numbers
    timestamp = (long) (bd.longValue() * timePrecision);
    List<String> patlist = new ArrayList<String>();
    for (int i = 0; i < bigMap.get(metricName).size(); i++) {
      patlist.add("ph");
    }
    Iterator<Entry<String, String>> iter3 = pathMap.entrySet().iterator();
    while (iter3.hasNext()) {
      Entry<String, String> entry = iter3.next();
      patlist.set(
          Integer.valueOf(bigMap.get(metricName).get(entry.getKey()).toString()),
          "\"" + entry.getValue() + "\"");
    }
    int sgcode = metricName.hashCode() % sgcount;
    String path =
        "root.sg" + Math.abs(sgcode) + "." + metricName + "." + Joiner.on(".").join(patlist);
    String sql = "insert into " + path + "(timestamp,node) values(" + timestamp + "," + value + ")";
    return sql;
  }

  public void initBigMap(SecurityContext securityContext) {
    String sql = "select metric_name,label_name,label_order from root.system_p.label_info";
    try {
      Planner planner = new Planner();
      PhysicalPlan physicalPlan = planner.parseSQLToPhysicalPlan(sql);
      if (!AuthorityChecker.check(
          securityContext.getUserPrincipal().getName(),
          physicalPlan.getPaths(),
          physicalPlan.getOperatorType(),
          null)) {
        return;
      }
      QueryDataSet dataSet = getDataBySelect(securityContext, sql, physicalPlan);

      while (dataSet.hasNext()) {
        RowRecord rowRecord = dataSet.next();
        List<org.apache.iotdb.tsfile.read.common.Field> fields = rowRecord.getFields();
        List<Path> listpath = dataSet.getPaths();
        Map<String, Object> temMap = new HashMap<String, Object>();
        List<Label> labelList = new ArrayList<>();
        String meticName = "";
        String labelName = "";
        String orderValue = "";
        for (int i = 0; i < listpath.size(); i++) {
          Path path = listpath.get(i);
          if (path.getFullPath().contains("metric_name")) {
            meticName = fields.get(i).getStringValue();
          } else if (path.getFullPath().contains("label_name")) {
            labelName = fields.get(i).getStringValue();
          } else if (path.getFullPath().contains("label_order")) {
            orderValue = fields.get(i).getStringValue();
          }
        }
        ConcurrentHashMap<String, String> m = new ConcurrentHashMap<String, String>();
        if (bigmap.get(meticName) == null) {
          bigmap.put(meticName, m);
        } else {
          m = bigmap.get(meticName);
        }
        if (m.get(labelName) == null) {
          m.put(labelName, orderValue);
        }
      }
    } catch (IOException | QueryProcessException | AuthException e) {
      e.printStackTrace();
    }
  }
}
