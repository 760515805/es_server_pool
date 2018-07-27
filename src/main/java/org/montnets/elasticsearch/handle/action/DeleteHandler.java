package org.montnets.elasticsearch.handle.action;



import java.io.IOException;
import java.util.Collections;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.montnets.elasticsearch.entity.EsRequestEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 
* Copyright: Copyright (c) 2018 Montnets
* 
* @ClassName: DelIndexAction.java
* @Description: 该类的功能描述
* es删除工具类
* @version: v1.0.0
* @author: chenhj
* @date: 2018年6月13日 下午3:04:59 
*
* Modification History:
* Date         Author          Version            Description
*---------------------------------------------------------*
* 2018年6月14日     chenhj          v1.0.0               修改原因
 */
public class DeleteHandler {
	  
	  private String index;
	  private String type;	  
	  private RestHighLevelClient rhlClient;
	  private	QueryBuilder queryBuilder;
	  private boolean isSync = false;
	  private static final Logger LOG = LoggerFactory.getLogger(DeleteHandler.class);

	public DeleteHandler(RestHighLevelClient rhlClient,EsRequestEntity<?> entity){
		this.index=entity.getIndex();
		this.type =entity.getType();
		this.rhlClient=rhlClient;
	}
	 /**
	  * 设置过滤条件
	  */
	 public DeleteHandler setQueryBuilder(QueryBuilder queryBuilder) {
			this.queryBuilder = queryBuilder;
			return this;
	 }
	/**
	 * 根据ID删除数据
	* @author chenhongjie 
	*/
	public  boolean  delDocById(String id) throws Exception{
			 DeleteRequest request = new DeleteRequest(index,type,id); 
			 DeleteResponse deleteResponse =  rhlClient.delete(request);
			 return deleteResponse.status()==RestStatus.OK;
	}
	/**
	 * 根据搜索内容删除数据
	* @author chenhongjie 
	 */
	public  boolean  delDocByQuery() throws Exception{
		 try {
			 SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder(); 
		     //是否有自定义条件
		     if(queryBuilder==null)throw new RuntimeException("请设置删除条数,或者你是想删除整个库?");
		     searchSourceBuilder.query(queryBuilder);
		     //LOG.debug("删除的内容条件:{}",searchSourceBuilder.toString());
		     //取低级客户端API来执行这步操作
		     RestClient restClient = rhlClient.getLowLevelClient();
			 String endPoint = "/" + index + "/" + type +"/_delete_by_query?conflicts=proceed&scroll_size=100000&timeout=1000s";
			 //删除的条件
			 String source = searchSourceBuilder.toString();
			 HttpEntity entity = new NStringEntity(source, ContentType.APPLICATION_JSON);
			 //Response response = restClient.performRequest("POST", command,Collections.<String, String> emptyMap(),entity);
			 //异步执行
			 RecordDeleteLog reLog = new RecordDeleteLog();
			 reLog.setCommand(endPoint);
			 reLog.setEntity(entity);
			 reLog.setRestClient(restClient);
			 reLog.setLogStr(searchSourceBuilder.toString());
			 new Thread(reLog, "删除线程_"+System.currentTimeMillis()).start();
			 // return response.getStatusLine().getStatusCode() == HttpStatus.SC_OK;
			 return true;
		 } catch (Exception e) {			
				throw e;
		}
	}
}
/**
*
* @ClassName: DelIndexAction.java
* @Description: 异步写删除日志的方法
*/
class RecordDeleteLog implements Runnable{
	private static final Logger LOG = LoggerFactory.getLogger(RecordDeleteLog.class);
	private String command;
	private HttpEntity entity;
	private RestClient restClient;
	private String LogStr;
	public void setCommand(String command) {
		this.command = command;
	}
	public void setEntity(HttpEntity entity) {
		this.entity = entity;
	}
	public void setRestClient(RestClient restClient) {
		this.restClient = restClient;
	}
	public void setLogStr(String logStr) {
		LogStr = logStr;
	}
	@Override
	public void run() {
		 try {
			Response response = restClient.performRequest("POST", command,Collections.<String, String> emptyMap(),entity);
			boolean status = response.getStatusLine().getStatusCode() == HttpStatus.SC_OK;
			if(status){
				LOG.info("删除成功...删除查询语句:{}",LogStr);
			}else{
				LOG.error("删除失败...删除查询语句:{}",LogStr);
			}
		} catch (IOException e) {
			LOG.error("删除出异常,如果是超时异常则无需处理...删除查询语句:{},异常:{}",LogStr,e);
		}
	}
}
